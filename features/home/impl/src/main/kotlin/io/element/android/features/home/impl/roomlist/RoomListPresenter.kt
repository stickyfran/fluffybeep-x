/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.roomlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import dev.zacsweers.metro.Inject
import im.vector.app.features.analytics.plan.Interaction
import io.element.android.features.announcement.api.Announcement
import io.element.android.features.announcement.api.AnnouncementService
import io.element.android.features.home.impl.datasource.RoomListDataSource
import io.element.android.features.home.impl.model.RoomListRoomSummary
import io.element.android.features.home.impl.filters.RoomListFiltersState
import io.element.android.features.home.impl.filters.into
import io.element.android.features.home.impl.search.GlobalSearchState
import io.element.android.features.home.impl.search.RoomListSearchEvent
import io.element.android.features.home.impl.search.RoomListSearchState
import io.element.android.features.home.impl.spacefilters.SpaceFiltersState
import io.element.android.features.home.impl.spacefilters.into
import io.element.android.features.home.impl.spacefilters.selectedFilter
import io.element.android.features.invite.api.SeenInvitesStore
import io.element.android.features.invite.api.acceptdecline.AcceptDeclineInviteEvent.AcceptInvite
import io.element.android.features.invite.api.acceptdecline.AcceptDeclineInviteEvent.DeclineInvite
import io.element.android.features.invite.api.acceptdecline.AcceptDeclineInviteState
import io.element.android.features.leaveroom.api.LeaveRoomEvent
import io.element.android.features.leaveroom.api.LeaveRoomState
import io.element.android.features.preferences.impl.tasks.MarkRoomAsRead
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.featureflag.api.FeatureFlagService
import io.element.android.libraries.featureflag.api.FeatureFlags
import io.element.android.libraries.fullscreenintent.api.FullScreenIntentPermissionsState
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.contactmerge.MergedContact
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.encryption.RecoveryState
import io.element.android.libraries.matrix.api.labels.RoomLabel
import io.element.android.libraries.matrix.api.roomlist.RoomList
import io.element.android.libraries.matrix.api.roomlist.RoomListFilter
import io.element.android.libraries.matrix.ui.safety.rememberHideInvitesAvatar
import io.element.android.libraries.push.api.battery.BatteryOptimizationState
import io.element.android.services.analytics.api.AnalyticsService
import io.element.android.services.analytics.api.watchers.AnalyticsColdStartWatcher
import io.element.android.services.analyticsproviders.api.trackers.captureInteraction
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

@Inject
class RoomListPresenter(
    private val client: MatrixClient,
    private val leaveRoomPresenter: Presenter<LeaveRoomState>,
    private val roomListDataSource: RoomListDataSource,
    private val filtersPresenter: Presenter<RoomListFiltersState>,
    private val searchPresenter: Presenter<RoomListSearchState>,
    private val analyticsService: AnalyticsService,
    private val acceptDeclineInvitePresenter: Presenter<AcceptDeclineInviteState>,
    private val fullScreenIntentPermissionsPresenter: Presenter<FullScreenIntentPermissionsState>,
    private val batteryOptimizationPresenter: Presenter<BatteryOptimizationState>,
    private val markRoomAsRead: MarkRoomAsRead,
    private val seenInvitesStore: SeenInvitesStore,
    private val announcementService: AnnouncementService,
    private val coldStartWatcher: AnalyticsColdStartWatcher,
    private val spaceFiltersPresenter: Presenter<SpaceFiltersState>,
    private val globalSearchPresenter: Presenter<GlobalSearchState>,
    private val featureFlagService: FeatureFlagService,
) : Presenter<RoomListState> {
    private val encryptionService = client.encryptionService

    @Composable
    override fun present(): RoomListState {
        val coroutineScope = rememberCoroutineScope()
        val leaveRoomState = leaveRoomPresenter.present()
        val filtersState = filtersPresenter.present()
        val searchState = searchPresenter.present()
        val spaceFiltersState = spaceFiltersPresenter.present()
        val acceptDeclineInviteState = acceptDeclineInvitePresenter.present()
        val globalSearchState = globalSearchPresenter.present()

        LaunchedEffect(Unit) {
            roomListDataSource.launchIn(this)
        }

        var securityBannerDismissed by rememberSaveable { mutableStateOf(false) }
        val showNewNotificationSoundBanner by remember {
            announcementService.announcementsToShowFlow().map { announcements ->
                announcements.contains(Announcement.NewNotificationSound)
            }
        }.collectAsState(false)

        // Avatar indicator
        val hideInvitesAvatar by client.rememberHideInvitesAvatar()

        val contextMenu = remember { mutableStateOf<RoomListState.ContextMenu>(RoomListState.ContextMenu.Hidden) }
        val declineInviteMenu = remember { mutableStateOf<RoomListState.DeclineInviteMenu>(RoomListState.DeclineInviteMenu.Hidden) }
        val organizeInSpaces = remember { mutableStateOf<RoomListState.OrganizeInSpaces?>(null) }
        val activeLabelsTarget = remember { mutableStateOf<ActiveLabelsTarget?>(null) }
        val isCreatingLabel = remember { mutableStateOf(false) }
        val labelToEdit = remember { mutableStateOf<RoomLabel?>(null) }
        val allLabels by client.labelService.labels.collectAsState()
        val mergePicker = remember { mutableStateOf<RoomListState.MergePicker?>(null) }
        val activeLabelFilter = remember { mutableStateOf<RoomLabel?>(null) }

        fun handleEvent(event: RoomListEvent) {
            when (event) {
                is RoomListEvent.UpdateVisibleRange -> coroutineScope.launch {
                    roomListDataSource.updateVisibleRange(event.range)
                }
                RoomListEvent.DismissRequestVerificationPrompt -> securityBannerDismissed = true
                RoomListEvent.DismissBanner -> securityBannerDismissed = true
                RoomListEvent.DismissNewNotificationSoundBanner -> coroutineScope.launch {
                    announcementService.onAnnouncementDismissed(Announcement.NewNotificationSound)
                }
                RoomListEvent.ToggleSearchResults -> searchState.eventSink(RoomListSearchEvent.ToggleSearchVisibility)
                is RoomListEvent.ShowContextMenu -> {
                    coroutineScope.showContextMenu(event, contextMenu)
                }
                is RoomListEvent.HideContextMenu -> {
                    contextMenu.value = RoomListState.ContextMenu.Hidden
                }
                is RoomListEvent.ShowOrganizeInSpaces -> {
                    contextMenu.value = RoomListState.ContextMenu.Hidden
                    coroutineScope.launch {
                        val mergedContact = client.contactMergeService.getMergedContactForRoom(event.roomId)
                        val roomIds = mergedContact?.roomIds ?: listOf(event.roomId)
                        val room = client.getRoom(event.roomId)
                        val roomName = mergedContact?.displayName ?: room?.use { it.roomInfoFlow.value.name }
                        val joinedSpaces = client.spaceService.joinedParents(event.roomId).getOrNull().orEmpty()
                        val memberSpaceIds = joinedSpaces.map { it.roomId }.toSet()
                        organizeInSpaces.value = RoomListState.OrganizeInSpaces(
                            roomId = event.roomId,
                            roomName = roomName,
                            memberSpaceIds = memberSpaceIds.toImmutableSet(),
                            mergedRoomCount = roomIds.size,
                        )
                    }
                }
                RoomListEvent.HideOrganizeInSpaces -> {
                    organizeInSpaces.value = null
                }
                is RoomListEvent.ToggleSpaceMembership -> {
                    val current = organizeInSpaces.value
                    if (current != null) {
                        val newSet = if (event.isMember) {
                            current.memberSpaceIds - event.spaceId
                        } else {
                            current.memberSpaceIds + event.spaceId
                        }
                        organizeInSpaces.value = current.copy(memberSpaceIds = newSet.toImmutableSet())
                        coroutineScope.launch {
                            val mergedContact = client.contactMergeService.getMergedContactForRoom(current.roomId)
                            val roomIds = mergedContact?.roomIds ?: listOf(current.roomId)
                            if (event.isMember) {
                                client.spaceService.removeRoomsFromSpace(event.spaceId, roomIds)
                            } else {
                                client.spaceService.addRoomsToSpace(event.spaceId, roomIds)
                            }
                        }
                    }
                }
                is RoomListEvent.ShowManageLabels -> {
                    contextMenu.value = RoomListState.ContextMenu.Hidden
                    coroutineScope.launch {
                        val mergedContact = client.contactMergeService.getMergedContactForRoom(event.roomId)
                        val roomIds = mergedContact?.roomIds ?: listOf(event.roomId)
                        val room = client.getRoom(event.roomId)
                        val roomName = event.roomName ?: mergedContact?.displayName ?: room?.use { it.roomInfoFlow.value.name }
                        activeLabelsTarget.value = ActiveLabelsTarget(
                            roomId = event.roomId,
                            roomName = roomName,
                            mergedRoomCount = roomIds.size,
                        )
                    }
                }
                RoomListEvent.HideManageLabels -> {
                    activeLabelsTarget.value = null
                }
                is RoomListEvent.ToggleLabelMembership -> {
                    val current = activeLabelsTarget.value
                    if (current != null) {
                        coroutineScope.launch {
                            val mergedContact = client.contactMergeService.getMergedContactForRoom(current.roomId)
                            val roomIds = mergedContact?.roomIds ?: listOf(current.roomId)
                            if (event.isAssigned) {
                                client.labelService.addRoomsToLabel(event.labelId, roomIds)
                            } else {
                                client.labelService.removeRoomsFromLabel(event.labelId, roomIds)
                            }
                        }
                    }
                }
                RoomListEvent.ShowCreateLabel -> {
                    labelToEdit.value = null
                    isCreatingLabel.value = true
                }
                is RoomListEvent.ShowEditLabel -> {
                    labelToEdit.value = event.label
                    isCreatingLabel.value = false
                }
                RoomListEvent.HideLabelEditor -> {
                    isCreatingLabel.value = false
                    labelToEdit.value = null
                }
                is RoomListEvent.SaveLabel -> {
                    val toEdit = labelToEdit.value
                    val targetSnapshot = activeLabelsTarget.value
                    coroutineScope.launch {
                        if (toEdit != null) {
                            client.labelService.updateLabel(
                                labelId = toEdit.id,
                                title = event.title,
                                emoji = event.emoji,
                                isShownInInbox = event.isShownInInbox,
                            )
                        } else {
                            val createdResult = client.labelService.createLabel(
                                title = event.title,
                                emoji = event.emoji,
                                isShownInInbox = event.isShownInInbox,
                            )
                            val created = createdResult.getOrNull()
                            if (targetSnapshot != null && created != null) {
                                val mergedContact = client.contactMergeService.getMergedContactForRoom(targetSnapshot.roomId)
                                val roomIds = mergedContact?.roomIds ?: listOf(targetSnapshot.roomId)
                                client.labelService.addRoomsToLabel(created.id, roomIds)
                            }
                        }
                        isCreatingLabel.value = false
                        labelToEdit.value = null
                    }
                }
                is RoomListEvent.DeleteLabel -> {
                    coroutineScope.launch {
                        client.labelService.deleteLabel(event.labelId)
                        isCreatingLabel.value = false
                        labelToEdit.value = null
                    }
                }
                is RoomListEvent.ShowMergePicker -> {
                    contextMenu.value = RoomListState.ContextMenu.Hidden
                    coroutineScope.launch {
                        val mergedContact = client.contactMergeService.getMergedContactForRoom(event.roomId)
                        val existingMergedIds: Set<RoomId> = mergedContact?.roomIds?.toSet() ?: setOf(event.roomId)
                        // Load current summaries from the room list and exclude already-merged rooms and DMs
                        val rawSummaries = roomListDataSource.roomSummariesFlow.firstOrNull().orEmpty()
                        val available = rawSummaries.filter { s ->
                            s.roomId !in existingMergedIds && !s.isDm
                        }.toImmutableList()
                        mergePicker.value = RoomListState.MergePicker(
                            sourceRoomId = event.roomId,
                            sourceRoomName = event.roomName,
                            availableRooms = available,
                        )
                    }
                }
                RoomListEvent.HideMergePicker -> {
                    mergePicker.value = null
                }
                is RoomListEvent.ExecuteMerge -> {
                    coroutineScope.launch {
                        val sourceRoom = client.getRoom(event.sourceRoomId)
                        val displayName = sourceRoom?.use { it.roomInfoFlow.value.name } ?: ""
                        val existingMerge = client.contactMergeService.getMergedContactForRoom(event.sourceRoomId)
                        if (existingMerge != null) {
                            client.contactMergeService.addRoomToMerge(existingMerge.id, event.targetRoomId)
                        } else {
                            client.contactMergeService.mergeRooms(
                                displayName = displayName,
                                roomIds = listOf(event.sourceRoomId, event.targetRoomId),
                                activeRoomId = event.sourceRoomId,
                            )
                        }
                        mergePicker.value = null
                    }
                }
                is RoomListEvent.UnmergeFromList -> {
                    coroutineScope.launch {
                        val existingMerge = client.contactMergeService.getMergedContactForRoom(event.roomId)
                        if (existingMerge != null) {
                            client.contactMergeService.unmergeContact(existingMerge.id)
                        }
                        contextMenu.value = RoomListState.ContextMenu.Hidden
                    }
                }
                is RoomListEvent.SelectLabelFilter -> {
                    activeLabelFilter.value = event.label
                }
                is RoomListEvent.LeaveRoom -> {
                    leaveRoomState.eventSink(LeaveRoomEvent.LeaveRoom(event.roomId, needsConfirmation = event.needsConfirmation))
                }
                is RoomListEvent.SetRoomIsFavorite -> coroutineScope.setRoomIsFavorite(event.roomId, event.isFavorite)
                is RoomListEvent.MarkAsRead -> coroutineScope.markAsRead(event.roomId)
                is RoomListEvent.MarkAsUnread -> coroutineScope.markAsUnread(event.roomId)
                is RoomListEvent.AcceptInvite -> {
                    acceptDeclineInviteState.eventSink(
                        AcceptInvite(event.roomSummary.toInviteData())
                    )
                }
                is RoomListEvent.DeclineInvite -> {
                    acceptDeclineInviteState.eventSink(
                        DeclineInvite(event.roomSummary.toInviteData(), blockUser = event.blockUser, shouldConfirm = false)
                    )
                }
                is RoomListEvent.ShowDeclineInviteMenu -> declineInviteMenu.value = RoomListState.DeclineInviteMenu.Shown(event.roomSummary)
                RoomListEvent.HideDeclineInviteMenu -> declineInviteMenu.value = RoomListState.DeclineInviteMenu.Hidden
            }
        }

        LaunchedEffect(filtersState.filterSelectionStates, spaceFiltersState.selectedFilter()) {
            val selectedFilters = filtersState.selectedFilters().map { filter -> filter.into() }
            val selectedSpaceFilter = spaceFiltersState.selectedFilter().into()
            val allFilters = RoomListFilter.All(selectedFilters + listOfNotNull(selectedSpaceFilter))
            roomListDataSource.updateFilter(allFilters)
        }

        val canReportRoom by produceState(false) { value = client.canReportRoom() }
        val showUnreadCount by produceState(false) {
            value = featureFlagService.isFeatureEnabled(FeatureFlags.UnreadIndicatorCount)
        }

        val contentState = roomListContentState(
            securityBannerDismissed = securityBannerDismissed,
            showNewNotificationSoundBanner = showNewNotificationSoundBanner,
            showUnreadCount = showUnreadCount,
            isSpaceFilterActive = spaceFiltersState.selectedFilter() != null,
            activeLabelFilter = activeLabelFilter.value,
        )

        val manageLabelsState = activeLabelsTarget.value?.let { target ->
            RoomListState.ManageLabels(
                roomId = target.roomId,
                roomName = target.roomName,
                labels = allLabels.toImmutableList(),
                mergedRoomCount = target.mergedRoomCount,
            )
        }

        return RoomListState(
            contextMenu = contextMenu.value,
            declineInviteMenu = declineInviteMenu.value,
            leaveRoomState = leaveRoomState,
            filtersState = filtersState,
            searchState = searchState,
            globalSearchState = globalSearchState,
            spaceFiltersState = spaceFiltersState,
            contentState = contentState,
            acceptDeclineInviteState = acceptDeclineInviteState,
            hideInvitesAvatars = hideInvitesAvatar,
            canReportRoom = canReportRoom,
            organizeInSpaces = organizeInSpaces.value,
            manageLabels = manageLabelsState,
            isCreatingLabel = isCreatingLabel.value,
            labelToEdit = labelToEdit.value,
            mergePicker = mergePicker.value,
            activeLabelFilter = activeLabelFilter.value,
            allLabels = allLabels,
            eventSink = ::handleEvent,
        )
    }

    @Composable
    private fun rememberSecurityBannerState(
        securityBannerDismissed: Boolean,
    ): State<SecurityBannerState> {
        val currentSecurityBannerDismissed by rememberUpdatedState(securityBannerDismissed)
        val recoveryState by encryptionService.recoveryStateStateFlow.collectAsState()
        return remember {
            derivedStateOf {
                calculateBannerState(
                    securityBannerDismissed = currentSecurityBannerDismissed,
                    recoveryState = recoveryState,
                )
            }
        }
    }

    private fun calculateBannerState(
        securityBannerDismissed: Boolean,
        recoveryState: RecoveryState,
    ): SecurityBannerState {
        if (securityBannerDismissed) {
            return SecurityBannerState.None
        }

        when (recoveryState) {
            RecoveryState.DISABLED -> return SecurityBannerState.SetUpRecovery
            RecoveryState.INCOMPLETE -> return SecurityBannerState.RecoveryKeyConfirmation
            RecoveryState.UNKNOWN,
            RecoveryState.WAITING_FOR_SYNC,
            RecoveryState.ENABLED -> Unit
        }

        return SecurityBannerState.None
    }

    @Composable
    private fun roomListContentState(
        securityBannerDismissed: Boolean,
        showNewNotificationSoundBanner: Boolean,
        showUnreadCount: Boolean,
        isSpaceFilterActive: Boolean,
        activeLabelFilter: RoomLabel? = null,
    ): RoomListContentState {
        val roomSummaries by produceState(initialValue = AsyncData.Loading()) {
            roomListDataSource.roomSummariesFlow.collect { value = AsyncData.Success(it) }
        }
        val mergedContacts by client.contactMergeService.mergedContacts.collectAsState(initial = emptyList())
        val hiddenRoomIds by client.labelService.hiddenFromInboxRoomIds.collectAsState(initial = emptySet())
        val allLabels by client.labelService.labels.collectAsState(initial = emptyList())
        val loadingState by roomListDataSource.loadingState.collectAsState()
        val showEmpty by remember {
            derivedStateOf {
                (loadingState as? RoomList.LoadingState.Loaded)?.numberOfRooms == 0
            }
        }
        val showSkeleton by remember {
            derivedStateOf {
                loadingState == RoomList.LoadingState.NotLoaded || roomSummaries is AsyncData.Loading
            }
        }
        val seenRoomInvites by remember { seenInvitesStore.seenRoomIds() }.collectAsState(emptySet())
        val securityBannerState by rememberSecurityBannerState(securityBannerDismissed)
        val roomIdToMerge = remember(mergedContacts) {
            val map = HashMap<RoomId, MergedContact>()
            for (mc in mergedContacts) {
                for (rId in mc.roomIds) {
                    map[rId] = mc
                }
            }
            map
        }
        val processedSummaries = remember(roomSummaries, roomIdToMerge, hiddenRoomIds, allLabels, isSpaceFilterActive) {
            val raw = roomSummaries.dataOrNull().orEmpty()
            val filteredRaw = if (isSpaceFilterActive || hiddenRoomIds.isEmpty()) {
                raw
            } else {
                raw.filter { summary -> summary.roomId !in hiddenRoomIds }
            }
            if (roomIdToMerge.isEmpty()) {
                filteredRaw.map { summary ->
                    val emojis = allLabels.filter { summary.roomId in it.roomIds }.mapNotNull { it.emoji }.toImmutableList()
                    if (emojis.isNotEmpty()) summary.copy(labelEmojis = emojis) else summary
                }.toImmutableList()
            } else {
                val handledMergeIds = HashSet<String>()
                val result = ArrayList<RoomListRoomSummary>(filteredRaw.size)

                for (summary in filteredRaw) {
                    val mc = roomIdToMerge[summary.roomId]
                    if (mc == null) {
                        val emojis = allLabels.filter { summary.roomId in it.roomIds }.mapNotNull { it.emoji }.toImmutableList()
                        result.add(if (emojis.isNotEmpty()) summary.copy(labelEmojis = emojis) else summary)
                    } else {
                        if (handledMergeIds.add(mc.id)) {
                            val siblings = filteredRaw.filter { it.roomId in mc.roomIds }
                            val primary = siblings.firstOrNull { it.roomId == mc.activeRoomId } ?: summary
                            val totalUnreadMessages = siblings.sumOf { it.numberOfUnreadMessages }
                            val totalUnreadNotifications = siblings.sumOf { it.numberOfUnreadNotifications }
                            val totalUnreadMentions = siblings.sumOf { it.numberOfUnreadMentions }
                            val isAnyMarkedUnread = siblings.any { it.isMarkedUnread }
                            val allBadges = siblings.flatMap { it.networkBadges }.distinct().toImmutableList()
                            val emojis = allLabels.filter { label -> mc.roomIds.any { it in label.roomIds } }.mapNotNull { it.emoji }.distinct().toImmutableList()

                            result.add(
                                primary.copy(
                                    name = mc.displayName.ifBlank { primary.name },
                                    numberOfUnreadMessages = totalUnreadMessages,
                                    numberOfUnreadNotifications = totalUnreadNotifications,
                                    numberOfUnreadMentions = totalUnreadMentions,
                                    isMarkedUnread = isAnyMarkedUnread,
                                    networkBadges = if (allBadges.isNotEmpty()) allBadges else primary.networkBadges,
                                    labelEmojis = emojis,
                                )
                            )
                        }
                    }
                }
                result.toImmutableList()
            }
        }
        // Filtrar por label activa si hay una seleccionada (paridad con FluffyBeep Flutter)
        val labelFilteredSummaries = if (activeLabelFilter != null) {
            processedSummaries.filter { s ->
                val mc = roomIdToMerge[s.roomId]
                if (mc != null) {
                    mc.roomIds.any { it in activeLabelFilter.roomIds }
                } else {
                    s.roomId in activeLabelFilter.roomIds
                }
            }.toImmutableList()
        } else {
            processedSummaries
        }
        return when {
            showEmpty -> RoomListContentState.Empty(
                securityBannerState = securityBannerState,
            )
            showSkeleton -> RoomListContentState.Skeleton(count = 16)
            else -> {
                coldStartWatcher.onRoomListVisible()

                RoomListContentState.Rooms(
                    securityBannerState = securityBannerState,
                    showNewNotificationSoundBanner = showNewNotificationSoundBanner,
                    showUnreadCount = showUnreadCount,
                    fullScreenIntentPermissionsState = fullScreenIntentPermissionsPresenter.present(),
                    batteryOptimizationState = batteryOptimizationPresenter.present(),
                    summaries = labelFilteredSummaries,
                    seenRoomInvites = seenRoomInvites.toImmutableSet(),
                )
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun CoroutineScope.showContextMenu(event: RoomListEvent.ShowContextMenu, contextMenuState: MutableState<RoomListState.ContextMenu>) = launch {
        val isMerged = client.contactMergeService.getMergedContactForRoom(event.roomSummary.roomId) != null
        val initialState = RoomListState.ContextMenu.Shown(
            roomId = event.roomSummary.roomId,
            roomName = event.roomSummary.name,
            isDm = event.roomSummary.isDm,
            isFavorite = event.roomSummary.isFavorite,
            hasNewContent = event.roomSummary.hasNewContent,
            isMerged = isMerged,
        )
        contextMenuState.value = initialState

        client.getRoom(event.roomSummary.roomId)?.use { room ->

            val isShowingContextMenuFlow = snapshotFlow { contextMenuState.value is RoomListState.ContextMenu.Shown }
                .distinctUntilChanged()

            val isFavoriteFlow = room.roomInfoFlow
                .map { it.isFavorite }
                .distinctUntilChanged()

            isFavoriteFlow
                .onEach { isFavorite ->
                    contextMenuState.value = initialState.copy(isFavorite = isFavorite)
                }
                .flatMapLatest { isShowingContextMenuFlow }
                .takeWhile { isShowingContextMenu -> isShowingContextMenu }
                .collect()
        }
    }

    private fun CoroutineScope.setRoomIsFavorite(roomId: RoomId, isFavorite: Boolean) = launch {
        client.getRoom(roomId)?.use { room ->
            room.setIsFavorite(isFavorite)
                .onSuccess {
                    analyticsService.captureInteraction(name = Interaction.Name.MobileRoomListRoomContextMenuFavouriteToggle)
                }
        }
    }

    private fun CoroutineScope.markAsRead(roomId: RoomId) = launch {
        markRoomAsRead(roomId)
            .onSuccess {
                analyticsService.captureInteraction(name = Interaction.Name.MobileRoomListRoomContextMenuUnreadToggle)
            }
    }

    private fun CoroutineScope.markAsUnread(roomId: RoomId) = launch {
        client.getRoom(roomId)?.use { room ->
            room.setUnreadFlag(isUnread = true)
                .onSuccess {
                    analyticsService.captureInteraction(name = Interaction.Name.MobileRoomListRoomContextMenuUnreadToggle)
                }
        }
    }
}

private data class ActiveLabelsTarget(
    val roomId: RoomId,
    val roomName: String?,
    val mergedRoomCount: Int,
)
