package com.aipn.connect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aipn.connect.R
import kotlinx.coroutines.launch
import java.util.Base64

/** The app mark: an amber four-point sparkle on a teal tile, as in the reference. */
@Composable
fun AppMark(size: androidx.compose.ui.unit.Dp = 56.dp, corner: androidx.compose.ui.unit.Dp = 16.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(Teal),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Bolt,
            contentDescription = null,
            tint = Amber,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

@Composable
fun ChatScreen(
    viewModel: AppViewModel,
    onOpenSettings: () -> Unit,
    onOpenKeys: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenDrawer: () -> Unit,
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val sending by viewModel.sending.collectAsStateWithLifecycle()

    var input by remember { mutableStateOf("") }
    var attachedImage by remember { mutableStateOf<Pair<String, String>?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    val model = viewModel.currentModel()

    LaunchedEffect(messages.size, sending) {
        val target = messages.size - 1
        if (target >= 0) listState.animateScrollToItem(target)
    }

    Column(Modifier.fillMaxSize()) {

        ChatTopBar(
            model = model,
            onMenu = onOpenDrawer,
            onSettings = onOpenSettings,
            onModel = onOpenModels,
        )

        Box(Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (messages.isEmpty()) {
                    item { EmptyState(viewModel, onOpenKeys) }
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
        }

        Composer(
            input = input,
            onInputChange = { input = it },
            sending = sending,
            hasImage = attachedImage != null,
            onImagePicked = { uri -> scope.launch { attachedImage = viewModel.prepareImage(uri) } },
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

/** Header: hamburger on the trailing side, model pill in the middle, gear at the far side. */
@Composable
private fun ChatTopBar(
    model: String,
    onMenu: () -> Unit,
    onSettings: () -> Unit,
    onModel: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onSettings) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = stringResource(R.string.settings_gear),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.width(6.dp))

        Row(
            Modifier
                .weight(1f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .clickable(onClick = onModel)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                model,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Rounded.Bolt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(Modifier.width(6.dp))

        IconButton(onClick = onMenu) {
            Icon(
                Icons.Rounded.Menu,
                contentDescription = stringResource(R.string.drawer_open),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun EmptyState(viewModel: AppViewModel, onOpenKeys: () -> Unit) {
    val active by remember { mutableStateOf(viewModel.activeModel()) }
    val suggested = remember { viewModel.suggestedProvider() }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppMark(size = 64.dp, corner = 18.dp)

        Spacer(Modifier.height(22.dp))

        Text(
            stringResource(R.string.chat_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            if (active != null) {
                stringResource(R.string.active_model, active!!.first, active!!.second)
            } else {
                stringResource(R.string.active_model_none)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (active == null && suggested != null) {
            Spacer(Modifier.height(26.dp))
            Text(
                stringResource(R.string.add_key_for, suggested.name),
                style = MaterialTheme.typography.titleMedium,
                color = Amber,
                modifier = Modifier
                    .clip(CircleShape)
                    .border(1.dp, Amber, CircleShape)
                    .clickable(onClick = onOpenKeys)
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            )
        }
    }
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
        Column(
            Modifier
                .widthIn()
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(14.dp),
        ) {
            if (message.imageData != null) {
                val image = remember(message.imageData) {
                    runCatching {
                        Base64.getDecoder().decode(message.imageData).toBitmap().asImageBitmap()
                    }.getOrNull()
                }
                if (image != null) {
                    androidx.compose.foundation.Image(
                        bitmap = image,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .clip(RoundedCornerShape(14.dp)),
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            when {
                message.pending -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.chat_thinking),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                message.error != null -> Text(
                    stringResource(R.string.chat_failed) + ": " + message.error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )

                else -> MarkdownText(message.text)
            }

            if (!isUser && message.providerName != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    message.providerName + (message.model?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (!isUser && !message.pending && message.text.isNotBlank()) {
            Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                TinyAction(Icons.Rounded.ContentCopy, stringResource(R.string.chat_copy), onCopy)
                TinyAction(Icons.Rounded.Refresh, stringResource(R.string.chat_regenerate), onRegenerate)
                TinyAction(Icons.Rounded.DeleteOutline, stringResource(R.string.chat_deleted), onDelete)
            }
        }
    }
}

private fun Modifier.widthIn(): Modifier = this

@Composable
private fun TinyAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(34.dp)) {
        Icon(
            icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun Composer(
    input: String,
    onInputChange: (String) -> Unit,
    sending: Boolean,
    hasImage: Boolean,
    onImagePicked: (android.net.Uri) -> Unit,
    onClearImage: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) onImagePicked(uri) }

    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasImage) {
            IconButton(onClick = onClearImage) {
                Icon(
                    Icons.Rounded.Image,
                    contentDescription = stringResource(R.string.chat_attach),
                    tint = Amber,
                )
            }
        }

        Box(
            Modifier
                .weight(1f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Amber),
                maxLines = 5,
                decorationBox = { inner ->
                    if (input.isEmpty()) {
                        Text(
                            stringResource(R.string.send_message),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    inner()
                },
            )
        }

        Spacer(Modifier.width(10.dp))

        if (sending) {
            IconButton(
                onClick = onStop,
                modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error),
            ) {
                Icon(
                    Icons.Rounded.Stop,
                    contentDescription = stringResource(R.string.chat_stop),
                    tint = Color.White,
                )
            }
        } else {
            IconButton(
                onClick = onSend,
                enabled = input.isNotBlank() || hasImage,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (input.isNotBlank() || hasImage) Amber else MaterialTheme.colorScheme.outline),
            ) {
                Icon(
                    Icons.Rounded.Send,
                    contentDescription = stringResource(R.string.chat_send),
                    tint = Color(0xFF1A1206),
                )
            }
        }
    }
}

private fun ByteArray.toBitmap(): android.graphics.Bitmap =
    android.graphics.BitmapFactory.decodeByteArray(this, 0, size)