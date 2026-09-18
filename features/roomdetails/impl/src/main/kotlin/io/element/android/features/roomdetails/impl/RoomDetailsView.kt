/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.roomdetails.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import im.vector.app.features.analytics.plan.Interaction
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.roomcall.api.hasPermissionToJoin
import io.element.android.features.userprofile.api.UserProfileState
import io.element.android.features.userprofile.api.UserProfileVerificationState
import io.element.android.features.userprofile.shared.blockuser.BlockUserDialogs
import io.element.android.features.userprofile.shared.blockuser.BlockUserSection
import io.element.android.libraries.androidutils.system.copyToClipboard
import io.element.android.libraries.architecture.coverage.ExcludeFromCoverage
import io.element.android.libraries.designsystem.atomic.atoms.MatrixBadgeAtom
import io.element.android.libraries.designsystem.atomic.molecules.MatrixBadgeRowMolecule
import io.element.android.libraries.designsystem.components.ClickableLinkText
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.components.button.MainActionButton
import io.element.android.libraries.designsystem.components.list.ListItemContent
import io.element.android.libraries.designsystem.components.preferences.PreferenceCategory
import io.element.android.libraries.designsystem.components.preferences.PreferenceSwitch
import io.element.android.libraries.designsystem.modifiers.niceClickable
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.ElementPreviewDark
import io.element.android.libraries.designsystem.preview.ElementPreviewLight
import io.element.android.libraries.designsystem.preview.PreviewWithExtraLargeHeight
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.DropdownMenu
import io.element.android.libraries.designsystem.theme.components.DropdownMenuItem
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.IconSource
import io.element.android.libraries.designsystem.theme.components.ListItem
import io.element.android.libraries.designsystem.theme.components.ListItemStyle
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TopAppBar
import io.element.android.libraries.designsystem.utils.lazyColumnContentPadding
import io.element.android.libraries.designsystem.utils.scaffoldScrollableContentInsets
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarHost
import io.element.android.libraries.designsystem.utils.snackbar.rememberSnackbarHostState
import io.element.android.libraries.matrix.api.core.RoomAlias
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.notification.CallIntent
import io.element.android.libraries.matrix.api.room.RoomMember
import io.element.android.libraries.matrix.api.room.RoomNotificationMode
import io.element.android.libraries.matrix.api.room.getBestName
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.ui.model.getAvatarData
import io.element.android.libraries.matrix.ui.model.toText
import io.element.android.libraries.matrix.api.contactmerge.detectNetwork
import io.element.android.libraries.matrix.ui.model.SelectRoomInfo
import io.element.android.libraries.testtags.TestTags
import io.element.android.libraries.testtags.testTag
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.services.analytics.compose.LocalAnalyticsService
import io.element.android.services.analyticsproviders.api.trackers.captureInteraction
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Ref: https://www.figma.com/design/pDlJZGBsri47FNTXMnEdXB/Compound-Android-Templates?node-id=21-120385
 */
@Composable
fun RoomDetailsView(
    state: RoomDetailsState,
    goBack: () -> Unit,
    onActionClick: (RoomDetailsAction) -> Unit,
    onShareRoom: () -> Unit,
    openRoomMemberList: () -> Unit,
    openRoomNotificationSettings: () -> Unit,
    invitePeople: () -> Unit,
    openAvatarPreview: (name: String, url: String) -> Unit,
    openPollHistory: () -> Unit,
    openMediaGallery: () -> Unit,
    openAdminSettings: () -> Unit,
    onJoinCallClick: (CallIntent) -> Unit,
    onPinnedMessagesClick: () -> Unit,
    onKnockRequestsClick: () -> Unit,
    onSecurityAndPrivacyClick: () -> Unit,
    onProfileClick: (UserId) -> Unit,
    onReportRoomClick: () -> Unit,
    modifier: Modifier = Modifier,
    leaveRoomView: @Composable () -> Unit,
) {
    val snackbarHostState = rememberSnackbarHostState(snackbarMessage = state.snackbarMessage)
    Scaffold(
        modifier = modifier,
        topBar = {
            RoomDetailsTopBar(
                goBack = goBack,
                showEdit = state.canEdit,
                onActionClick = onActionClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = scaffoldScrollableContentInsets,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .consumeWindowInsets(padding)
                .padding(lazyColumnContentPadding)
        ) {
            leaveRoomView()

            when (state.roomType) {
                RoomDetailsType.Room -> {
                    RoomHeaderSection(
                        avatarUrl = state.roomAvatarUrl,
                        roomId = state.roomId,
                        roomName = state.roomName,
                        roomAlias = state.roomAlias,
                        heroes = state.heroes,
                        isTombstoned = state.isTombstoned,
                        openAvatarPreview = { avatarUrl ->
                            openAvatarPreview(state.roomName, avatarUrl)
                        },
                        onSubtitleClick = { subtitle ->
                            state.eventSink(RoomDetailsEvent.CopyToClipboard(subtitle))
                        }
                    )
                }
                is RoomDetailsType.Dm -> {
                    DmHeaderSection(
                        otherMember = state.roomType.otherMember,
                        roomName = state.roomName,
                        isTombstoned = state.isTombstoned,
                        openAvatarPreview = { name, avatarUrl ->
                            openAvatarPreview(name, avatarUrl)
                        },
                        onSubtitleClick = { subtitle ->
                            state.eventSink(RoomDetailsEvent.CopyToClipboard(subtitle))
                        }
                    )
                }
            }
            BadgeList(
                roomBadge = state.roomBadges,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(32.dp))
            MainActionsSection(
                state = state,
                onShareRoom = onShareRoom,
                onInvitePeople = invitePeople,
                onCall = onJoinCallClick,
            )
            Spacer(Modifier.height(12.dp))

            if (state.roomTopic !is RoomTopicState.Hidden) {
                TopicSection(
                    roomTopic = state.roomTopic,
                    onActionClick = onActionClick,
                )
            }

            PreferenceCategory {
                if (state.hasNewContent) {
                    ListItem(
                        content = {
                            Text(
                                text = stringResource(id = R.string.screen_roomlist_mark_as_read),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                        onClick = {
                            state.eventSink(RoomDetailsEvent.MarkAsRead)
                        },
                        leadingContent = ListItemContent.Icon(
                            iconSource = IconSource.Vector(CompoundIcons.MarkAsRead())
                        ),
                        trailingContent = ListItemContent.Custom {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(ElementTheme.colors.iconAccentPrimary)
                            )
                        },
                    )
                } else {
                    ListItem(
                        content = {
                            Text(
                                text = stringResource(id = R.string.screen_roomlist_mark_as_unread),
                            )
                        },
                        onClick = {
                            state.eventSink(RoomDetailsEvent.MarkAsUnread)
                        },
                        leadingContent = ListItemContent.Icon(
                            iconSource = IconSource.Vector(CompoundIcons.MarkAsUnread())
                        ),
                    )
                }
            }
            // Room content
            PreferenceCategory {
                MediaGalleryItem(
                    onClick = openMediaGallery
                )
                PinnedMessagesItem(
                    pinnedMessagesCount = state.pinnedMessagesCount,
                    onPinnedMessagesClick = onPinnedMessagesClick
                )
                PollsItem(
                    openPollHistory = openPollHistory
                )
            }
            when (state.roomType) {
                is RoomDetailsType.Room -> {
                    PreferenceCategory {
                        MembersItem(
                            memberCount = state.memberCount,
                            hasVerificationViolations = state.hasMemberVerificationViolations,
                            openRoomMemberList = openRoomMemberList,
                        )
                        if (state.canShowKnockRequests) {
                            KnockRequestsItem(
                                knockRequestsCount = state.knockRequestsCount,
                                onKnockRequestsClick = onKnockRequestsClick
                            )
                        }
                        if (state.displayRolesAndPermissionsSettings) {
                            ListItem(
                                content = { Text(stringResource(R.string.screen_room_details_roles_and_permissions)) },
                                leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Admin())),
                                onClick = openAdminSettings,
                            )
                        }
                    }
                }
                is RoomDetailsType.Dm -> {
                    if (state.canInvite) {
                        // Note: for rooms the invite action is a Main action
                        PreferenceCategory {
                            InviteItem(onClick = invitePeople)
                        }
                    }
                    state.dmOtherMemberDetailsState?.let { dmMemberDetails ->
                        PreferenceCategory {
                            ProfileItem(
                                verificationState = dmMemberDetails.verificationState,
                                onClick = { onProfileClick(dmMemberDetails.userId) }
                            )
                        }
                    }
                }
            }
            PreferenceCategory {
                if (state.roomNotificationSettings != null) {
                    NotificationItem(
                        isDefaultMode = state.roomNotificationSettings.isDefault,
                        openRoomNotificationSettings = openRoomNotificationSettings
                    )
                }
                FavoriteItem(
                    isFavorite = state.isFavorite,
                    onFavoriteChanges = {
                        state.eventSink(RoomDetailsEvent.SetFavorite(it))
                    }
                )
                if (state.canShowSecurityAndPrivacy && state.roomType is RoomDetailsType.Room) {
                    SecurityAndPrivacyItem(
                        onClick = onSecurityAndPrivacyClick
                    )
                }
            }
            if (state.isEncrypted) {
                PreferenceCategory(
                    title = stringResource(R.string.screen_room_details_security_title)
                ) {
                    ListItem(
                        leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Lock())),
                        content = {
                            Text(stringResource(id = R.string.screen_room_details_encryption_enabled_title))
                        },
                        supportingContent = {
                            Text(stringResource(id = R.string.screen_room_details_encryption_enabled_subtitle))
                        },
                    )
                }
            }
            ContactMergeSection(state = state)
            OtherActionsSection(
                dmOtherMemberDetailsState = state.dmOtherMemberDetailsState,
                canReportRoom = state.canReportRoom,
                onReportRoomClick = onReportRoomClick,
                onLeaveRoomClick = { state.eventSink(RoomDetailsEvent.LeaveRoom(needsConfirmation = true)) }
            )
            if (state.showDebugInfo) {
                DebugInfoSection(
                    roomId = state.roomId,
                    roomVersion = state.roomVersion,
                )
            }
        }
    }
}

@Composable
private fun KnockRequestsItem(knockRequestsCount: Int?, onKnockRequestsClick: () -> Unit) {
    ListItem(
        content = { Text(stringResource(R.string.screen_room_details_requests_to_join_title)) },
        leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.AskToJoin())),
        trailingContent = if (knockRequestsCount == null || knockRequestsCount == 0) {
            null
        } else {
            ListItemContent.Counter(knockRequestsCount)
        },
        onClick = onKnockRequestsClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomDetailsTopBar(
    goBack: () -> Unit,
    onActionClick: (RoomDetailsAction) -> Unit,
    showEdit: Boolean,
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { },
        navigationIcon = { BackButton(onClick = goBack) },
        actions = {
            if (showEdit) {
                IconButton(onClick = { showMenu = !showMenu }) {
                    Icon(CompoundIcons.OverflowVertical(), stringResource(id = CommonStrings.a11y_user_menu))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(id = CommonStrings.action_edit)) },
                        onClick = {
                            // Explicitly close the menu before handling the action, as otherwise it stays open during the
                            // transition and renders really badly.
                            showMenu = false
                            onActionClick(RoomDetailsAction.Edit)
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun MainActionsSection(
    state: RoomDetailsState,
    onShareRoom: () -> Unit,
    onInvitePeople: () -> Unit,
    onCall: (callIntent: CallIntent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        state.roomNotificationSettings?.let { roomNotificationSettings ->
            if (roomNotificationSettings.mode == RoomNotificationMode.MUTE) {
                MainActionButton(
                    title = stringResource(CommonStrings.common_unmute),
                    imageVector = CompoundIcons.NotificationsOff(),
                    onClick = {
                        state.eventSink(RoomDetailsEvent.UnmuteNotification)
                    },
                )
            } else {
                MainActionButton(
                    title = stringResource(CommonStrings.common_mute),
                    imageVector = CompoundIcons.Notifications(),
                    onClick = {
                        state.eventSink(RoomDetailsEvent.MuteNotification)
                    },
                )
            }
        }
        if (state.roomCallState.hasPermissionToJoin()) {
            // TODO Improve the view depending on all the cases here?
            if (state.roomType is RoomDetailsType.Dm) {
                // As per design, only show voice call in DM
                MainActionButton(
                    title = stringResource(CommonStrings.action_call),
                    imageVector = CompoundIcons.VoiceCall(),
                    onClick = { onCall(CallIntent.AUDIO) },
                )
            }

            MainActionButton(
                title = stringResource(CommonStrings.common_video),
                imageVector = CompoundIcons.VideoCall(),
                onClick = { onCall(CallIntent.VIDEO) },
            )
        }
        if (state.canInvite && state.roomType !is RoomDetailsType.Dm) {
            MainActionButton(
                title = stringResource(CommonStrings.action_invite),
                imageVector = CompoundIcons.UserAdd(),
                onClick = onInvitePeople,
            )
        }
        if (state.roomType is RoomDetailsType.Room) {
            // Share CTA should be hidden for DMs
            MainActionButton(
                title = stringResource(CommonStrings.action_share),
                imageVector = CompoundIcons.ShareAndroid(),
                onClick = onShareRoom
            )
        }
    }
}

@Composable
private fun RoomHeaderSection(
    avatarUrl: String?,
    roomId: RoomId,
    roomName: String,
    roomAlias: RoomAlias?,
    heroes: ImmutableList<MatrixUser>,
    isTombstoned: Boolean,
    openAvatarPreview: (url: String) -> Unit,
    onSubtitleClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Avatar(
            avatarData = AvatarData(roomId.value, roomName, avatarUrl, AvatarSize.RoomDetailsHeader),
            avatarType = AvatarType.Room(
                heroes = heroes.map { user ->
                    user.getAvatarData(size = AvatarSize.RoomDetailsHeader)
                }.toImmutableList(),
                isTombstoned = isTombstoned,
            ),
            contentDescription = stringResource(CommonStrings.a11y_room_avatar),
            modifier = Modifier
                .clip(CircleShape)
                .clickable(
                    enabled = avatarUrl != null,
                    onClickLabel = stringResource(CommonStrings.action_view),
                ) {
                    openAvatarPreview(avatarUrl!!)
                }
                .testTag(TestTags.roomDetailAvatar)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = roomName,
            style = ElementTheme.typography.fontHeadingLgBold,
            textAlign = TextAlign.Center,
        )
        if (roomAlias != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                modifier = Modifier.niceClickable { onSubtitleClick(roomAlias.value) },
                text = roomAlias.value,
                style = ElementTheme.typography.fontBodyLgRegular,
                color = ElementTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun DmHeaderSection(
    otherMember: RoomMember,
    roomName: String,
    isTombstoned: Boolean,
    openAvatarPreview: (name: String, url: String) -> Unit,
    onSubtitleClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Avatar(
            avatarData = AvatarData(otherMember.userId.value, roomName, otherMember.avatarUrl, AvatarSize.RoomDetailsHeader),
            avatarType = AvatarType.Room(
                heroes = persistentListOf(
                    otherMember.getAvatarData(size = AvatarSize.RoomDetailsHeader)
                ),
                isTombstoned = isTombstoned,
            ),
            contentDescription = stringResource(CommonStrings.a11y_room_avatar),
            modifier = Modifier
                .clip(CircleShape)
                .clickable(
                    enabled = otherMember.avatarUrl != null,
                    onClickLabel = stringResource(CommonStrings.action_view),
                ) {
                    openAvatarPreview(otherMember.getBestName(), otherMember.avatarUrl!!)
                }
                .testTag(TestTags.roomDetailAvatar)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = roomName,
            style = ElementTheme.typography.fontHeadingLgBold,
            textAlign = TextAlign.Center,
        )
        val userStatus = otherMember.displayedStatus?.toText()
        if (userStatus != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = userStatus,
                style = ElementTheme.typography.fontBodyLgMedium,
                maxLines = 1,
                color = ElementTheme.colors.textSecondary,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            modifier = Modifier.niceClickable { onSubtitleClick(otherMember.userId.value) },
            text = otherMember.userId.value,
            style = ElementTheme.typography.fontBodyLgRegular,
            color = ElementTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun BadgeList(
    roomBadge: ImmutableList<RoomBadge>,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (roomBadge.isNotEmpty()) {
            MatrixBadgeRowMolecule(
                data = roomBadge.map {
                    it.toMatrixBadgeData()
                }.toImmutableList(),
            )
        }
    }
}

@Composable
private fun RoomBadge.toMatrixBadgeData(): MatrixBadgeAtom.MatrixBadgeData {
    return when (this) {
        RoomBadge.ENCRYPTED -> {
            MatrixBadgeAtom.MatrixBadgeData(
                text = stringResource(R.string.screen_room_details_badge_encrypted),
                icon = CompoundIcons.LockSolid(),
                type = MatrixBadgeAtom.Type.Positive,
            )
        }
        RoomBadge.NOT_ENCRYPTED -> {
            MatrixBadgeAtom.MatrixBadgeData(
                text = stringResource(R.string.screen_room_details_badge_not_encrypted),
                icon = CompoundIcons.LockOff(),
                type = MatrixBadgeAtom.Type.Info,
            )
        }
        RoomBadge.PUBLIC -> {
            MatrixBadgeAtom.MatrixBadgeData(
                text = stringResource(R.string.screen_room_details_badge_public),
                icon = CompoundIcons.Public(),
                type = MatrixBadgeAtom.Type.Info,
            )
        }
        RoomBadge.SHARED_HISTORY_HIDDEN -> {
            MatrixBadgeAtom.MatrixBadgeData(
                text = stringResource(R.string.crypto_history_sharing_room_info_hidden_badge_content),
                icon = CompoundIcons.VisibilityOff(),
                type = MatrixBadgeAtom.Type.Info
            )
        }
        RoomBadge.SHARED_HISTORY_SHARED -> {
            MatrixBadgeAtom.MatrixBadgeData(
                text = stringResource(R.string.crypto_history_sharing_room_info_shared_badge_content),
                icon = CompoundIcons.History(),
                type = MatrixBadgeAtom.Type.Info
            )
        }
        RoomBadge.SHARED_HISTORY_WORLD_READABLE -> {
            MatrixBadgeAtom.MatrixBadgeData(
                text = stringResource(R.string.crypto_history_sharing_room_info_world_readable_badge_content),
                icon = CompoundIcons.UserProfileSolid(),
                type = MatrixBadgeAtom.Type.Info
            )
        }
    }
}

@Composable
private fun TopicSection(
    roomTopic: RoomTopicState,
    onActionClick: (RoomDetailsAction) -> Unit,
) {
    PreferenceCategory(
        title = stringResource(CommonStrings.common_topic),
        showTopDivider = false,
    ) {
        if (roomTopic is RoomTopicState.CanAddTopic) {
            ListItem(
                leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Plus())),
                content = {
                    Text(stringResource(id = R.string.screen_room_details_add_topic_title))
                },
                onClick = {
                    onActionClick(RoomDetailsAction.AddTopic)
                },
            )
        } else if (roomTopic is RoomTopicState.ExistingTopic) {
            ClickableLinkText(
                text = roomTopic.topic,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                interactionSource = remember { MutableInteractionSource() },
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.tertiary,
                ),
            )
        }
    }
}

@Composable
private fun NotificationItem(
    isDefaultMode: Boolean,
    openRoomNotificationSettings: () -> Unit,
) {
    val subtitle = if (isDefaultMode) {
        stringResource(R.string.screen_room_details_notification_mode_default)
    } else {
        stringResource(R.string.screen_room_details_notification_mode_custom)
    }
    ListItem(
        content = { Text(text = stringResource(R.string.screen_room_details_notification_title)) },
        supportingContent = { Text(text = subtitle) },
        leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Notifications())),
        onClick = openRoomNotificationSettings,
    )
}

@Composable
private fun SecurityAndPrivacyItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        content = { Text(stringResource(R.string.screen_room_details_security_and_privacy_title)) },
        leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Lock())),
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun FavoriteItem(
    isFavorite: Boolean,
    onFavoriteChanges: (Boolean) -> Unit,
) {
    val (textResId, icon) = if (isFavorite) {
        CommonStrings.common_favourited to CompoundIcons.FavouriteSolid()
    } else {
        CommonStrings.common_favourite to CompoundIcons.Favourite()
    }
    PreferenceSwitch(
        icon = icon,
        title = stringResource(id = textResId),
        isChecked = isFavorite,
        onCheckedChange = onFavoriteChanges
    )
}

@Composable
private fun ProfileItem(
    verificationState: UserProfileVerificationState,
    onClick: () -> Unit,
) {
    ListItem(
        leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.UserProfile())),
        content = { Text(stringResource(id = R.string.screen_room_details_profile_row_title)) },
        trailingContent = when (verificationState) {
            UserProfileVerificationState.VERIFIED -> ListItemContent.Icon(
                iconSource = IconSource.Vector(CompoundIcons.Verified()),
                tintColor = ElementTheme.colors.iconSuccessPrimary,
            )
            UserProfileVerificationState.VERIFICATION_VIOLATION -> ListItemContent.Icon(
                iconSource = IconSource.Vector(CompoundIcons.ErrorSolid()),
                tintColor = ElementTheme.colors.iconCriticalPrimary,
            )
            else -> null
        },
        onClick = onClick,
    )
}

@Composable
private fun MembersItem(
    memberCount: Long,
    hasVerificationViolations: Boolean,
    openRoomMemberList: () -> Unit,
) {
    ListItem(
        content = { Text(stringResource(CommonStrings.common_people)) },
        leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.User())),
        trailingContent = if (hasVerificationViolations) {
            ListItemContent.Icon(
                iconSource = IconSource.Vector(CompoundIcons.ErrorSolid()),
                tintColor = ElementTheme.colors.textCriticalPrimary,
            )
        } else {
            ListItemContent.Text(memberCount.toString())
        },
        onClick = openRoomMemberList,
    )
}

@Composable
private fun InviteItem(
    onClick: () -> Unit,
) {
    ListItem(
        content = { Text(stringResource(R.string.screen_room_details_invite_title)) },
        leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.UserAdd())),
        onClick = onClick,
    )
}

@Composable
private fun PinnedMessagesItem(
    pinnedMessagesCount: Int?,
    onPinnedMessagesClick: () -> Unit,
) {
    val analyticsService = LocalAnalyticsService.current
    ListItem(
        content = { Text(stringResource(R.string.screen_room_details_pinned_events_row_title)) },
        leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Pin())),
        trailingContent =
            if (pinnedMessagesCount == null) {
                ListItemContent.Custom {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                }
            } else {
                ListItemContent.Text(pinnedMessagesCount.toString())
            },
        onClick = {
            analyticsService.captureInteraction(Interaction.Name.PinnedMessageRoomInfoButton)
            onPinnedMessagesClick()
        }
    )
}

@Composable
private fun PollsItem(
    openPollHistory: () -> Unit,
) {
    ListItem(
        content = { Text(stringResource(R.string.screen_polls_history_title)) },
        leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Polls())),
        onClick = openPollHistory,
    )
}

@Composable
private fun MediaGalleryItem(
    onClick: () -> Unit,
) {
    ListItem(
        content = { Text(stringResource(R.string.screen_room_details_media_gallery_title)) },
        leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Image())),
        onClick = onClick,
    )
}

@Composable
private fun OtherActionsSection(
    canReportRoom: Boolean,
    onReportRoomClick: () -> Unit,
    onLeaveRoomClick: () -> Unit,
    dmOtherMemberDetailsState: UserProfileState?,
) {
    PreferenceCategory {
        if (dmOtherMemberDetailsState != null) {
            BlockUserSection(dmOtherMemberDetailsState)
            BlockUserDialogs(dmOtherMemberDetailsState)
        }
        if (canReportRoom) {
            ListItem(
                content = {
                    Text(stringResource(CommonStrings.action_report_room))
                },
                leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.ChatProblem())),
                style = ListItemStyle.Destructive,
                onClick = onReportRoomClick,
            )
        }
        ListItem(
            content = {
                Text(stringResource(CommonStrings.action_leave_room))
            },
            leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Leave())),
            style = ListItemStyle.Destructive,
            onClick = onLeaveRoomClick,
        )
    }
}

@Composable
private fun DebugInfoSection(
    roomId: RoomId,
    roomVersion: String?,
) {
    val context = LocalContext.current
    PreferenceCategory {
        val toastMessage = stringResource(CommonStrings.common_copied_to_clipboard)
        ListItem(
            content = {
                Text("Internal room ID")
            },
            supportingContent = {
                Text(
                    text = roomId.value,
                    style = ElementTheme.typography.fontBodySmRegular,
                    color = ElementTheme.colors.textSecondary,
                )
            },
            leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Code())),
            trailingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Copy())),
            onClick = {
                context.copyToClipboard(
                    text = roomId.value,
                    toastMessage = toastMessage,
                )
            },
        )
        ListItem(
            content = {
                Text("Room version")
            },
            supportingContent = {
                Text(
                    text = roomVersion ?: "Unknown",
                    style = ElementTheme.typography.fontBodySmRegular,
                    color = ElementTheme.colors.textSecondary,
                )
            },
            leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Info())),
        )
    }
}

@Composable
private fun ContactMergeSection(
    state: RoomDetailsState,
    modifier: Modifier = Modifier,
) {
    var showMergeDialog by remember { mutableStateOf(false) }

    PreferenceCategory(
        modifier = modifier,
    ) {
        val mergedContact = state.mergedContact
        if (mergedContact != null) {
            state.siblingRooms.forEach { sibling ->
                ListItem(
                    content = {
                        Text(
                            text = sibling.name,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    supportingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = sibling.network,
                                style = MaterialTheme.typography.bodySmall,
                                color = ElementTheme.colors.textSecondary,
                            )
                            if (sibling.isActive) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "• Activo para enviar",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ElementTheme.colors.textSuccessPrimary,
                                )
                            }
                        }
                    },
                    leadingContent = ListItemContent.Custom {
                        Avatar(
                            avatarData = AvatarData(
                                id = sibling.roomId.value,
                                name = sibling.name,
                                url = sibling.avatarUrl,
                                size = AvatarSize.RoomListItem,
                            ),
                            avatarType = AvatarType.Room(),
                        )
                    },
                    trailingContent = ListItemContent.Custom {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!sibling.isActive) {
                                IconButton(onClick = {
                                    state.eventSink(RoomDetailsEvent.SetActiveMergeRoom(sibling.roomId))
                                }) {
                                    Icon(
                                        imageVector = CompoundIcons.Check(),
                                        contentDescription = "Hacer activo",
                                        tint = ElementTheme.colors.iconSecondary,
                                    )
                                }
                            }
                            IconButton(onClick = {
                                state.eventSink(RoomDetailsEvent.UnlinkMergedRoom(sibling.roomId))
                            }) {
                                Icon(
                                    imageVector = CompoundIcons.Delete(),
                                    contentDescription = "Desvincular",
                                    tint = ElementTheme.colors.iconCriticalPrimary,
                                )
                            }
                        }
                    }
                )
            }
            ListItem(
                content = {
                    Text(
                        text = "+ Vincular otra sala",
                        style = MaterialTheme.typography.bodyLarge,
                        color = ElementTheme.colors.textActionPrimary,
                    )
                },
                leadingContent = ListItemContent.Icon(
                    iconSource = IconSource.Vector(CompoundIcons.Link())
                ),
                onClick = { showMergeDialog = true },
            )
            ListItem(
                content = {
                    Text(
                        text = "Desvincular todas las salas",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                leadingContent = ListItemContent.Icon(
                    iconSource = IconSource.Vector(CompoundIcons.Delete())
                ),
                style = ListItemStyle.Destructive,
                onClick = { state.eventSink(RoomDetailsEvent.UnmergeAll) },
            )
        } else {
            ListItem(
                content = {
                    Text(
                        text = "Vincular con otra sala",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                supportingContent = {
                    Text(
                        text = "Unificar este chat con WhatsApp, Instagram u otra sala",
                        style = MaterialTheme.typography.bodySmall,
                        color = ElementTheme.colors.textSecondary,
                    )
                },
                leadingContent = ListItemContent.Icon(
                    iconSource = IconSource.Vector(CompoundIcons.Link())
                ),
                onClick = { showMergeDialog = true },
            )
        }
    }

    if (showMergeDialog) {
        MergeRoomSelectionDialog(
            availableRooms = state.availableRoomsToMerge,
            onRoomSelected = { targetRoomId ->
                state.eventSink(RoomDetailsEvent.MergeWithRoom(targetRoomId))
                showMergeDialog = false
            },
            onDismiss = { showMergeDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MergeRoomSelectionDialog(
    availableRooms: ImmutableList<SelectRoomInfo>,
    onRoomSelected: (RoomId) -> Unit,
    onDismiss: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredRooms = remember(availableRooms, searchQuery) {
        if (searchQuery.isBlank()) {
            availableRooms
        } else {
            availableRooms.filter {
                (it.name ?: "").contains(searchQuery, ignoreCase = true) ||
                    it.roomId.value.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    androidx.compose.material3.BasicAlertDialog(
        onDismissRequest = onDismiss,
    ) {
        androidx.compose.material3.Surface(
            shape = MaterialTheme.shapes.large,
            color = ElementTheme.colors.bgCanvasDefault,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Vincular con otra sala",
                    style = MaterialTheme.typography.titleLarge,
                    color = ElementTheme.colors.textPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                androidx.compose.material3.OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar chat...", color = ElementTheme.colors.textSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    singleLine = true,
                )
                if (filteredRooms.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (availableRooms.isEmpty()) "No hay otras salas disponibles" else "No se encontraron salas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ElementTheme.colors.textSecondary
                        )
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(filteredRooms.size) { index ->
                            val item = filteredRooms[index]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onRoomSelected(item.roomId) }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Avatar(
                                    avatarData = item.getAvatarData(AvatarSize.RoomListItem),
                                    avatarType = AvatarType.Room(),
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name ?: item.roomId.value,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = detectNetwork(item.roomId, item.name),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ElementTheme.colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    androidx.compose.material3.TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = ElementTheme.colors.textActionPrimary)
                    }
                }
            }
        }
    }
}


@PreviewWithExtraLargeHeight
@Composable
internal fun RoomDetailsPreview(@PreviewParameter(RoomDetailsStatePreviewParam::class) state: RoomDetailsState) =
    ElementPreviewLight { ContentToPreview(state) }

@PreviewWithExtraLargeHeight
@Composable
internal fun RoomDetailsDarkPreview(@PreviewParameter(RoomDetailsStatePreviewParam::class) state: RoomDetailsState) =
    ElementPreviewDark { ContentToPreview(state) }

@PreviewWithExtraLargeHeight
@Composable
internal fun RoomDetailsA11yPreview() = ElementPreview {
    ContentToPreview(
        state = aRoomDetailsState(displayAdminSettings = true)
    )
}

@ExcludeFromCoverage
@Composable
private fun ContentToPreview(state: RoomDetailsState) {
    RoomDetailsView(
        state = state,
        goBack = {},
        onActionClick = {},
        onShareRoom = {},
        openRoomMemberList = {},
        openRoomNotificationSettings = {},
        invitePeople = {},
        openAvatarPreview = { _, _ -> },
        openPollHistory = {},
        openMediaGallery = {},
        openAdminSettings = {},
        onJoinCallClick = {},
        onPinnedMessagesClick = {},
        onKnockRequestsClick = {},
        onSecurityAndPrivacyClick = {},
        onProfileClick = {},
        onReportRoomClick = {},
        leaveRoomView = {},
    )
}
