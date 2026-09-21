/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.roomlist

import io.element.android.features.home.impl.model.RoomListRoomSummary
import io.element.android.libraries.matrix.api.core.RoomId

sealed interface RoomListEvent {
    data class UpdateVisibleRange(val range: IntRange) : RoomListEvent
    data object DismissRequestVerificationPrompt : RoomListEvent
    data object DismissBanner : RoomListEvent
    data object DismissNewNotificationSoundBanner : RoomListEvent
    data object ToggleSearchResults : RoomListEvent
    data class ShowContextMenu(val roomSummary: RoomListRoomSummary) : RoomListEvent

    data class AcceptInvite(val roomSummary: RoomListRoomSummary) : RoomListEvent
    data class DeclineInvite(val roomSummary: RoomListRoomSummary, val blockUser: Boolean) : RoomListEvent
    data class ShowDeclineInviteMenu(val roomSummary: RoomListRoomSummary) : RoomListEvent
    data object HideDeclineInviteMenu : RoomListEvent

    sealed interface ContextMenuEvent : RoomListEvent
    data object HideContextMenu : ContextMenuEvent
    data class LeaveRoom(val roomId: RoomId, val needsConfirmation: Boolean) : ContextMenuEvent
    data class MarkAsRead(val roomId: RoomId) : ContextMenuEvent
    data class MarkAsUnread(val roomId: RoomId) : ContextMenuEvent
    data class SetRoomIsFavorite(val roomId: RoomId, val isFavorite: Boolean) : ContextMenuEvent
    data class ShowOrganizeInSpaces(val roomId: RoomId) : ContextMenuEvent
    data object HideOrganizeInSpaces : RoomListEvent
    data class ToggleSpaceMembership(val spaceId: RoomId, val isMember: Boolean) : RoomListEvent
    data class ShowManageLabels(val roomId: RoomId, val roomName: String?) : ContextMenuEvent
    data object HideManageLabels : RoomListEvent
    data class ToggleLabelMembership(val labelId: String, val isAssigned: Boolean) : RoomListEvent
    data object ShowCreateLabel : RoomListEvent
    data class ShowEditLabel(val label: io.element.android.libraries.matrix.api.labels.RoomLabel) : RoomListEvent
    data object HideLabelEditor : RoomListEvent
    data class SaveLabel(val title: String, val emoji: String?, val isShownInInbox: Boolean) : RoomListEvent
    data class DeleteLabel(val labelId: String) : RoomListEvent

    // Merge events — permite fusionar/desfusionar desde el context menu de la lista
    data class ShowMergePicker(val roomId: RoomId, val roomName: String?) : ContextMenuEvent
    data object HideMergePicker : RoomListEvent
    data class ExecuteMerge(val sourceRoomId: RoomId, val targetRoomId: RoomId) : RoomListEvent
    data class UnmergeFromList(val roomId: RoomId) : ContextMenuEvent

    // Label filter — selecciona/limpia filtro de label activo en la barra de chips
    data class SelectLabelFilter(val label: io.element.android.libraries.matrix.api.labels.RoomLabel?) : RoomListEvent
}
