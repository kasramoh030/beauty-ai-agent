package com.aipn.connect.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aipn.connect.R
import com.aipn.connect.data.Provider

@Composable
fun KeysScreen(
    viewModel: AppViewModel,
    isPersian: Boolean,
    openUrl: (String) -> Unit,
) {
    val keys by viewModel.keys.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Provider?>(null) }
    var deleting by remember { mutableStateOf<Provider?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                stringResource(R.string.keys_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
        }

        items(keys, key = { it.provider.id }) { status ->
            KeyCard(
                status = status,
                keyTail = viewModel.vault.keyTail(status.provider.id),
                isPersian = isPersian,
                onTest = { viewModel.testKey(status.provider.id) },
                onEdit = { editing = status.provider },
                onDelete = { deleting = status.provider },
                onToggle = { viewModel.setEnabled(status.provider.id, it) },
                onPickModel = { viewModel.setModel(status.provider.id, it) },
                onRefreshModels = { viewModel.loadModels(status.provider.id) },
                onOpenUrl = openUrl,
            )
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    editing?.let { provider ->
        KeyDialog(
            provider = provider,
            hasKey = viewModel.vault.hasKey(provider.id),
            onDismiss = { editing = null },
            onSave = { key, account ->
                viewModel.setKey(provider.id, key)
                viewModel.setAccountId(provider.id, account)
                editing = null
            },
        )
    }

    deleting?.let { provider ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.keys_delete)) },
            text = { Text(stringResource(R.string.keys_delete_confirm, provider.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setKey(provider.id, "")
                    deleting = null
                }) { Text(stringResource(R.string.keys_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text(stringResource(R.string.key_dialog_cancel))
                }
            },
        )
    }
}

@Composable
private fun KeyCard(
    status: KeyStatus,
    keyTail: String?,
    isPersian: Boolean,
    onTest: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onPickModel: (String) -> Unit,
    onRefreshModels: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val provider = status.provider
    var modelsOpen by remember { mutableStateOf(false) }

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(14.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(provider.accent)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (isPersian) provider.nameFa else provider.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        when {
                            status.hasKey -> keyTail ?: "••••••••"
                            provider.keyOptional -> stringResource(R.string.keys_no_key_needed)
                            else -> stringResource(R.string.keys_empty)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = status.enabled, onCheckedChange = onToggle)
            }

            Spacer(Modifier.height(10.dp))

            Text(
                if (isPersian) provider.freeTierFa else provider.freeTierEn,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onTest,
                    enabled = (status.hasKey || provider.keyOptional) && !status.testing,
                ) {
                    if (status.testing) {
                        CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Bolt, contentDescription = null, Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.keys_test))
                }

                TonalChip(
                    text = if (status.hasKey) stringResource(R.string.keys_edit) else stringResource(R.string.keys_add),
                    icon = if (status.hasKey) Icons.Rounded.Edit else Icons.Rounded.Add,
                    onClick = onEdit,
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { onOpenUrl(provider.keyUrl) },
                    label = { Text(stringResource(R.string.keys_get_free)) },
                    leadingIcon = {
                        Icon(Icons.Rounded.OpenInNew, contentDescription = null, Modifier.size(16.dp))
                    },
                )
                AssistChip(
                    onClick = { onOpenUrl(provider.docsUrl) },
                    label = { Text(stringResource(R.string.keys_docs)) },
                )
            }

            if (status.result != null) {
                Spacer(Modifier.height(8.dp))
                val ok = status.result.contains("models")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (ok) Icons.Rounded.Check else Icons.Rounded.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        status.result,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.settings_model) + ": " +
                        status.model.ifBlank { provider.defaultModel },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { modelsOpen = !modelsOpen }) {
                    Text(
                        if (modelsOpen) stringResource(R.string.common_close)
                        else stringResource(R.string.settings_model_pick)
                    )
                }
            }

            if (modelsOpen) {
                val models = status.models.ifEmpty { provider.models }
                Column(Modifier.fillMaxWidth()) {
                    TextButton(onClick = { onPickModel(""); modelsOpen = false }) {
                        Text(stringResource(R.string.settings_model_auto))
                    }
                    models.take(24).forEach { model ->
                        TextButton(onClick = { onPickModel(model); modelsOpen = false }) {
                            Text(model, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    TextButton(onClick = onRefreshModels) {
                        Text(stringResource(R.string.settings_model_refresh))
                    }
                }
            }

            if (status.requests > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.keys_used, status.requests),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (status.hasKey) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Rounded.Visibility,
                            contentDescription = stringResource(R.string.keys_edit),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.keys_delete),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TonalChip(text: String, icon: ImageVector, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick) {
        Icon(icon, contentDescription = null, Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text)
    }
}

@Composable
private fun KeyDialog(
    provider: Provider,
    hasKey: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var key by remember { mutableStateOf("") }
    var account by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (hasKey) stringResource(R.string.key_dialog_edit)
                else stringResource(R.string.key_dialog_new)
            )
        },
        text = {
            Column {
                Text(
                    stringResource(R.string.key_dialog_provider) + ": " + provider.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text(stringResource(R.string.key_dialog_value)) },
                    placeholder = { Text(stringResource(R.string.key_dialog_value_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (provider.needsAccountId) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = account,
                        onValueChange = { account = it },
                        label = { Text(stringResource(R.string.key_dialog_account)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(key, account) },
                enabled = provider.keyOptional || key.isNotBlank(),
            ) { Text(stringResource(R.string.key_dialog_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.key_dialog_cancel)) }
        },
    )
}