/*
 * Copyright (c) 2026 Element Creations Ltd. / FluffyBeep
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.labels

import io.element.android.libraries.matrix.api.core.RoomId
import kotlinx.coroutines.flow.StateFlow

/**
 * Service for managing room and contact labels, persisting to com.beeper.labels account data.
 */
interface LabelService {
    /** All user-defined labels, reactive. */
    val labels: StateFlow<List<RoomLabel>>

    /** Set of room IDs that should be hidden from the primary inbox. */
    val hiddenFromInboxRoomIds: StateFlow<Set<RoomId>>

    /** Creates a new label. */
    suspend fun createLabel(
        title: String,
        emoji: String? = null,
        isShownInInbox: Boolean = true,
    ): Result<RoomLabel>

    /** Updates existing label properties. */
    suspend fun updateLabel(
        labelId: String,
        title: String,
        emoji: String? = null,
        isShownInInbox: Boolean = true,
    ): Result<Unit>

    /** Deletes a label by ID. */
    suspend fun deleteLabel(labelId: String): Result<Unit>

    /** Adds room IDs to a label. */
    suspend fun addRoomsToLabel(labelId: String, roomIds: List<RoomId>): Result<Unit>

    /** Removes room IDs from a label. */
    suspend fun removeRoomsFromLabel(labelId: String, roomIds: List<RoomId>): Result<Unit>

    /** Returns all labels assigned to any of the specified [roomIds]. */
    fun getLabelsForRooms(roomIds: Set<RoomId>): List<RoomLabel>

    /** Checks whether a room is currently marked as hidden from the inbox. */
    fun isRoomHiddenFromInbox(roomId: RoomId): Boolean
}
