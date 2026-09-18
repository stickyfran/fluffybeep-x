/*
 * Copyright (c) 2026 Element Creations Ltd. / FluffyBeep
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.contactmerge

import io.element.android.libraries.matrix.api.core.RoomId

/**
 * Represents a contact whose chats from multiple bridge networks (e.g. WhatsApp, Instagram)
 * are unified into a single logical contact.
 */
data class MergedContact(
    val id: String,
    val displayName: String,
    val roomIds: List<RoomId>,
    val activeRoomId: RoomId,
    val customAvatarUrl: String? = null,
)
