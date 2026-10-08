package com.aipn.connect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import com.aipn.connect.R
import com.aipn.connect.data.Session

/** Button text colour that stays readable on the amber accent. */
private val Ink = Color(0xFF1A1206)

/** Side drawer with the app mark, "new chat", the saved conversations and a keys shortcut. */
@Composable
fun ChatDrawer(
    history: List<Session>,
    activeId: String?,
    onNewChat: () -> Unit,
    onOpenSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onOpenKeys: () -> Unit,
    onClose: () -> Unit,
) {
    val depth = rememberDepth()
    Column(
        Modifier
            .fillMaxHeight()
            .width(310.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(18.dp))

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppMark(size = 34.dp, corner = 10.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.app_short_name),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(R.string.drawer_close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose)
                    .padding(6.dp),
            )
        }

        Spacer(Modifier.height(18.dp))

        AmberPillButton(
            text = stringResource(R.string.new_chat),
            icon = Icons.Rounded.Add,
            onClick = {
                onNewChat()
                onClose()
            },
        )

        Spacer(Modifier.height(16.dp))

        if (history.isEmpty()) {
            Text(
                stringResource(R.string.no_chats),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(history, key = { it.id }) { session ->
                    val rowShape = RoundedCornerShape(14.dp)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .raised(depth, rowShape, elevation = if (session.id == activeId) 8.dp else 2.dp)
                            .clip(rowShape)
                            .background(
                                if (session.id == activeId) SelectedTeal
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .rimLight(depth, rowShape)
                            .clickable {
                                onOpenSession(session.id)
                                onClose()
                            }
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            session.preview,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.chat_deleted),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable { onDeleteSession(session.id) }
                                .padding(5.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        val keysShape = CircleShape
        Row(
            Modifier
                .fillMaxWidth()
                .raised(depth, keysShape, elevation = 8.dp)
                .clip(keysShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .rimLight(depth, keysShape)
                .clickable {
                    onOpenKeys()
                    onClose()
                }
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Key, contentDescription = null, tint = Amber, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.search_keys), style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AmberPillButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val depth = rememberDepth()
    Row(
        Modifier
            .fillMaxWidth()
            .raised(depth, CircleShape, elevation = 12.dp, glow = 0.8f)
            .clip(CircleShape)
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    0f to Amber.copy(alpha = 0.72f),
                    1f to Amber,
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(0f, 400f),
                )
            )
            .sheen(depth.sheenColor)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, color = Ink)
    }
}

/** Model picker with a search field and a tick on the active model. */
@Composable
fun ModelSheet(
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val depth = rememberDepth()
    val sheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(sheetShape)
            .background(MaterialTheme.colorScheme.surface)
            .rimLight(depth, sheetShape)
            .padding(horizontal = 18.dp),
    ) {
        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f).sunken(depth, RoundedCornerShape(14.dp)),
                placeholder = { Text(stringResource(R.string.search_models)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Amber,
                ),
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(R.string.drawer_close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss)
                    .padding(6.dp),
            )
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(Modifier.heightIn(max = 420.dp)) {
            val usable = cards.filter { it.hasKey || it.provider.keyOptional }
            for (card in usable) {
                val models = card.models.filter { model ->
                    query.isBlank() || model.contains(query, ignoreCase = true)
                }
                if (models.isEmpty()) continue

                item(key = "header_" + card.provider.id) {
                    Text(
                        card.provider.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
                    )
                }

                items(
                    count = models.size,
                    key = { index -> card.provider.id + "_" + models[index] },
                ) { index ->
                    val model = models[index]
                    val selected = card.model == model
                    val rowShape = RoundedCornerShape(12.dp)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(rowShape)
                            .background(if (selected) SelectedTeal else Color.Transparent)
                            .rimLight(depth, rowShape)
                            .clickable {
                                viewModel.setModel(card.provider.id, model)
                                onDismiss()
                            }
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(model, style = MaterialTheme.typography.bodyLarge)
                        if (selected) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Amber,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
    }
}
