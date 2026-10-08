package com.aipn.connect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aipn.connect.R

/**
 * The API-keys page, laid out like the reference screen: a card per provider with a
 * "free" badge, a "get key" link, the key field and a custom-models field.
 */
@Composable
fun KeysScreen(
    viewModel: AppViewModel,
    onClose: (() -> Unit)? = null,
    openUrl: (String) -> Unit,
) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    var savedProvider by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {

        if (onClose != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                Text(
                    stringResource(R.string.keys_title_page),
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
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.keys_privacy_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            items(cards, key = { it.provider.id }) { card ->
                ProviderKeyCard(
                    card = card,
                    saved = savedProvider == card.provider.id,
                    onKeyChange = { viewModel.setDraftKey(card.provider.id, it) },
                    onModelsChange = { viewModel.setDraftModels(card.provider.id, it) },
                    onSave = {
                        viewModel.saveKey(card.provider.id)
                        savedProvider = card.provider.id
                    },
                    onTest = { viewModel.testKey(card.provider.id) },
                    onToggle = { viewModel.setEnabled(card.provider.id, it) },
                    onOpenUrl = { openUrl(card.provider.keyUrl) },
                )
            }

            item { Spacer(Modifier.height(6.dp)) }
        }
    }
}

@Composable
private fun ProviderKeyCard(
    card: KeyCardState,
    saved: Boolean,
    onKeyChange: (String) -> Unit,
    onModelsChange: (String) -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onOpenUrl: () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        card.provider.name,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.keys_free_tier_short),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FreeBadge()
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = card.enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = Amber,
                    ),
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.keys_get_key),
                    style = MaterialTheme.typography.labelMedium,
                    color = Amber,
                    modifier = Modifier
                        .clickable(onClick = onOpenUrl)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
                Icon(
                    Icons.Rounded.OpenInNew,
                    contentDescription = null,
                    tint = Amber,
                    modifier = Modifier.size(14.dp),
                )
            }

            Spacer(Modifier.height(10.dp))

            DarkField(
                value = card.key,
                onValueChange = onKeyChange,
                placeholder = stringResource(R.string.keys_api_key_label),
                isPassword = true,
            )

            Spacer(Modifier.height(8.dp))

            DarkField(
                value = card.customModels,
                onValueChange = onModelsChange,
                placeholder = stringResource(R.string.keys_custom_models),
                isPassword = false,
            )

            if (card.testResult != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    card.testResult,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (card.testResult.contains("models")) FreeGreen
                    else MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (card.testing) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.keys_testing),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (saved) {
                    Text(
                        stringResource(R.string.keys_saved),
                        style = MaterialTheme.typography.labelMedium,
                        color = FreeGreen,
                    )
                    Spacer(Modifier.width(10.dp))
                }
                AmberButton(
                    text = stringResource(R.string.keys_save),
                    onClick = onSave,
                    enabled = card.key.isNotBlank() || card.provider.keyOptional,
                )
            }
        }
    }
}

@Composable
private fun FreeBadge() {
    Box(
        Modifier
            .background(SelectedTeal, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            stringResource(R.string.keys_badge_free),
            style = MaterialTheme.typography.labelMedium,
            color = FreeGreen,
        )
    }
}

@Composable
private fun DarkField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyLarge) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        visualTransformation = if (isPassword) {
            androidx.compose.ui.text.input.PasswordVisualTransformation()
        } else {
            androidx.compose.ui.text.input.VisualTransformation.None
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.outline,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            cursorColor = Amber,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

@Composable
fun AmberButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Amber,
            contentColor = Color(0xFF1A1206),
            disabledContainerColor = Amber.copy(alpha = 0.35f),
            disabledContentColor = Color(0xFF1A1206).copy(alpha = 0.7f),
        ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}