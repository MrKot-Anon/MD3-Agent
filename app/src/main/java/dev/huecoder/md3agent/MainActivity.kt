package dev.huecoder.md3agent

import android.content.Context
import android.content.Intent
import android.Manifest
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.app.AlarmManager
import android.provider.Settings
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.core.net.toUri
import dev.huecoder.md3agent.assistant.AssistantRole
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.huecoder.md3agent.ui.avatar.ProvideAgentLook
import dev.huecoder.md3agent.ui.haptics.ProvideHaptics
import dev.huecoder.md3agent.ui.keyboard.KeyboardGuard
import dev.huecoder.md3agent.ui.locale.AppLocale
import dev.huecoder.md3agent.ui.navigation.AppNavHost
import dev.huecoder.md3agent.ui.navigation.LaunchTarget
import dev.huecoder.md3agent.ui.theme.AssistantTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val pendingChatId = MutableStateFlow<String?>(null)
    private val pendingTarget = MutableStateFlow<LaunchTarget?>(null)
    private val accessSteps = ArrayDeque<Intent>()
    private val askRuntime = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { openNextAccessStep() }
    private val askSpecial = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            (application as App).container.applicationScope.launch {
                (application as App).container.settings.updateTools { tools ->
                    tools.copy(access = tools.access.copy(files = dev.huecoder.md3agent.data.settings.FileScope.SHARED_STORAGE))
                }
            }
        }
        openNextAccessStep()
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) consume(intent)
        if (savedInstanceState == null) requestDeviceAccess()
        setContent {
            val chatToOpen by pendingChatId.collectAsStateWithLifecycle()
            val target by pendingTarget.collectAsStateWithLifecycle()
            AssistantTheme {
                ProvideAgentLook {
                    ProvideHaptics {
                        KeyboardGuard()
                        AppNavHost(
                            chatToOpen = chatToOpen,
                            onChatOpened = { pendingChatId.value = null },
                            target = target,
                            onTargetHandled = { pendingTarget.value = null },
                        )
                    }
                }
            }
        }
    }

    private fun requestDeviceAccess() {
        val prefs = getSharedPreferences("startup_access", MODE_PRIVATE)
        if (prefs.getBoolean("requested", false)) return
        prefs.edit().putBoolean("requested", true).apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            accessSteps += Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, "package:$packageName".toUri())
        }
        if (!packageManager.canRequestPackageInstalls()) {
            accessSteps += Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:$packageName".toUri())
        }
        if (!(application as App).container.phoneControl.isReady()) accessSteps += Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !(getSystemService(ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()) {
            accessSteps += Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:$packageName".toUri())
        }
        if (!(getSystemService(POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)) {
            accessSteps += Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:$packageName".toUri())
        }
        val runtime = buildList {
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (runtime.isEmpty()) openNextAccessStep() else askRuntime.launch(runtime.toTypedArray())
    }

    private fun openNextAccessStep() {
        while (accessSteps.isNotEmpty()) {
            val intent = accessSteps.removeFirst()
            try {
                askSpecial.launch(intent)
                return
            } catch (_: android.content.ActivityNotFoundException) {
                // Some Android builds do not expose every special access screen.
            }
        }
        if (!AssistantRole.isHeld(this)) AssistantRole.openSystemPicker(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consume(intent)
    }

    private fun consume(intent: Intent?) {
        intent?.getStringExtra(EXTRA_CHAT_ID)?.let { pendingChatId.value = it }
        LaunchTarget.parse(intent?.getStringExtra(EXTRA_TARGET))?.let { pendingTarget.value = it }
    }

    companion object {
        const val EXTRA_CHAT_ID = "dev.huecoder.md3agent.extra.CHAT_ID"
        const val EXTRA_TARGET = "dev.huecoder.md3agent.extra.TARGET"

        fun targetIntent(context: Context, target: LaunchTarget): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_TARGET, target.name)

        fun chatIntent(context: Context, chatId: String?): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .apply { chatId?.let { putExtra(EXTRA_CHAT_ID, it) } }
    }
}
