package dev.huecoder.md3agent.system.shell

import android.content.Context
import dev.huecoder.md3agent.data.settings.AccessSettings
import dev.huecoder.md3agent.data.settings.ShellMode
import java.io.File

class ShellAccess(context: Context) {
    val workspace = File(context.filesDir, WORKSPACE_DIR)
    val debian = DebianProot(context, workspace)
    val shizuku = ShizukuShell(context)
    val root = RootShell()
    private val sandbox = SandboxShell(workspace, context.cacheDir)

    fun privilegedShell(access: AccessSettings): Shell? = when {
        access.usesRoot -> root
        access.shell == ShellMode.SHIZUKU -> shizuku
        else -> null
    }

    fun shell(mode: ShellMode): Shell = when (mode) {
        ShellMode.SANDBOX -> sandbox
        ShellMode.TERMUX -> debian
        ShellMode.DEBIAN -> debian
        ShellMode.SHIZUKU -> shizuku
        ShellMode.ROOT -> root
    }

    suspend fun status(mode: ShellMode, probeRoot: Boolean): AccessStatus = when (mode) {
        ShellMode.SANDBOX -> AccessStatus.READY
        ShellMode.TERMUX, ShellMode.DEBIAN -> debian.status()
        ShellMode.SHIZUKU -> shizuku.status()
        ShellMode.ROOT -> if (probeRoot) root.status() else AccessStatus.UNKNOWN
    }

    private companion object {
        const val WORKSPACE_DIR = "workspace"
    }
}
