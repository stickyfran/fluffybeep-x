/*
 * Copyright (c) 2026 Element Creations Ltd. / FluffyBeep
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.contactmerge

import io.element.android.libraries.matrix.api.core.RoomId
import kotlinx.coroutines.flow.StateFlow

/**
 * Service for managing merged contacts across multiple bridges/rooms.
 * Persists merges to Matrix account_data (m.fluffybeep.merges).
 */
interface ContactMergeService {
    /** Reactive flow emitting all merged contacts. */
    val mergedContacts: StateFlow<List<MergedContact>>

    /** Returns the merged contact containing the specified room, or null if unmerged. */
    suspend fun getMergedContactForRoom(roomId: RoomId): MergedContact?

    /** Merges multiple rooms into a single contact. */
    suspend fun mergeRooms(
        displayName: String,
        roomIds: List<RoomId>,
        activeRoomId: RoomId = roomIds.first(),
    ): Result<MergedContact>

    /** Unmerges a contact, separating all linked rooms. */
    suspend fun unmergeContact(mergeId: String): Result<Unit>

    /** Adds another room to an existing merged contact. */
    suspend fun addRoomToMerge(mergeId: String, roomId: RoomId): Result<MergedContact>

    /** Removes a single room from a merged contact. */
    suspend fun removeRoomFromMerge(mergeId: String, roomId: RoomId): Result<Unit>

    /** Sets the primary/active room to open when this contact is selected. */
    suspend fun setActiveRoom(mergeId: String, roomId: RoomId): Result<Unit>

    /** Gets saved text draft for a merged contact. */
    fun getDraft(mergeId: String): String?

    /** Sets or clears saved text draft for a merged contact. */
    fun setDraft(mergeId: String, text: String?)
}
