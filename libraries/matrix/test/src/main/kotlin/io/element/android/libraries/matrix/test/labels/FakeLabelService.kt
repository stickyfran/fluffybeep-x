/*
 * Copyright (c) 2026 Element Creations Ltd. / FluffyBeep
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.test.labels

import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.labels.LabelService
import io.element.android.libraries.matrix.api.labels.RoomLabel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeLabelService(
    initialLabels: List<RoomLabel> = emptyList(),
) : LabelService {

    private val _labels = MutableStateFlow(initialLabels)
    override val labels: StateFlow<List<RoomLabel>> = _labels.asStateFlow()

    private val _hiddenFromInboxRoomIds = MutableStateFlow<Set<RoomId>>(emptySet())
    override val hiddenFromInboxRoomIds: StateFlow<Set<RoomId>> = _hiddenFromInboxRoomIds.asStateFlow()

    override suspend fun createLabel(title: String, emoji: String?, isShownInInbox: Boolean): Result<RoomLabel> {
        val newLabel = RoomLabel(
            id = "label_${System.currentTimeMillis()}",
            title = title,
            emoji = emoji,
            isShownInInbox = isShownInInbox,
            roomIds = emptyList<RoomId>().toImmutableList(),
            createdAt = System.currentTimeMillis() / 1000,
        )
        _labels.value = _labels.value + newLabel
        return Result.success(newLabel)
    }

    override suspend fun updateLabel(labelId: String, title: String, emoji: String?, isShownInInbox: Boolean): Result<Unit> {
        _labels.value = _labels.value.map {
            if (it.id == labelId) it.copy(title = title, emoji = emoji, isShownInInbox = isShownInInbox) else it
        }
        return Result.success(Unit)
    }

    override suspend fun deleteLabel(labelId: String): Result<Unit> {
        _labels.value = _labels.value.filterNot { it.id == labelId }
        return Result.success(Unit)
    }

    override suspend fun addRoomsToLabel(labelId: String, roomIds: List<RoomId>): Result<Unit> {
        _labels.value = _labels.value.map {
            if (it.id == labelId) it.copy(roomIds = (it.roomIds + roomIds).distinct().toImmutableList()) else it
        }
        return Result.success(Unit)
    }

    override suspend fun removeRoomsFromLabel(labelId: String, roomIds: List<RoomId>): Result<Unit> {
        _labels.value = _labels.value.map {
            if (it.id == labelId) it.copy(roomIds = it.roomIds.filterNot { id -> id in roomIds }.toImmutableList()) else it
        }
        return Result.success(Unit)
    }

    override fun getLabelsForRooms(roomIds: Set<RoomId>): List<RoomLabel> {
        return _labels.value.filter { label -> label.roomIds.any { it in roomIds } }
    }

    override fun isRoomHiddenFromInbox(roomId: RoomId): Boolean {
        return roomId in _hiddenFromInboxRoomIds.value
    }

    fun setHiddenFromInboxRoomIds(roomIds: Set<RoomId>) {
        _hiddenFromInboxRoomIds.value = roomIds
    }
}
