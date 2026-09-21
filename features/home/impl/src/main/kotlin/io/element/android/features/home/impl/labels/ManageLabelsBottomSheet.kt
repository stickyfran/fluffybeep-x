/*
 * Copyright (c) 2026 Element Creations Ltd. / FluffyBeep
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.labels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.ModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.utils.scaffoldScrollableContentInsets
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.labels.RoomLabel
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageLabelsBottomSheet(
    roomName: String?,
    roomId: RoomId,
    labels: ImmutableList<RoomLabel>,
    onToggleLabel: (labelId: String, isAssigned: Boolean) -> Unit,
    onCreateLabelClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    mergedRoomCount: Int = 1,
    onEditLabelClick: ((RoomLabel) -> Unit)? = null,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        scrollable = false,
        contentWindowInsets = { scaffoldScrollableContentInsets },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Etiquetas del chat",
                style = ElementTheme.typography.fontHeadingSmMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            if (!roomName.isNullOrBlank()) {
                Text(
                    text = roomName,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = ElementTheme.colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (mergedRoomCount > 1) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Contacto fusionado ($mergedRoomCount redes). Se etiquetarán todas juntas.",
                    style = ElementTheme.typography.fontBodyXsRegular,
                    color = ElementTheme.colors.textActionAccent,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (labels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No tienes etiquetas creadas todavía.",
                        style = ElementTheme.typography.fontBodyMdRegular,
                        color = ElementTheme.colors.textSecondary,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    items(labels, key = { it.id }) { label ->
                        val isAssigned = label.roomIds.contains(roomId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleLabel(label.id, !isAssigned) }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = label.emoji ?: "🏷️",
                                style = ElementTheme.typography.fontBodyLgMedium,
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label.title,
                                    style = ElementTheme.typography.fontBodyLgMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (!label.isShownInInbox) {
                                    Text(
                                        text = "Oculta del inbox principal",
                                        style = ElementTheme.typography.fontBodyXsRegular,
                                        color = ElementTheme.colors.textSecondary,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            if (onEditLabelClick != null) {
                                IconButton(
                                    onClick = { onEditLabelClick(label) },
                                ) {
                                    Icon(
                                        imageVector = CompoundIcons.Edit(),
                                        contentDescription = "Editar",
                                        tint = ElementTheme.colors.iconSecondary,
                                    )
                                }
                            }
                            Checkbox(
                                checked = isAssigned,
                                onCheckedChange = { onToggleLabel(label.id, it) },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            ) {
                Button(
                    text = "Nueva etiqueta",
                    onClick = onCreateLabelClick,
                    leadingIcon = {
                        Icon(
                            imageVector = CompoundIcons.Plus(),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
