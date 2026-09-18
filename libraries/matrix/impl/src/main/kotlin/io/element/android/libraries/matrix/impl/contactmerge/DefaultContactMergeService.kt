/*
 * Copyright (c) 2026 Element Creations Ltd. / FluffyBeep
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.contactmerge

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.androidutils.json.JsonProvider
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.di.annotations.SessionCoroutineScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.contactmerge.ContactMergeService
import io.element.android.libraries.matrix.api.contactmerge.MergedContact
import io.element.android.libraries.matrix.api.core.RoomId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import timber.log.Timber
import java.util.UUID

private const val ACCOUNT_DATA_MERGES_TYPE = "m.fluffybeep.merges"

@Serializable
private data class MergesContainer(
    val merges: List<MergedContactDto> = emptyList(),
)

@Serializable
private data class MergedContactDto(
    val id: String,
    val displayName: String,
    val roomIds: List<String>,
    val activeRoomId: String,
    val customAvatarUrl: String? = null,
) {
    fun toModel(): MergedContact = MergedContact(
        id = id,
        displayName = displayName,
        roomIds = roomIds.map(::RoomId),
        activeRoomId = RoomId(activeRoomId),
        customAvatarUrl = customAvatarUrl,
    )
}

private fun MergedContact.toDto(): MergedContactDto = MergedContactDto(
    id = id,
    displayName = displayName,
    roomIds = roomIds.map { it.value },
    activeRoomId = activeRoomId.value,
    customAvatarUrl = customAvatarUrl,
)

@SingleIn(SessionScope::class)
@ContributesBinding(SessionScope::class)
@Inject
class DefaultContactMergeService(
    private val matrixClient: MatrixClient,
    private val dispatchers: CoroutineDispatchers,
    private val jsonProvider: JsonProvider,
    @SessionCoroutineScope private val sessionCoroutineScope: CoroutineScope,
) : ContactMergeService {

    private val _mergedContacts = MutableStateFlow<List<MergedContact>>(emptyList())
    override val mergedContacts: StateFlow<List<MergedContact>> = _mergedContacts.asStateFlow()

    init {
        sessionCoroutineScope.launch(dispatchers.io) {
            loadMerges()
        }
    }

    private suspend fun loadMerges() = withContext(dispatchers.io) {
        val rawJson = matrixClient.getAccountData(ACCOUNT_DATA_MERGES_TYPE).getOrNull()
        if (!rawJson.isNullOrBlank()) {
            try {
                val container = jsonProvider().decodeFromString<MergesContainer>(rawJson)
                _mergedContacts.value = container.merges.map { it.toModel() }
                Timber.i("Loaded ${_mergedContacts.value.size} merged contacts from account_data")
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse merged contacts from account_data")
            }
        }
    }

    private suspend fun saveMerges(contacts: List<MergedContact>): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val container = MergesContainer(merges = contacts.map { it.toDto() })
            val jsonString = jsonProvider().encodeToString(container)
            matrixClient.setAccountData(ACCOUNT_DATA_MERGES_TYPE, jsonString).getOrThrow()
            _mergedContacts.value = contacts
            Timber.i("Saved ${contacts.size} merged contacts to account_data")
        }
    }

    override suspend fun getMergedContactForRoom(roomId: RoomId): MergedContact? {
        return _mergedContacts.value.firstOrNull { roomId in it.roomIds }
    }

    override suspend fun mergeRooms(
        displayName: String,
        roomIds: List<RoomId>,
        activeRoomId: RoomId,
    ): Result<MergedContact> = withContext(dispatchers.io) {
        runCatching {
            val newContact = MergedContact(
                id = "merge_${UUID.randomUUID()}",
                displayName = displayName,
                roomIds = roomIds.distinct(),
                activeRoomId = activeRoomId,
            )
            val updated = _mergedContacts.value
                .filterNot { existing -> existing.roomIds.any { it in roomIds } } + newContact
            saveMerges(updated).getOrThrow()
            newContact
        }
    }

    override suspend fun unmergeContact(mergeId: String): Result<Unit> = withContext(dispatchers.io) {
        val updated = _mergedContacts.value.filterNot { it.id == mergeId }
        saveMerges(updated)
    }

    override suspend fun addRoomToMerge(mergeId: String, roomId: RoomId): Result<MergedContact> = withContext(dispatchers.io) {
        runCatching {
            val current = _mergedContacts.value.firstOrNull { it.id == mergeId }
                ?: throw IllegalArgumentException("Merge $mergeId not found")
            if (roomId in current.roomIds) return@runCatching current

            val updatedContact = current.copy(roomIds = current.roomIds + roomId)
            val updatedList = _mergedContacts.value.map { if (it.id == mergeId) updatedContact else it }
            saveMerges(updatedList).getOrThrow()
            updatedContact
        }
    }

    override suspend fun removeRoomFromMerge(mergeId: String, roomId: RoomId): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val current = _mergedContacts.value.firstOrNull { it.id == mergeId }
                ?: throw IllegalArgumentException("Merge $mergeId not found")
            val newRoomIds = current.roomIds.filterNot { it == roomId }
            if (newRoomIds.size <= 1) {
                // If only 1 room remains, unmerge completely
                unmergeContact(mergeId).getOrThrow()
            } else {
                val newActiveRoomId = if (current.activeRoomId == roomId) newRoomIds.first() else current.activeRoomId
                val updatedContact = current.copy(roomIds = newRoomIds, activeRoomId = newActiveRoomId)
                val updatedList = _mergedContacts.value.map { if (it.id == mergeId) updatedContact else it }
                saveMerges(updatedList).getOrThrow()
            }
        }
    }

    override suspend fun setActiveRoom(mergeId: String, roomId: RoomId): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val current = _mergedContacts.value.firstOrNull { it.id == mergeId }
                ?: throw IllegalArgumentException("Merge $mergeId not found")
            if (roomId !in current.roomIds) {
                throw IllegalArgumentException("Room $roomId is not part of merge $mergeId")
            }
            val updatedContact = current.copy(activeRoomId = roomId)
            val updatedList = _mergedContacts.value.map { if (it.id == mergeId) updatedContact else it }
            saveMerges(updatedList).getOrThrow()
        }
    }
}
