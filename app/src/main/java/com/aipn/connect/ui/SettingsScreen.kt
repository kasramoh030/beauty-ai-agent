package com.aipn.connect.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aipn.connect.R
import com.aipn.connect.ThemeChoice

@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val keys by viewModel.keys.collectAsStateWithLifecycle()
    var prompt by remember(settings.systemPrompt) { mutableStateOf(settings.systemPrompt) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SettingsCard(title = stringResource(R.string.settings_model_pick)) {
            keys.filter { it.enabled && (it.hasKey || it.provider.keyOptional) }
                .forEach { status ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(
                            if (LocaleState.isPersian) status.provider.nameFa else status.provider.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            status.model.ifBlank { status.provider.defaultModel },
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
        } }

        item { SettingsCard(title = stringResource(R.string.settings_system_prompt)) {
            OutlinedTextField(
                value = prompt,
                onValueChange = {
                    prompt = it
                    viewModel.setSystemPrompt(it)
                },
                placeholder = { Text(stringResource(R.string.settings_system_prompt_hint)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
        } }

        item { SettingsCard(title = stringResource(R.string.settings_temperature)) {
            Text(
                stringResource(R.string.settings_temperature, settings.temperature),
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = settings.temperature,
                onValueChange = { viewModel.setTemperature(it) },
                valueRange = 0f..2f,
                steps = 19,
            )
            Text(
                stringResource(R.string.settings_max_tokens) + ": " + settings.maxTokens,
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = settings.maxTokens.toFloat(),
                onValueChange = { viewModel.setMaxTokens(it.toInt()) },
                valueRange = 128f..8192f,
                steps = 30,
            )
        } }

        item { SettingsCard(title = stringResource(R.string.settings_streaming)) {
            SwitchRow(
                title = stringResource(R.string.settings_streaming),
                subtitle = stringResource(R.string.settings_streaming_desc),
                checked = settings.streaming,
                onChange = viewModel::setStreaming,
            )
            SwitchRow(
                title = stringResource(R.string.chat_auto_fallback),
                subtitle = stringResource(R.string.chat_auto_fallback_desc),
                checked = settings.autoFallback,
                onChange = viewModel::setAutoFallback,
            )
        } }

        item { SettingsCard(title = stringResource(R.string.settings_theme)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val options = listOf(
                    ThemeChoice.SYSTEM to R.string.settings_theme_system,
                    ThemeChoice.LIGHT to R.string.settings_theme_light,
                    ThemeChoice.DARK to R.string.settings_theme_dark,
                )
                options.forEachIndexed { index, (choice, label) ->
                    SegmentedButton(
                        selected = settings.theme == choice,
                        onClick = { viewModel.setTheme(choice) },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    ) { Text(stringResource(label)) }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleMedium,
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val options = listOf(
                    "" to R.string.settings_language_system,
                    "en" to R.string.settings_language_en,
                    "fa" to R.string.settings_language_fa,
                )
                options.forEachIndexed { index, (tag, label) ->
                    SegmentedButton(
                        selected = settings.languageTag == tag,
                        onClick = { viewModel.setLanguage(tag) },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    ) { Text(stringResource(label)) }
                }
            }
        } }

        item { SettingsCard(title = stringResource(R.string.settings_usage)) {
            keys.filter { it.requests > 0 }
                .sortedByDescending { it.requests }
                .forEach { status ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(
                            status.provider.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            stringResource(R.string.keys_used, status.requests),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            if (settings.totalRequests == 0) {
                Text(
                    stringResource(R.string.common_no_history),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = viewModel::resetStats) {
                Text(stringResource(R.string.settings_clear_history))
            }
            TextButton(onClick = viewModel::clearChat) {
                Text(stringResource(R.string.chat_clear))
            }
        } }

        item { SettingsCard(title = stringResource(R.string.settings_about)) {
            Text(
                stringResource(R.string.settings_about_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.app_name) + " · " +
                    stringResource(R.string.settings_version, com.aipn.connect.BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(10.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** Simple holder so screens can pick the right language without threading state everywhere. */
object LocaleState {
    var isPersian: Boolean = false
}