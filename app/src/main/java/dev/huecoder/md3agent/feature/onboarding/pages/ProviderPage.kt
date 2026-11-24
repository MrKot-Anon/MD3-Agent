package dev.huecoder.md3agent.feature.onboarding.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.huecoder.md3agent.R
import dev.huecoder.md3agent.data.settings.ModelSelection
import dev.huecoder.md3agent.feature.models.ModelPickerSheet
import dev.huecoder.md3agent.feature.onboarding.LATE_ORDER
import dev.huecoder.md3agent.feature.onboarding.OnboardingState
import dev.huecoder.md3agent.feature.onboarding.PageScaffold
import dev.huecoder.md3agent.feature.onboarding.reveal

@Composable
fun ProviderPage(
    state: OnboardingState,
    onPickModel: (ModelSelection) -> Unit,
    onCustom: () -> Unit,
    onLater: () -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    PageScaffold(
        title = stringResource(R.string.onboarding_provider_title),
        body = stringResource(R.string.onboarding_local_endpoint_hint),
    ) {
        val name = state.providerName
        if (name == null) {
            FilledTonalButton(onClick = onCustom, modifier = Modifier.fillMaxWidth().reveal(2)) {
                Text(stringResource(R.string.onboarding_local_endpoint_action))
            }
            TextButton(onClick = onLater, modifier = Modifier.reveal(LATE_ORDER)) {
                Text(stringResource(R.string.onboarding_provider_later))
            }
        } else {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth().reveal(2)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.onboarding_provider_connected, name), style = MaterialTheme.typography.titleMedium)
                    FilledTonalButton(onClick = { picking = true }) {
                        Text(state.model?.modelId ?: stringResource(R.string.onboarding_provider_pick_model))
                    }
                }
            }
        }
    }
    if (picking) {
        ModelPickerSheet(model = state.model, agentId = null, onSelectModel = onPickModel, onSelectAgent = {}, onDismiss = { picking = false })
    }
}
