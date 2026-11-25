package dev.huecoder.md3agent.feature.settings

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Code
import com.composables.icons.materialsymbols.rounded.Gavel
import com.composables.icons.materialsymbols.rounded.Open_in_new
import com.composables.icons.materialsymbols.rounded.Package_2
import dev.huecoder.md3agent.BuildConfig
import dev.huecoder.md3agent.R
import dev.huecoder.md3agent.data.updates.UpdateChecker
import dev.huecoder.md3agent.data.updates.UpdateStatus
import dev.huecoder.md3agent.feature.main.UpdatePromptDialog
import dev.huecoder.md3agent.ui.components.appViewModel
import dev.huecoder.md3agent.ui.components.SettingsScaffold
import dev.huecoder.md3agent.ui.components.ShapeBadge
import dev.huecoder.md3agent.ui.components.groupedListColors

@Composable
fun AboutScreen(onBack: () -> Unit, onSecret: () -> Unit) {
    val context = LocalContext.current
    val updates = appViewModel { AboutViewModel(it.updates) }
    val check by updates.check.collectAsStateWithLifecycle()
    var offer by remember { mutableStateOf<UpdateStatus.Available?>(null) }
    LaunchedEffect(check) { offer = (check as? UpdateCheck.Done)?.status as? UpdateStatus.Available }
    val links = listOf(
        AboutLink(MaterialSymbols.Rounded.Code, R.string.about_source, R.string.about_source_summary, UpdateChecker.REPOSITORY_URL),
        AboutLink(MaterialSymbols.Rounded.Gavel, R.string.about_license, R.string.about_license_summary, "${UpdateChecker.REPOSITORY_URL}/blob/main/LICENSE"),
        AboutLink(MaterialSymbols.Rounded.Package_2, R.string.about_third_party, R.string.about_third_party_summary, "${UpdateChecker.REPOSITORY_URL}/blob/main/THIRD_PARTY.md"),
    )
    val total = links.size

    SettingsScaffold(stringResource(R.string.settings_about), onBack) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            AppIdentity(onSecret, Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp))
            SegmentedListItem(
                onClick = {
                    val available = (check as? UpdateCheck.Done)?.status as? UpdateStatus.Available
                    if (available != null) offer = available else updates.checkForUpdates()
                },
                shapes = ListItemDefaults.segmentedShapes(0, 1),
                colors = groupedListColors(),
                leadingContent = { ShapeBadge(MaterialSymbols.Rounded.Package_2) },
                supportingContent = {
                    Text(when (val status = (check as? UpdateCheck.Done)?.status) {
                        is UpdateStatus.Available -> stringResource(R.string.about_update_available, status.version)
                        is UpdateStatus.UpToDate -> stringResource(R.string.about_update_latest, status.latest)
                        UpdateStatus.NotPublished -> stringResource(R.string.about_update_not_published)
                        UpdateStatus.Unreachable -> stringResource(R.string.about_update_unreachable)
                        null -> stringResource(if (check == UpdateCheck.Running) R.string.about_update_running else R.string.about_update_idle)
                    })
                },
            ) { Text(stringResource(R.string.about_check_updates), style = MaterialTheme.typography.titleMediumEmphasized) }
            links.forEachIndexed { index, link ->
                SegmentedListItem(
                    onClick = { context.openUrl(link.url) },
                    shapes = ListItemDefaults.segmentedShapes(index, total),
                    colors = groupedListColors(),
                    leadingContent = { ShapeBadge(link.icon) },
                    supportingContent = { Text(stringResource(link.summary)) },
                    trailingContent = { Icon(MaterialSymbols.Rounded.Open_in_new, contentDescription = null) },
                ) { Text(stringResource(link.title), style = MaterialTheme.typography.titleMediumEmphasized) }
            }
        }
    }
    offer?.let { UpdatePromptDialog(it) { offer = null } }
}

@Composable
private fun AppIdentity(onSecret: () -> Unit, modifier: Modifier = Modifier) {
    val taps = rememberSecretTaps(onSecret)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = taps.squish.value
                    scaleY = taps.squish.value
                }
                .clickable(interactionSource = null, indication = null, onClick = taps::tap)
                .size(LOGO_SIZE)
                .clip(RoundedCornerShape(LOGO_CORNER_PERCENT))
                .background(colorResource(R.color.ic_launcher_background)),
        ) {
            Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.requiredSize(LOGO_SIZE * ADAPTIVE_BLEED))
        }
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMediumEmphasized)
        Text(
            taps.remaining?.let { stringResource(R.string.about_logo_taps_left, it) }
                ?: stringResource(R.string.about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class AboutLink(val icon: ImageVector, @StringRes val title: Int, @StringRes val summary: Int, val url: String)

private fun Context.openUrl(url: String) = startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))

private val LOGO_SIZE = 96.dp
private const val LOGO_CORNER_PERCENT = 30
private const val ADAPTIVE_BLEED = 1.5f
