/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalHazeMaterialsApi::class)

package io.element.android.features.home.impl

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.home.impl.components.HomeTopBar
import io.element.android.features.home.impl.components.RoomListContentView
import io.element.android.features.home.impl.components.RoomListMenuAction
import io.element.android.features.home.impl.labels.LabelEditorBottomSheet
import io.element.android.features.home.impl.labels.ManageLabelsBottomSheet
import io.element.android.features.home.impl.model.RoomListRoomSummary
import io.element.android.features.home.impl.roomlist.RoomListContextMenu
import io.element.android.features.home.impl.roomlist.RoomListDeclineInviteMenu
import io.element.android.features.home.impl.roomlist.RoomListEvent
import io.element.android.features.home.impl.roomlist.RoomListState
import io.element.android.features.home.impl.search.GlobalSearchEvent
import io.element.android.features.home.impl.search.GlobalSearchView
import io.element.android.features.home.impl.search.RoomListSearchView
import io.element.android.features.home.impl.spacefilters.SpaceFiltersEvent
import io.element.android.features.home.impl.spacefilters.SpaceFiltersState
import io.element.android.features.home.impl.spacefilters.SpaceFiltersView
import io.element.android.features.home.impl.spacefilters.availableFilters
import io.element.android.features.home.impl.spacefilters.quickBarFilters
import io.element.android.features.home.impl.spacefilters.sendEvent
import io.element.android.features.home.impl.spaces.HomeSpacesView
import io.element.android.libraries.androidutils.throttler.FirstThrottler
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.matrix.api.spaces.SpaceServiceFilter
import io.element.android.libraries.matrix.ui.model.getAvatarData
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.FloatingActionButton
import io.element.android.libraries.designsystem.theme.components.HorizontalFloatingToolbar
import io.element.android.libraries.designsystem.theme.components.HorizontalFloatingToolbarItem
import io.element.android.libraries.designsystem.theme.components.HorizontalFloatingToolbarSeparator
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.utils.lazyColumnContentPadding
import io.element.android.libraries.designsystem.utils.scaffoldScrollableContentInsets
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarHost
import io.element.android.libraries.designsystem.utils.snackbar.rememberSnackbarHostState
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.coroutines.launch

@Composable
fun HomeView(
    homeState: HomeState,
    onRoomClick: (RoomId, EventId?) -> Unit,
    onSettingsClick: () -> Unit,
    onSetUpRecoveryClick: () -> Unit,
    onConfirmRecoveryKeyClick: () -> Unit,
    onStartChatClick: () -> Unit,
    onCreateSpaceClick: () -> Unit,
    onRoomSettingsClick: (roomId: RoomId) -> Unit,
    onMenuActionClick: (RoomListMenuAction) -> Unit,
    onReportRoomClick: (roomId: RoomId) -> Unit,
    onDeclineInviteAndBlockUser: (roomSummary: RoomListRoomSummary) -> Unit,
    acceptDeclineInviteView: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leaveRoomView: @Composable () -> Unit,
) {
    val state: RoomListState = homeState.roomListState
    val coroutineScope = rememberCoroutineScope()
    val firstThrottler = remember { FirstThrottler(300, coroutineScope) }
    Box(modifier) {
        if (state.contextMenu is RoomListState.ContextMenu.Shown) {
            RoomListContextMenu(
                contextMenu = state.contextMenu,
                canReportRoom = state.canReportRoom,
                eventSink = state.eventSink,
                onRoomSettingsClick = onRoomSettingsClick,
                onReportRoomClick = onReportRoomClick,
                onOrganizeInSpacesClick = { roomId ->
                    state.eventSink(RoomListEvent.ShowOrganizeInSpaces(roomId))
                },
            )
        }
        val organizeInSpaces = state.organizeInSpaces
        if (organizeInSpaces != null) {
            io.element.android.features.home.impl.spaces.OrganizeInSpacesBottomSheet(
                roomName = organizeInSpaces.roomName,
                spaceFilters = state.spaceFiltersState.availableFilters(),
                memberSpaceIds = organizeInSpaces.memberSpaceIds,
                mergedRoomCount = organizeInSpaces.mergedRoomCount,
                onToggleSpace = { spaceId, isMember ->
                    state.eventSink(RoomListEvent.ToggleSpaceMembership(spaceId, isMember))
                },
                onDismissRequest = {
                    state.eventSink(RoomListEvent.HideOrganizeInSpaces)
                },
            )
        }
        val manageLabels = state.manageLabels
        if (manageLabels != null) {
            ManageLabelsBottomSheet(
                roomName = manageLabels.roomName,
                roomId = manageLabels.roomId,
                labels = manageLabels.labels,
                mergedRoomCount = manageLabels.mergedRoomCount,
                onToggleLabel = { labelId, isAssigned ->
                    state.eventSink(RoomListEvent.ToggleLabelMembership(labelId, isAssigned))
                },
                onCreateLabelClick = {
                    state.eventSink(RoomListEvent.ShowCreateLabel)
                },
                onEditLabelClick = { label ->
                    state.eventSink(RoomListEvent.ShowEditLabel(label))
                },
                onDismissRequest = {
                    state.eventSink(RoomListEvent.HideManageLabels)
                },
            )
        }
        if (state.isCreatingLabel || state.labelToEdit != null) {
            LabelEditorBottomSheet(
                labelToEdit = state.labelToEdit,
                onSave = { title, emoji, isShownInInbox ->
                    state.eventSink(RoomListEvent.SaveLabel(title, emoji, isShownInInbox))
                },
                onDelete = state.labelToEdit?.let { label ->
                    { state.eventSink(RoomListEvent.DeleteLabel(label.id)) }
                },
                onDismissRequest = {
                    state.eventSink(RoomListEvent.HideLabelEditor)
                },
            )
        }
        if (state.declineInviteMenu is RoomListState.DeclineInviteMenu.Shown) {
            RoomListDeclineInviteMenu(
                menu = state.declineInviteMenu,
                canReportRoom = state.canReportRoom,
                eventSink = state.eventSink,
                onDeclineAndBlockClick = onDeclineInviteAndBlockUser,
            )
        }

        leaveRoomView()

        HomeScaffold(
            state = homeState,
            onSetUpRecoveryClick = onSetUpRecoveryClick,
            onConfirmRecoveryKeyClick = onConfirmRecoveryKeyClick,
            onRoomClick = { roomId -> if (firstThrottler.canHandle()) onRoomClick(roomId, null) },
            onOpenSettings = { if (firstThrottler.canHandle()) onSettingsClick() },
            onStartChatClick = { if (firstThrottler.canHandle()) onStartChatClick() },
            onCreateSpaceClick = { if (firstThrottler.canHandle()) onCreateSpaceClick() },
            onMenuActionClick = onMenuActionClick,
        )

        if (state.globalSearchState.isEnabled) {
            GlobalSearchView(
                state = state.globalSearchState,
                onSelectSearchResult = { roomId, eventId -> if (firstThrottler.canHandle()) onRoomClick(roomId, eventId) },
                modifier = Modifier
                    .fillMaxSize()
                    .background(ElementTheme.colors.bgCanvasDefault),
            )
        } else {
            // This overlaid view will only be visible when state.displaySearchResults is true
            RoomListSearchView(
                state = state.searchState,
                eventSink = state.eventSink,
                hideInvitesAvatars = state.hideInvitesAvatars,
                onRoomClick = { roomId -> if (firstThrottler.canHandle()) onRoomClick(roomId, null) },
                modifier = Modifier
                    .fillMaxSize()
                    .background(ElementTheme.colors.bgCanvasDefault)
            )
        }

        acceptDeclineInviteView()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScaffold(
    state: HomeState,
    onSetUpRecoveryClick: () -> Unit,
    onConfirmRecoveryKeyClick: () -> Unit,
    onRoomClick: (RoomId) -> Unit,
    onOpenSettings: () -> Unit,
    onStartChatClick: () -> Unit,
    onCreateSpaceClick: () -> Unit,
    onMenuActionClick: (RoomListMenuAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun onRoomClick(room: RoomListRoomSummary) {
        onRoomClick(room.roomId)
    }

    val appBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(appBarState)
    val snackbarHostState = rememberSnackbarHostState(snackbarMessage = state.snackbarMessage)
    val roomListState: RoomListState = state.roomListState

    BackHandler(enabled = state.isBackHandlerEnabled) {
        if (state.currentHomeNavigationBarItem != HomeNavigationBarItem.Chats) {
            state.eventSink(HomeEvent.SelectHomeNavigationBarItem(HomeNavigationBarItem.Chats))
        } else {
            val spaceFiltersState = state.roomListState.spaceFiltersState
            if (spaceFiltersState is SpaceFiltersState.Selected) {
                spaceFiltersState.eventSink(SpaceFiltersEvent.Selected.ClearSelection)
            }
        }
    }

    val hazeState = rememberHazeState()
    val roomsLazyListState = rememberLazyListState()
    val spacesLazyListState = rememberLazyListState()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            HomeTopBar(
                selectedNavigationItem = state.currentHomeNavigationBarItem,
                currentUserAndNeighbors = state.currentUserAndNeighbors,
                showAvatarIndicator = state.showAvatarIndicator,
                areSearchResultsDisplayed = if (roomListState.globalSearchState.isEnabled) {
                    roomListState.globalSearchState.isSearchActive
                } else {
                    roomListState.searchState.isSearchActive
                },
                onToggleSearch = {
                    if (roomListState.globalSearchState.isEnabled) {
                        roomListState.globalSearchState.eventSink(GlobalSearchEvent.ToggleSearchVisibility)
                    } else {
                        roomListState.eventSink(RoomListEvent.ToggleSearchResults)
                    }
                },
                onMenuActionClick = onMenuActionClick,
                onOpenSettings = onOpenSettings,
                onAccountSwitch = {
                    state.eventSink(HomeEvent.SwitchToAccount(it))
                },
                scrollBehavior = scrollBehavior,
                displayFilters = state.displayRoomListFilters,
                filtersState = roomListState.filtersState,
                spaceFiltersState = roomListState.spaceFiltersState,
                canReportBug = state.canReportBug,
                modifier = Modifier.hazeEffect(
                    state = hazeState,
                    style = HazeMaterials.thick(),
                )
            )
        },
        floatingActionButton = {
            val coroutineScope = rememberCoroutineScope()
            HomeBottomBar(
                // The Scaffold uses top-only insets so the scrollable content can go edge-to-edge behind the
                // navigation bar, so the floating toolbar has to apply the bottom inset itself to avoid overlapping it.
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                currentHomeNavigationBarItem = state.currentHomeNavigationBarItem,
                spaceFiltersState = roomListState.spaceFiltersState,
                onSelectAllChats = {
                    if (state.currentHomeNavigationBarItem != HomeNavigationBarItem.Chats) {
                        state.eventSink(HomeEvent.SelectHomeNavigationBarItem(HomeNavigationBarItem.Chats))
                    }
                    val spaceFiltersState = roomListState.spaceFiltersState
                    if (spaceFiltersState is SpaceFiltersState.Selected) {
                        spaceFiltersState.eventSink(SpaceFiltersEvent.Selected.ClearSelection)
                    } else if (state.currentHomeNavigationBarItem == HomeNavigationBarItem.Chats) {
                        coroutineScope.launch {
                            if (roomsLazyListState.firstVisibleItemIndex > 10) {
                                roomsLazyListState.scrollToItem(10)
                            }
                            scrollBehavior.state.heightOffset = 0f
                            roomsLazyListState.animateScrollToItem(0)
                        }
                    }
                },
                onSelectSpace = { space ->
                    if (state.currentHomeNavigationBarItem != HomeNavigationBarItem.Chats) {
                        state.eventSink(HomeEvent.SelectHomeNavigationBarItem(HomeNavigationBarItem.Chats))
                    }
                    val spaceFiltersState = roomListState.spaceFiltersState
                    val isAlreadySelected = spaceFiltersState is SpaceFiltersState.Selected &&
                        spaceFiltersState.selectedFilter.spaceRoom.roomId == space.spaceRoom.roomId

                    if (isAlreadySelected) {
                        coroutineScope.launch {
                            if (roomsLazyListState.firstVisibleItemIndex > 10) {
                                roomsLazyListState.scrollToItem(10)
                            }
                            scrollBehavior.state.heightOffset = 0f
                            roomsLazyListState.animateScrollToItem(0)
                        }
                    } else {
                        when (spaceFiltersState) {
                            is SpaceFiltersState.Unselected -> {
                                spaceFiltersState.eventSink(SpaceFiltersEvent.Unselected.SelectFilter(space))
                            }
                            is SpaceFiltersState.Selected -> {
                                spaceFiltersState.eventSink(SpaceFiltersEvent.Selected.SelectFilter(space))
                            }
                            is SpaceFiltersState.Selecting -> {
                                spaceFiltersState.eventSink(SpaceFiltersEvent.Selecting.SelectFilter(space))
                            }
                            SpaceFiltersState.Disabled -> Unit
                        }
                    }
                },
                onSelectSpacesTab = {
                    if (state.currentHomeNavigationBarItem == HomeNavigationBarItem.Spaces) {
                        coroutineScope.launch {
                            if (spacesLazyListState.firstVisibleItemIndex > 10) {
                                spacesLazyListState.scrollToItem(10)
                            }
                            scrollBehavior.state.heightOffset = 0f
                            spacesLazyListState.animateScrollToItem(0)
                        }
                    } else {
                        state.eventSink(HomeEvent.SelectHomeNavigationBarItem(HomeNavigationBarItem.Spaces))
                    }
                },
                floatingActionButton = {
                    when (state.currentHomeNavigationBarItem) {
                        HomeNavigationBarItem.Chats -> {
                            HomeFloatingActionButton(onStartChatClick, CommonStrings.action_create_room)
                        }
                        HomeNavigationBarItem.Spaces -> {
                            HomeFloatingActionButton(onCreateSpaceClick, CommonStrings.action_create_space)
                        }
                    }
                },
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        contentWindowInsets = scaffoldScrollableContentInsets,
        content = { padding ->
            val outerPadding = PaddingValues(
                start = padding.calculateStartPadding(LocalLayoutDirection.current),
                end = padding.calculateEndPadding(LocalLayoutDirection.current),
                // Remove these two lines once https://issuetracker.google.com/issues/436432313 has been fixed
                bottom = padding.calculateBottomPadding(),
                top = padding.calculateTopPadding()
            )
            val contentPadding = PaddingValues(
                bottom = 96.dp,
            )
            when (state.currentHomeNavigationBarItem) {
                HomeNavigationBarItem.Chats -> {
                    RoomListContentView(
                        contentState = roomListState.contentState,
                        filtersState = roomListState.filtersState,
                        spaceFiltersState = roomListState.spaceFiltersState,
                        lazyListState = roomsLazyListState,
                        hideInvitesAvatars = roomListState.hideInvitesAvatars,
                        eventSink = roomListState.eventSink,
                        onSetUpRecoveryClick = onSetUpRecoveryClick,
                        onConfirmRecoveryKeyClick = onConfirmRecoveryKeyClick,
                        onRoomClick = ::onRoomClick,
                        onCreateRoomClick = onStartChatClick,
                        contentPadding = lazyColumnContentPadding + contentPadding,
                        modifier = Modifier
                            .padding(outerPadding)
                            .consumeWindowInsets(outerPadding)
                            .hazeSource(state = hazeState)
                    )
                    SpaceFiltersView(roomListState.spaceFiltersState)
                }
                HomeNavigationBarItem.Spaces -> {
                    HomeSpacesView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(outerPadding)
                            .consumeWindowInsets(outerPadding)
                            .hazeSource(state = hazeState),
                        contentPadding = lazyColumnContentPadding + contentPadding,
                        state = state.homeSpacesState,
                        lazyListState = spacesLazyListState,
                        onSpaceClick = { spaceId ->
                            onRoomClick(spaceId)
                        },
                        onCreateSpaceClick = onCreateSpaceClick,
                        // TODO use actual callbacks for this
                        onExploreClick = {},
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    )
}

@Composable
private fun HomeFloatingActionButton(
    onClick: () -> Unit,
    contentDescription: Int,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = CompoundIcons.Plus(),
            contentDescription = stringResource(id = contentDescription),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeBottomBar(
    currentHomeNavigationBarItem: HomeNavigationBarItem,
    spaceFiltersState: SpaceFiltersState,
    onSelectAllChats: () -> Unit,
    onSelectSpace: (SpaceServiceFilter) -> Unit,
    onSelectSpacesTab: () -> Unit,
    modifier: Modifier = Modifier,
    floatingActionButton: (@Composable () -> Unit)?,
) {
    val availableFilters = spaceFiltersState.quickBarFilters()
    val isAllChatsSelected = currentHomeNavigationBarItem == HomeNavigationBarItem.Chats &&
        spaceFiltersState !is SpaceFiltersState.Selected

    HorizontalFloatingToolbar(
        floatingActionButton = floatingActionButton,
        modifier = modifier.zIndex(1f),
    ) {
        // 1. All Chats tab
        HorizontalFloatingToolbarItem(
            icon = if (isAllChatsSelected) CompoundIcons.ChatSolid() else CompoundIcons.Chat(),
            tooltipLabel = stringResource(R.string.screen_home_tab_chats),
            isSelected = isAllChatsSelected,
            onClick = onSelectAllChats,
        )

        // 2. Spaces as dynamic icons (like Telegram folders)
        for (space in availableFilters) {
            HorizontalFloatingToolbarSeparator()
            val isSpaceSelected = currentHomeNavigationBarItem == HomeNavigationBarItem.Chats &&
                spaceFiltersState is SpaceFiltersState.Selected &&
                spaceFiltersState.selectedFilter.spaceRoom.roomId == space.spaceRoom.roomId
            val spaceRoom = space.spaceRoom

            var showMenu by remember { mutableStateOf(false) }
            Box {
                HorizontalFloatingToolbarItem(
                    iconContent = {
                        Avatar(
                            avatarData = spaceRoom.getAvatarData(AvatarSize.TimelineThreadLatestEventSender),
                            avatarType = AvatarType.Space(),
                        )
                    },
                    tooltipLabel = spaceRoom.displayName,
                    isSelected = isSpaceSelected,
                    onClick = { onSelectSpace(space) },
                    onLongClick = { showMenu = true },
                )
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Ocultar de la barra rápida") },
                        leadingIcon = {
                            Icon(
                                imageVector = CompoundIcons.Pin(),
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            showMenu = false
                            spaceFiltersState.sendEvent(SpaceFiltersEvent.HideFromQuickBar(space.spaceRoom.roomId.value))
                        }
                    )
                }
            }
        }

        // 3. Spaces explore/management tab
        HorizontalFloatingToolbarSeparator()
        val isSpacesTabSelected = currentHomeNavigationBarItem == HomeNavigationBarItem.Spaces
        HorizontalFloatingToolbarItem(
            icon = if (isSpacesTabSelected) CompoundIcons.SpaceSolid() else CompoundIcons.Space(),
            tooltipLabel = stringResource(R.string.screen_home_tab_spaces),
            isSelected = isSpacesTabSelected,
            onClick = onSelectSpacesTab,
        )
    }
}

internal fun RoomListRoomSummary.contentType() = displayType.ordinal

@PreviewsDayNight
@Composable
internal fun HomeViewPreview(@PreviewParameter(HomeStatePreviewParam::class) state: HomeState) = ElementPreview {
    HomeView(
        homeState = state,
        onRoomClick = { _, _ -> },
        onSettingsClick = {},
        onSetUpRecoveryClick = {},
        onConfirmRecoveryKeyClick = {},
        onStartChatClick = {},
        onCreateSpaceClick = {},
        onRoomSettingsClick = {},
        onReportRoomClick = {},
        onMenuActionClick = {},
        onDeclineInviteAndBlockUser = {},
        acceptDeclineInviteView = {},
        leaveRoomView = {}
    )
}

@Preview
@Composable
internal fun HomeViewA11yPreview() = ElementPreview {
    HomeView(
        homeState = aHomeState(),
        onRoomClick = { _, _ -> },
        onSettingsClick = {},
        onSetUpRecoveryClick = {},
        onConfirmRecoveryKeyClick = {},
        onStartChatClick = {},
        onCreateSpaceClick = {},
        onRoomSettingsClick = {},
        onReportRoomClick = {},
        onMenuActionClick = {},
        onDeclineInviteAndBlockUser = {},
        acceptDeclineInviteView = {},
        leaveRoomView = {}
    )
}
