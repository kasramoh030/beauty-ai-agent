package com.aipn.connect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aipn.connect.R
import com.aipn.connect.ThemeChoice

@Composable
fun SettingsScreen(viewModel: AppViewModel, onClose: () -> Unit, openUrl: (String) -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    var prompt by remember(settings.systemPrompt) { mutableStateOf(settings.systemPrompt) }
    val depth = rememberDepth()

    AmbientBackground(depth) {
        Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.settings_title_page),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.drawer_close),
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        item { SettingsCard(title = stringResource(R.string.settings_model_pick)) {
            cards.filter { it.enabled && (it.hasKey || it.provider.keyOptional) }
                .forEach { status ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(
                            status.provider.name,
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

        item { SettingsCard(title = stringResource(R.string.settings_generation)) {
            Text(
                stringResource(R.string.settings_temperature, settings.temperature),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DepthSlider(
                value = settings.temperature,
                onValueChange = { viewModel.setTemperature(it) },
                valueRange = 0f..2f,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.settings_max_tokens_value, settings.maxTokens),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DepthSlider(
                value = settings.maxTokens.toFloat(),
                onValueChange = { viewModel.setMaxTokens(roundToStep(it, 128f).toInt()) },
                valueRange = 128f..8192f,
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
            // Seven palettes will not fit in one segmented row, and a swatch per
            // theme is the only way to show what the picker is actually choosing.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (choice in ThemeChoice.entries) {
                    ThemeChip(
                        label = stringResource(themeLabelOf(choice)),
                        swatch = themeSwatch(choice),
                        selected = settings.theme == choice,
                        onClick = { viewModel.setTheme(choice) },
                    )
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
            cards.filter { it.requests > 0 }
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
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {
                openUrl("https://github.com/kasramoh030/beauty-ai-agent")
            }) {
                Text(stringResource(R.string.settings_source), style = MaterialTheme.typography.labelMedium)
            }
        } }
        }
        }
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

/**
 * A plain amber track instead of the stock M3 slider, whose dotted tick marks
 * turned a two-value card into a ruler. The value is quantised by the caller.
 */
@Composable
private fun DepthSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
) {
    val track = MaterialTheme.colorScheme.outlineVariant
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = track,
        ),
    )
}

private fun roundToStep(value: Float, step: Float): Float =
    (value / step).let { kotlin.math.round(it) } * step

@Composable
private fun themeLabelOf(choice: ThemeChoice): Int = when (choice) {
    ThemeChoice.SYSTEM -> R.string.settings_theme_system
    ThemeChoice.LIGHT -> R.string.settings_theme_light
    ThemeChoice.DARK -> R.string.settings_theme_dark
    ThemeChoice.MIDNIGHT -> R.string.settings_theme_midnight
    ThemeChoice.OLED -> R.string.settings_theme_oled
    ThemeChoice.SAND -> R.string.settings_theme_sand
    ThemeChoice.OCEAN -> R.string.settings_theme_ocean
}

/** Background, surface and accent of the palette this choice resolves to. */
@Composable
private fun themeSwatch(choice: ThemeChoice): List<Color> =
    paletteFor(choice, systemDark = false).let { scheme ->
        listOf(scheme.background, scheme.surfaceVariant, scheme.primary)
    }

@Composable
private fun ThemeChip(
    label: String,
    swatch: List<Color>,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
        ) {
            swatch.forEach { color ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(color)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
        )
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
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedBorderColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}
