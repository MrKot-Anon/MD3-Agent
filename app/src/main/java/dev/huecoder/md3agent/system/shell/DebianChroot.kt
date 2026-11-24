package dev.huecoder.md3agent.system.shell

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/** The bundled Debian arm64 userland, entered through a real root chroot. */
class DebianChroot(private val context: Context, private val root: RootShell, private val workspace: File) : Shell {
    private val mutex = Mutex()
    private val installDir = File(context.filesDir, "debian")
    private val rootfs = File(installDir, "rootfs")
    private val x86Emulator = Build.SUPPORTED_ABIS.firstOrNull() == "x86_64"
    private val asset = if (x86Emulator) "debian/rootfs-x86_64.blob" else "debian/rootfs-arm64.blob"
    private val expectedSha256 = if (x86Emulator) X86_SHA256 else ARM64_SHA256

    override suspend fun run(command: String, timeoutMs: Long): ShellResult = mutex.withLock {
        if (root.status() != AccessStatus.READY) {
            return@withLock ShellResult(null, "", "Root access through su is required for the Debian chroot")
        }
        runInterruptible(Dispatchers.IO) {
            ensureInstalled()
            updateResolver()
            runInChroot(command, timeoutMs)
        }
    }

    suspend fun status(): AccessStatus = root.status()

    private fun ensureInstalled() {
        val ready = File(installDir, READY_FILE)
        if (ready.isFile && ready.readText() == expectedSha256 && File(rootfs, "usr/bin/bash").isFile && File(rootfs, "bin").isDirectory) return
        installDir.mkdirs()
        val archive = File(installDir, "rootfs.tar.gz")
        val staging = File(installDir, "rootfs-installing")
        removeAsRoot(staging)
        staging.mkdirs()
        val sha = MessageDigest.getInstance("SHA-256")
        context.assets.open(asset).use { input ->
            archive.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    sha.update(buffer, 0, count)
                    output.write(buffer, 0, count)
                }
            }
        }
        val digest = sha.digest().joinToString("") { "%02x".format(it) }
        if (digest != expectedSha256) throw IOException("Bundled Debian rootfs failed its SHA-256 check")
        // Toybox tar reports warnings for absolute symlinks and can skip hardlinks.
        // Bootstrap GNU tar from the layer, then extract again inside the chroot.
        val bootstrap = root.runBlocking("/system/bin/tar -xozf ${sh(archive.path)} -C ${sh(staging.path)}", INSTALL_TIMEOUT_MS)
        if (bootstrap.timedOut || !File(staging, "usr/bin/tar").isFile || !File(staging, "usr/bin/bash").isFile) {
            throw IOException("Could not bootstrap Debian: ${bootstrap.stderr.take(500)}")
        }
        val unpack = root.runBlocking(
            "/system/bin/gzip -dc ${sh(archive.path)} | chroot ${sh(staging.path)} /usr/bin/tar -xf - -C /",
            INSTALL_TIMEOUT_MS,
        )
        archive.delete()
        if (unpack.exitCode != 0) throw IOException("Could not unpack Debian: ${unpack.stderr.take(500)}")
        if (!File(staging, "usr/bin/bash").isFile || !File(staging, "bin").isDirectory) {
            removeAsRoot(staging)
            throw IOException("Bundled Debian rootfs is incomplete")
        }
        val mountPoints = listOf("dev", "proc", "sys", "sdcard", "app", "app-cache", "workspace")
        val makeMounts = root.runBlocking("mkdir -p ${mountPoints.joinToString(" ") { sh(File(staging, it).path) }}", PROBE_TIMEOUT_MS)
        if (makeMounts.exitCode != 0) throw IOException("Could not prepare Debian mounts: ${makeMounts.stderr.take(200)}")
        removeAsRoot(rootfs)
        if (!staging.renameTo(rootfs)) throw IOException("Could not install Debian rootfs")
        ready.writeText(expectedSha256)
    }

    private fun updateResolver() {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return
        val network = manager.activeNetwork ?: return
        val servers = manager.getLinkProperties(network)?.dnsServers.orEmpty().map { it.hostAddress }.filterNotNull()
        if (servers.isNotEmpty()) {
            val lines = servers.joinToString("\n", postfix = "\n") { "nameserver $it" }
            val result = root.runBlocking("printf %s ${sh(lines)} > ${sh(File(rootfs, "etc/resolv.conf").path)}", PROBE_TIMEOUT_MS)
            if (result.exitCode != 0) throw IOException("Could not configure Debian DNS: ${result.stderr.take(200)}")
        }
    }

    private fun removeAsRoot(file: File) {
        val result = root.runBlocking("rm -rf ${sh(file.path)}", INSTALL_TIMEOUT_MS)
        if (result.exitCode != 0) throw IOException("Could not clean Debian installation: ${result.stderr.take(200)}")
    }

    private fun runInChroot(command: String, timeoutMs: Long): ShellResult {
        workspace.mkdirs()
        val mounts = buildList {
            add("/dev" to "dev")
            add("/sys" to "sys")
            add(context.filesDir.canonicalPath to "app")
            add(context.cacheDir.canonicalPath to "app-cache")
            add(workspace.canonicalPath to "workspace")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()) {
                add("/storage/emulated/0" to "sdcard")
            }
        }
        val script = buildString {
            appendLine("set -e")
            appendLine("cleanup() {")
            appendLine("  umount ${sh(File(rootfs, "proc").path)} 2>/dev/null || true")
            mounts.asReversed().forEach { (_, target) -> appendLine("  umount ${sh(File(rootfs, target).path)} 2>/dev/null || true") }
            appendLine("}")
            appendLine("trap cleanup EXIT")
            mounts.forEach { (source, target) -> appendLine("mount -o bind ${sh(source)} ${sh(File(rootfs, target).path)}") }
            appendLine("mount -t proc proc ${sh(File(rootfs, "proc").path)}")
            appendLine("chroot ${sh(rootfs.path)} /usr/bin/env -i HOME=/root PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin DEBIAN_FRONTEND=noninteractive TERM=dumb /bin/bash -lc ${sh("cd /workspace && $command")}")
        }
        val isolated = root.runBlocking("unshare -m -- /system/bin/true", PROBE_TIMEOUT_MS).exitCode == 0
        val entry = if (isolated) "unshare -m -- /system/bin/sh -c ${sh(script)}" else script
        return root.runBlocking(entry, timeoutMs)
    }

    private fun sh(value: String): String = "'" + value.replace("'", "'\\''") + "'"

    private companion object {
        const val ARM64_SHA256 = "48fb00926fe468d83ca04ee6775f36350d17e0f328dfcb03aadb5de0c52ae3f6"
        const val X86_SHA256 = "40748add9f4f6f944f334cbfc286590d6e93f60fb06f6ee7ffa1b894080ad861"
        const val READY_FILE = ".md3-ready"
        const val INSTALL_TIMEOUT_MS = 180_000L
        const val PROBE_TIMEOUT_MS = 3_000L
    }
}
