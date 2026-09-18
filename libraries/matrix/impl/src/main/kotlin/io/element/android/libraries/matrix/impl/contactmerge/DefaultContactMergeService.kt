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
import java.util.concurrent.ConcurrentHashMap

private const val ACCOUNT_DATA_MERGES_TYPE = "m.fluffybeep.merges"
private const val ACCOUNT_DATA_BEEPER_MERGES = "com.beeper.merged_contacts"

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

@Serializable
private data class BeeperMergedContactDto(
    val displayName: String? = null,
    val avatarMxc: String? = null,
    val roomIds: List<String> = emptyList(),
    val createdAt: Long? = null,
)

@Serializable
private data class BeeperMergesContainer(
    val contacts: Map<String, BeeperMergedContactDto> = emptyMap(),
)

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
    private val drafts = ConcurrentHashMap<String, String>()

    init {
        sessionCoroutineScope.launch(dispatchers.io) {
            loadMerges()
        }
    }

    private suspend fun loadMerges() = withContext(dispatchers.io) {
        val loaded = mutableListOf<MergedContact>()

        // 1. Try loading from com.beeper.merged_contacts (FluffyBeep format)
        val beeperJson = matrixClient.getAccountData(ACCOUNT_DATA_BEEPER_MERGES).getOrNull()
        if (!beeperJson.isNullOrBlank()) {
            try {
                val beeperContainer = jsonProvider().decodeFromString<BeeperMergesContainer>(beeperJson)
                beeperContainer.contacts.forEach { (id, dto) ->
                    if (dto.roomIds.size >= 2) {
                        loaded.add(
                            MergedContact(
                                id = id,
                                displayName = dto.displayName ?: "Contacto fusionado",
                                roomIds = dto.roomIds.map(::RoomId),
                                activeRoomId = RoomId(dto.roomIds.first()),
                                customAvatarUrl = dto.avatarMxc,
                            )
                        )
                    }
                }
                Timber.i("Loaded ${loaded.size} merged contacts from $ACCOUNT_DATA_BEEPER_MERGES")
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse $ACCOUNT_DATA_BEEPER_MERGES")
            }
        }

        // 2. Try loading from m.fluffybeep.merges
        val rawJson = matrixClient.getAccountData(ACCOUNT_DATA_MERGES_TYPE).getOrNull()
        if (!rawJson.isNullOrBlank()) {
            try {
                val container = jsonProvider().decodeFromString<MergesContainer>(rawJson)
                val fluffyList = container.merges.map { it.toModel() }
                for (contact in fluffyList) {
                    if (loaded.none { it.id == contact.id }) {
                        loaded.add(contact)
                    }
                }
                Timber.i("Loaded ${loaded.size} total merged contacts after checking $ACCOUNT_DATA_MERGES_TYPE")
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse $ACCOUNT_DATA_MERGES_TYPE")
            }
        }

        _mergedContacts.value = loaded
    }

    private suspend fun saveMerges(contacts: List<MergedContact>): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            // Save to m.fluffybeep.merges
            val container = MergesContainer(merges = contacts.map { it.toDto() })
            val jsonString = jsonProvider().encodeToString(container)
            matrixClient.setAccountData(ACCOUNT_DATA_MERGES_TYPE, jsonString)

            // Also save to com.beeper.merged_contacts for full FluffyBeep compatibility
            val beeperContacts = contacts.associate { mc ->
                mc.id to BeeperMergedContactDto(
                    displayName = mc.displayName,
                    avatarMxc = mc.customAvatarUrl,
                    roomIds = mc.roomIds.map { it.value },
                    createdAt = System.currentTimeMillis() / 1000,
                )
            }
            val beeperJson = jsonProvider().encodeToString(BeeperMergesContainer(beeperContacts))
            matrixClient.setAccountData(ACCOUNT_DATA_BEEPER_MERGES, beeperJson).getOrThrow()

            _mergedContacts.value = contacts
            Timber.i("Saved ${contacts.size} merged contacts to account_data (both keys)")
        }
    }

    override fun getDraft(mergeId: String): String? = drafts[mergeId]

    override fun setDraft(mergeId: String, text: String?) {
        if (text.isNullOrBlank()) {
            drafts.remove(mergeId)
        } else {
            drafts[mergeId] = text
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
