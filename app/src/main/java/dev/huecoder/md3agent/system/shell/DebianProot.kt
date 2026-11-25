package dev.huecoder.md3agent.system.shell

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.security.MessageDigest

/** Bundled Debian userland running under the app UID through PRoot, with no su. */
class DebianProot(private val context: Context, private val workspace: File) : Shell {
    private val mutex = Mutex()
    private val prefix = File(context.filesDir, "usr")
    private val installDir = File(prefix, "var/lib/pr/installed-rootfs")
    private val rootfs = File(installDir, "debian")
    private val nativeDir = File(context.applicationInfo.nativeLibraryDir)
    private val proot = File(nativeDir, "libproot.so")
    private val loader = File(nativeDir, "libproot-loader.so")
    private val cli = File(nativeDir, "libpr-cli.so")

    override suspend fun run(command: String, timeoutMs: Long): ShellResult = mutex.withLock {
        if (status() != AccessStatus.READY) return@withLock ShellResult(null, "", "Debian PRoot is unavailable for this device architecture")
        runInterruptible(Dispatchers.IO) {
            ensureInstalled()
            execute(command, timeoutMs)
        }
    }

    fun status(): AccessStatus = if (proot.isFile && loader.isFile && cli.isFile && Build.SUPPORTED_ABIS.firstOrNull() == "arm64-v8a") AccessStatus.READY else AccessStatus.UNAVAILABLE

    private fun ensureInstalled() {
        val ready = File(installDir, "debian.sha256")
        if (ready.isFile && ready.readText() == ROOTFS_SHA256 && File(rootfs, "usr/bin/bash").isFile) return
        installDir.mkdirs()
        val staging = File(installDir, "rootfs-installing")
        staging.deleteRecursively()
        staging.mkdirs()
        val archive = File(installDir, "rootfs.blob")
        val sha = MessageDigest.getInstance("SHA-256")
        context.assets.open(ROOTFS_ASSET).use { input ->
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
        if (sha.digest().joinToString("") { "%02x".format(it) } != ROOTFS_SHA256) throw IOException("Bundled Debian checksum failed")
        try {
            DebianArchiveExtractor.extract(archive, staging)
            if (!File(staging, "usr/bin/bash").isFile || !File(staging, "usr/bin/apt").isFile || !Files.isSymbolicLink(File(staging, "bin").toPath())) {
                throw IOException("Bundled Debian rootfs is incomplete")
            }
            for (name in listOf("workspace", "app", "app-cache", "sdcard", "dev", "proc", "sys")) File(staging, name).mkdirs()
            File(staging, ".l2s").mkdirs()
            rootfs.deleteRecursively()
            if (!staging.renameTo(rootfs)) throw IOException("Could not install Debian rootfs")
            ready.writeText(ROOTFS_SHA256)
        } finally {
            archive.delete()
            staging.deleteRecursively()
        }
    }

    private fun dnsServers(): List<String> {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return emptyList()
        return manager.activeNetwork?.let(manager::getLinkProperties)?.dnsServers.orEmpty().mapNotNull { it.hostAddress }
    }

    private fun execute(command: String, timeoutMs: Long): ShellResult {
        workspace.mkdirs()
        File(context.filesDir, "home").mkdirs()
        val binds = listOf(
            "${workspace.canonicalPath}:/workspace",
            "${context.filesDir.canonicalPath}:/app",
            "${context.cacheDir.canonicalPath}:/app-cache",
        )
        val args = buildList {
            add(cli.path)
            add("login")
            add("debian")
            binds.forEach { add("--custom-bind=$it") }
            add("--")
            add("export DEBIAN_FRONTEND=noninteractive; cd /workspace && $command")
        }
        return ProcessRunner.run(args, timeoutMs, environment = mapOf(
            "APP_PREFIX" to prefix.path,
            "APP_HOME" to File(context.filesDir, "home").path,
            "APP_PACKAGE" to context.packageName,
            "APP_DNS_SERVERS" to dnsServers().joinToString(","),
            "PROOT_LOADER" to loader.path,
            "PROOT_L2S_DIR" to File(rootfs, ".l2s").path,
            "PROOT_NO_SECCOMP" to "1",
            "PROOT_TMP_DIR" to context.cacheDir.path,
            "TMPDIR" to context.cacheDir.path,
            "ANDROID_DATA" to "/data",
            "ANDROID_ROOT" to "/system",
            "DEBIAN_FRONTEND" to "noninteractive",
        ))
    }

    private companion object {
        const val ROOTFS_ASSET = "debian/rootfs-arm64.blob"
        const val ROOTFS_SHA256 = "48fb00926fe468d83ca04ee6775f36350d17e0f328dfcb03aadb5de0c52ae3f6"
    }
}
