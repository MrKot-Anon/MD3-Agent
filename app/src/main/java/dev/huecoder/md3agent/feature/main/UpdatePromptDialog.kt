package dev.huecoder.md3agent.feature.main

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import dev.huecoder.md3agent.R
import dev.huecoder.md3agent.data.updates.UpdateStatus
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun UpdatePromptDialog(offer: UpdateStatus.Available, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloading by remember(offer.version) { mutableStateOf(false) }
    var error by remember(offer.version) { mutableStateOf<String?>(null) }
    var pendingApk by remember(offer.version) { mutableStateOf<File?>(null) }

    fun install(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startActivity(intent)
            onDismiss()
        } catch (failure: Exception) {
            error = failure.message ?: context.getString(R.string.update_install_failed)
        }
    }

    val allowInstalls = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val file = pendingApk
        if (file != null && context.packageManager.canRequestPackageInstalls()) install(file)
        else error = context.getString(R.string.update_install_permission_needed)
    }

    AlertDialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        title = { Text(stringResource(R.string.update_prompt_title)) },
        text = {
            Column {
                Text(stringResource(R.string.update_prompt_body, offer.version))
                if (downloading) Text(stringResource(R.string.update_downloading))
                error?.let { Text(it) }
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, offer.pageUrl.toUri()))
                }) { Text(stringResource(R.string.update_prompt_changelog)) }
            }
        },
        confirmButton = {
            TextButton(enabled = !downloading, onClick = {
                downloading = true
                error = null
                scope.launch {
                    try {
                        val updates = (context.applicationContext as dev.huecoder.md3agent.App).container.updates
                        val file = updates.download(offer)
                        pendingApk = file
                        if (context.packageManager.canRequestPackageInstalls()) install(file)
                        else allowInstalls.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri()))
                    } catch (failure: Exception) {
                        error = failure.message ?: context.getString(R.string.update_download_failed)
                    } finally {
                        downloading = false
                    }
                }
            }) { Text(stringResource(R.string.update_prompt_install)) }
        },
        dismissButton = { TextButton(enabled = !downloading, onClick = onDismiss) { Text(stringResource(R.string.update_prompt_later)) } },
    )
}
