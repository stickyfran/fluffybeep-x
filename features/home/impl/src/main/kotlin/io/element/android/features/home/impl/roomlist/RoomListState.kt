/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.roomlist

import androidx.compose.runtime.Immutable
import io.element.android.features.home.impl.filters.RoomListFiltersState
import io.element.android.features.home.impl.model.RoomListRoomSummary
import io.element.android.features.home.impl.search.GlobalSearchState
import io.element.android.features.home.impl.search.RoomListSearchState
import io.element.android.features.home.impl.spacefilters.SpaceFiltersState
import io.element.android.features.invite.api.acceptdecline.AcceptDeclineInviteState
import io.element.android.features.leaveroom.api.LeaveRoomState
import io.element.android.libraries.fullscreenintent.api.FullScreenIntentPermissionsState
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.push.api.battery.BatteryOptimizationState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

data class RoomListState(
    val contextMenu: ContextMenu,
    val declineInviteMenu: DeclineInviteMenu,
    val leaveRoomState: LeaveRoomState,
    val filtersState: RoomListFiltersState,
    val searchState: RoomListSearchState,
    val globalSearchState: GlobalSearchState,
    val spaceFiltersState: SpaceFiltersState,
    val contentState: RoomListContentState,
    val acceptDeclineInviteState: AcceptDeclineInviteState,
    val hideInvitesAvatars: Boolean,
    val canReportRoom: Boolean,
    val organizeInSpaces: OrganizeInSpaces? = null,
    val manageLabels: ManageLabels? = null,
    val isCreatingLabel: Boolean = false,
    val labelToEdit: io.element.android.libraries.matrix.api.labels.RoomLabel? = null,
    val mergePicker: MergePicker? = null,
    val activeLabelFilter: io.element.android.libraries.matrix.api.labels.RoomLabel? = null,
    val allLabels: List<io.element.android.libraries.matrix.api.labels.RoomLabel> = emptyList(),
    val eventSink: (RoomListEvent) -> Unit,
) {
    val displayFilters = contentState is RoomListContentState.Rooms

    data class OrganizeInSpaces(
        val roomId: RoomId,
        val roomName: String?,
        val memberSpaceIds: ImmutableSet<RoomId>,
        val mergedRoomCount: Int = 1,
    )

    data class ManageLabels(
        val roomId: RoomId,
        val roomName: String?,
        val labels: ImmutableList<io.element.android.libraries.matrix.api.labels.RoomLabel>,
        val mergedRoomCount: Int = 1,
    )

    /**
     * Represents the state for the "Fusionar con..." bottom sheet picker,
     * shown when the user wants to merge two rooms from the room list.
     */
    data class MergePicker(
        val sourceRoomId: RoomId,
        val sourceRoomName: String?,
        val availableRooms: ImmutableList<io.element.android.features.home.impl.model.RoomListRoomSummary>,
    )

    sealed interface ContextMenu {
        data object Hidden : ContextMenu
        data class Shown(
            val roomId: RoomId,
            val roomName: String?,
            val isDm: Boolean,
            val isFavorite: Boolean,
            val hasNewContent: Boolean,
            val isMerged: Boolean = false,
        ) : ContextMenu
    }

    sealed interface DeclineInviteMenu {
        data object Hidden : DeclineInviteMenu
        data class Shown(val roomSummary: RoomListRoomSummary) : DeclineInviteMenu
    }
}

enum class SecurityBannerState {
    None,
    SetUpRecovery,
    RecoveryKeyConfirmation,
}

@Immutable
sealed interface RoomListContentState {
    data class Skeleton(val count: Int) : RoomListContentState
    data class Empty(
        val securityBannerState: SecurityBannerState,
    ) : RoomListContentState

    data class Rooms(
        val securityBannerState: SecurityBannerState,
        val fullScreenIntentPermissionsState: FullScreenIntentPermissionsState,
        val batteryOptimizationState: BatteryOptimizationState,
        val showNewNotificationSoundBanner: Boolean,
        val showUnreadCount: Boolean,
        val summaries: ImmutableList<RoomListRoomSummary>,
        val seenRoomInvites: ImmutableSet<RoomId>,
    ) : RoomListContentState
}
