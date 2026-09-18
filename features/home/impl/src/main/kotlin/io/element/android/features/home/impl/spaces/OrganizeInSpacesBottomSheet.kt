/*
 * Copyright (c) 2026 Element Creations Ltd. / FluffyBeep
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.spaces

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
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.theme.components.ModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.utils.scaffoldScrollableContentInsets
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.spaces.SpaceServiceFilter
import io.element.android.libraries.matrix.ui.model.getAvatarData
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizeInSpacesBottomSheet(
    roomName: String?,
    spaceFilters: ImmutableList<SpaceServiceFilter>,
    memberSpaceIds: Set<RoomId>,
    onToggleSpace: (spaceId: RoomId, isMember: Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    mergedRoomCount: Int = 1,
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
                text = "Organizar en Espacios",
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
                    text = "Contacto fusionado ($mergedRoomCount redes vinculadas). Se asignarán todas juntas.",
                    style = ElementTheme.typography.fontBodyXsRegular,
                    color = ElementTheme.colors.textActionAccent,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (spaceFilters.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No tienes espacios disponibles.",
                        style = ElementTheme.typography.fontBodyMdRegular,
                        color = ElementTheme.colors.textSecondary,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(spaceFilters) { filter ->
                        val spaceRoom = filter.spaceRoom
                        val isMember = memberSpaceIds.contains(spaceRoom.roomId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleSpace(spaceRoom.roomId, isMember) }
                                .padding(horizontal = 24.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Avatar(
                                avatarData = spaceRoom.getAvatarData(AvatarSize.RoomListItem),
                                avatarType = AvatarType.Space(),
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = spaceRoom.displayName,
                                    style = ElementTheme.typography.fontBodyLgMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                val memberCountText = if (filter.descendants.isNotEmpty()) {
                                    "${filter.descendants.size} chats"
                                } else {
                                    "0 chats"
                                }
                                Text(
                                    text = memberCountText,
                                    style = ElementTheme.typography.fontBodySmRegular,
                                    color = ElementTheme.colors.textSecondary,
                                )
                            }
                            Checkbox(
                                checked = isMember,
                                onCheckedChange = { onToggleSpace(spaceRoom.roomId, isMember) }
                            )
                        }
                    }
                }
            }
        }
    }
}
