package com.aipn.connect.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Visibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aipn.connect.R
import kotlinx.coroutines.launch
import java.util.Base64

@Composable
fun ChatScreen(
    viewModel: AppViewModel,
    onOpenKeys: () -> Unit,
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val sending by viewModel.sending.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var input by remember { mutableStateOf("") }
    var attachedImage by remember { mutableStateOf<Pair<String, String>?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    val hasAnyProvider = viewModel.vault.enabledIds().isNotEmpty() || com.aipn.connect.data.Providers.zeroConfig.isNotEmpty()

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch { attachedImage = viewModel.prepareImage(uri) }
        }
    }

    LaunchedEffect(messages.size, sending) {
        val target = messages.size - 1
        if (target >= 0) listState.animateScrollToItem(target)
    }

    Column(Modifier.fillMaxSize()) {

        if (sending) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        if (!hasAnyProvider) {
            NoKeysBanner(onOpenKeys)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (messages.isEmpty()) {
                item { EmptyState() }
            }
            items(messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    onCopy = { clipboard.setText(AnnotatedString(message.text)) },
                    onRegenerate = { viewModel.regenerate() },
                    onDelete = { viewModel.deleteMessage(message.id) },
                )
            }
        }

        Composer(
            input = input,
            onInputChange = { input = it },
            sending = sending,
            hasImage = attachedImage != null,
            onAttach = { pickImage.launch("image/*") },
            onClearImage = { attachedImage = null },
            onSend = {
                viewModel.send(input, attachedImage)
                input = ""
                attachedImage = null
            },
            onStop = { viewModel.stop() },
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(top = 48.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(52.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(R.string.chat_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.chat_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NoKeysBanner(onOpenKeys: () -> Unit) {
    SurfaceBanner {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.chat_no_key), style = MaterialTheme.typography.labelLarge)
                Text(
                    stringResource(R.string.chat_no_key_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onOpenKeys) { Text(stringResource(R.string.chat_open_keys)) }
        }
    }
}

@Composable
private fun SurfaceBanner(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer)
    ) { content() }
}

@Composable
private fun MessageBubble(
    message: UiMessage,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    onDelete: () -> Unit,
) {
    val isUser = message.role == "user"
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.padding(14.dp)) {
                if (message.imageData != null) {
                    val bytes = remember(message.imageData) {
                        runCatching { Base64.getDecoder().decode(message.imageData) }.getOrNull()
                    }
                    if (bytes != null) {
                        Image(
                            bitmap = remember(bytes) { bytes.toBitmap() },
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn()
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(10.dp)),
                        )
                    }
                }

                if (message.pending) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.chat_thinking),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else if (message.error != null) {
                    Text(
                        stringResource(R.string.chat_failed) + ": " + message.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    MarkdownText(message.text)
                }

                if (!isUser && message.providerName != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            buildString {
                                append(stringResource(R.string.chat_answered_by, message.providerName))
                                if (!message.model.isNullOrBlank()) append(" · ${message.model}")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (message.fellBackFrom != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.chat_fell_back_to, message.fellBackFrom),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }

        if (!isUser && !message.pending && message.text.isNotBlank()) {
            Row(
                Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TinyAction(Icons.Rounded.ContentCopy, stringResource(R.string.chat_copy), onCopy)
                TinyAction(Icons.Rounded.Refresh, stringResource(R.string.chat_regenerate), onRegenerate)
                TinyAction(Icons.Rounded.DeleteOutline, stringResource(R.string.common_close), onDelete)
            }
        }
    }
}

@Composable
private fun TinyAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(34.dp)) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun Composer(
    input: String,
    onInputChange: (String) -> Unit,
    sending: Boolean,
    hasImage: Boolean,
    onAttach: () -> Unit,
    onClearImage: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(10.dp)
    ) {
        if (hasImage) {
            AssistChip(
                onClick = onClearImage,
                label = { Text(stringResource(R.string.chat_attach)) },
                leadingIcon = { Icon(Icons.Rounded.Image, contentDescription = null, Modifier.size(16.dp)) },
                trailingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null, Modifier.size(16.dp)) },
            )
            Spacer(Modifier.height(6.dp))
        }
        Row(verticalAlignment = Alignment.Bottom) {
            IconButton(onClick = onAttach, enabled = !sending) {
                Icon(Icons.Rounded.Image, contentDescription = stringResource(R.string.chat_attach))
            }
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.chat_hint)) },
                maxLines = 5,
                shape = RoundedCornerShape(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            if (sending) {
                FilledIconButton(
                    onClick = onStop,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                ) {
                    Icon(Icons.Rounded.Stop, contentDescription = stringResource(R.string.chat_stop))
                }
            } else {
                FilledIconButton(
                    onClick = onSend,
                    enabled = input.isNotBlank() || hasImage,
                ) {
                    Icon(Icons.Rounded.Send, contentDescription = stringResource(R.string.chat_send))
                }
            }
        }
    }
}

private fun ByteArray.toBitmap(): android.graphics.Bitmap =
    android.graphics.BitmapFactory.decodeByteArray(this, 0, size)ay.toBitmap(): android.graphics.Bitmap =
    android.graphics.BitmapFactory.decodeByteArray(this, 0, size)