/*
 * Copyright (c) 2026 Element Creations Ltd. / FluffyBeep
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.merge

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.home.impl.model.RoomListRoomSummary
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.theme.components.ModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.utils.lazyColumnContentPadding
import io.element.android.libraries.matrix.api.core.RoomId
import kotlinx.collections.immutable.ImmutableList

/**
 * Bottom sheet que muestra las salas disponibles para fusionar con la sala fuente.
 * Lanzado desde el context menu de la lista de chats vía RoomListEvent.ShowMergePicker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeRoomPickerBottomSheet(
    sourceRoomName: String?,
    availableRooms: ImmutableList<RoomListRoomSummary>,
    onRoomSelected: (RoomId) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        scrollable = false,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Text(
                text = if (sourceRoomName != null) "Fusionar \"$sourceRoomName\" con..." else "Fusionar chat con...",
                style = ElementTheme.typography.fontHeadingSmMedium,
                color = ElementTheme.colors.textPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (availableRooms.isEmpty()) {
                Text(
                    text = "No hay chats disponibles para fusionar",
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = ElementTheme.colors.textSecondary,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .fillMaxWidth(),
                )
            } else {
                LazyColumn(
                    contentPadding = lazyColumnContentPadding,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(availableRooms, key = { it.roomId.value }) { room ->
                        MergeRoomItem(
                            room = room,
                            onClick = {
                                onRoomSelected(room.roomId)
                                onDismissRequest()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MergeRoomItem(
    room: RoomListRoomSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            avatarData = room.avatarData,
            avatarType = if (room.isSpace) {
                AvatarType.Space(isTombstoned = room.isTombstoned)
            } else {
                AvatarType.Room(
                    heroes = room.heroes,
                    isTombstoned = room.isTombstoned,
                )
            },
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = room.name ?: room.roomId.value,
                style = ElementTheme.typography.fontBodyLgMedium,
                color = ElementTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (room.networkBadges.isNotEmpty()) {
                Text(
                    text = room.networkBadges.joinToString(" · "),
                    style = ElementTheme.typography.fontBodySmRegular,
                    color = ElementTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
