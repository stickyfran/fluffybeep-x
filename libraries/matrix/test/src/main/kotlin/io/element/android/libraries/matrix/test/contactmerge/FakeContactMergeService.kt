/*
 * Copyright (c) 2026 Element Creations Ltd. / FluffyBeep
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.test.contactmerge

import io.element.android.libraries.matrix.api.contactmerge.ContactMergeService
import io.element.android.libraries.matrix.api.contactmerge.MergedContact
import io.element.android.libraries.matrix.api.core.RoomId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class FakeContactMergeService(
    initialContacts: List<MergedContact> = emptyList(),
) : ContactMergeService {

    private val _mergedContacts = MutableStateFlow(initialContacts)
    override val mergedContacts: StateFlow<List<MergedContact>> = _mergedContacts.asStateFlow()

    override suspend fun getMergedContactForRoom(roomId: RoomId): MergedContact? {
        return _mergedContacts.value.firstOrNull { roomId in it.roomIds }
    }

    override suspend fun mergeRooms(
        displayName: String,
        roomIds: List<RoomId>,
        activeRoomId: RoomId,
    ): Result<MergedContact> {
        val newContact = MergedContact(
            id = "merge_${UUID.randomUUID()}",
            displayName = displayName,
            roomIds = roomIds.distinct(),
            activeRoomId = activeRoomId,
        )
        _mergedContacts.value = _mergedContacts.value
            .filterNot { existing -> existing.roomIds.any { it in roomIds } } + newContact
        return Result.success(newContact)
    }

    override suspend fun unmergeContact(mergeId: String): Result<Unit> {
        _mergedContacts.value = _mergedContacts.value.filterNot { it.id == mergeId }
        return Result.success(Unit)
    }

    override suspend fun addRoomToMerge(mergeId: String, roomId: RoomId): Result<MergedContact> {
        val current = _mergedContacts.value.firstOrNull { it.id == mergeId }
            ?: return Result.failure(IllegalArgumentException("Merge $mergeId not found"))
        val updated = current.copy(roomIds = (current.roomIds + roomId).distinct())
        _mergedContacts.value = _mergedContacts.value.map { if (it.id == mergeId) updated else it }
        return Result.success(updated)
    }

    override suspend fun removeRoomFromMerge(mergeId: String, roomId: RoomId): Result<Unit> {
        val current = _mergedContacts.value.firstOrNull { it.id == mergeId }
            ?: return Result.failure(IllegalArgumentException("Merge $mergeId not found"))
        val newRoomIds = current.roomIds.filterNot { it == roomId }
        if (newRoomIds.size <= 1) {
            unmergeContact(mergeId)
        } else {
            val updated = current.copy(
                roomIds = newRoomIds,
                activeRoomId = if (current.activeRoomId == roomId) newRoomIds.first() else current.activeRoomId
            )
            _mergedContacts.value = _mergedContacts.value.map { if (it.id == mergeId) updated else it }
        }
        return Result.success(Unit)
    }

    override suspend fun setActiveRoom(mergeId: String, roomId: RoomId): Result<Unit> {
        val current = _mergedContacts.value.firstOrNull { it.id == mergeId }
            ?: return Result.failure(IllegalArgumentException("Merge $mergeId not found"))
        val updated = current.copy(activeRoomId = roomId)
        _mergedContacts.value = _mergedContacts.value.map { if (it.id == mergeId) updated else it }
        return Result.success(Unit)
    }
}
