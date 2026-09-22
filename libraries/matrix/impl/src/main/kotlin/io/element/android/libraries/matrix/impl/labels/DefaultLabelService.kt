/*
 * Copyright (c) 2026 Element Creations Ltd. / FluffyBeep
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.labels

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.di.annotations.SessionCoroutineScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.labels.LabelService
import io.element.android.libraries.matrix.api.labels.RoomLabel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

private const val ACCOUNT_DATA_KEY_LABELS = "com.beeper.labels"
private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

@Serializable
private data class LabelDto(
    val title: String = "",
    val emoji: String? = null,
    val isShownInInbox: Boolean = true,
    val rooms: List<String> = emptyList(),
    val createdAt: Long = 0L,
)

@SingleIn(SessionScope::class)
@ContributesBinding(SessionScope::class)
@Inject
class DefaultLabelService(
    private val matrixClient: MatrixClient,
    private val dispatchers: CoroutineDispatchers,
    @SessionCoroutineScope private val sessionCoroutineScope: CoroutineScope,
) : LabelService {

    private val _labels = MutableStateFlow<List<RoomLabel>>(emptyList())
    override val labels: StateFlow<List<RoomLabel>> = _labels.asStateFlow()

    private val _hiddenFromInboxRoomIds = MutableStateFlow<Set<RoomId>>(emptySet())
    override val hiddenFromInboxRoomIds: StateFlow<Set<RoomId>> = _hiddenFromInboxRoomIds.asStateFlow()

    private var cachedHiddenNetworksDto: LabelDto? = null

    init {
        sessionCoroutineScope.launch(dispatchers.io) {
            loadLabels()
        }
    }

    private suspend fun loadLabels() = withContext(dispatchers.io) {
        val rawJson = matrixClient.getAccountData(ACCOUNT_DATA_KEY_LABELS).getOrNull()
        if (rawJson.isNullOrBlank()) return@withContext

        try {
            val map = json.decodeFromString<Map<String, LabelDto>>(rawJson)
            cachedHiddenNetworksDto = map["_hidden_networks"]
            val parsed = map.entries
                .filter { it.key != "_hidden_networks" }
                .map { (id, dto) ->
                    RoomLabel(
                        id = id,
                        title = dto.title,
                        emoji = dto.emoji,
                        isShownInInbox = dto.isShownInInbox,
                        roomIds = dto.rooms.map(::RoomId).toImmutableList(),
                        createdAt = dto.createdAt,
                    )
                }
                .sortedBy { it.createdAt }

            _labels.value = parsed
            updateHiddenInboxSet(parsed)
            Timber.i("DefaultLabelService: loaded ${parsed.size} labels from $ACCOUNT_DATA_KEY_LABELS")
        } catch (e: Exception) {
            Timber.e(e, "DefaultLabelService: failed to decode $ACCOUNT_DATA_KEY_LABELS")
        }
    }

    private suspend fun saveLabels(updated: List<RoomLabel>): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val existingMap = mutableMapOf<String, LabelDto>()

            // Preserve special keys like _hidden_networks from cache
            val hiddenNetworksDto = cachedHiddenNetworksDto
            if (hiddenNetworksDto != null) {
                existingMap["_hidden_networks"] = hiddenNetworksDto
            }

            for (label in updated) {
                existingMap[label.id] = LabelDto(
                    title = label.title,
                    emoji = label.emoji,
                    isShownInInbox = label.isShownInInbox,
                    rooms = label.roomIds.map { it.value },
                    createdAt = label.createdAt,
                )
            }

            val encoded = json.encodeToString(existingMap)
            matrixClient.setAccountData(ACCOUNT_DATA_KEY_LABELS, encoded).getOrThrow()

            _labels.value = updated
            updateHiddenInboxSet(updated)
        }
    }

    private fun updateHiddenInboxSet(labels: List<RoomLabel>) {
        val hidden = labels
            .filter { !it.isShownInInbox }
            .flatMap { it.roomIds }
            .toSet()
        _hiddenFromInboxRoomIds.value = hidden
    }

    override suspend fun createLabel(
        title: String,
        emoji: String?,
        isShownInInbox: Boolean,
    ): Result<RoomLabel> = withContext(dispatchers.io) {
        runCatching {
            val id = "label_${System.currentTimeMillis()}"
            val newLabel = RoomLabel(
                id = id,
                title = title,
                emoji = emoji,
                isShownInInbox = isShownInInbox,
                roomIds = emptyList<RoomId>().toImmutableList(),
                createdAt = System.currentTimeMillis() / 1000,
            )
            saveLabels(_labels.value + newLabel).getOrThrow()
            newLabel
        }
    }

    override suspend fun updateLabel(
        labelId: String,
        title: String,
        emoji: String?,
        isShownInInbox: Boolean,
    ): Result<Unit> = withContext(dispatchers.io) {
        val updated = _labels.value.map {
            if (it.id == labelId) it.copy(title = title, emoji = emoji, isShownInInbox = isShownInInbox) else it
        }
        saveLabels(updated)
    }

    override suspend fun deleteLabel(labelId: String): Result<Unit> = withContext(dispatchers.io) {
        val updated = _labels.value.filterNot { it.id == labelId }
        saveLabels(updated)
    }

    override suspend fun addRoomsToLabel(labelId: String, roomIds: List<RoomId>): Result<Unit> = withContext(dispatchers.io) {
        val updated = _labels.value.map { label ->
            if (label.id == labelId) {
                val combined = (label.roomIds + roomIds).distinct()
                label.copy(roomIds = combined.toImmutableList())
            } else {
                label
            }
        }
        saveLabels(updated)
    }

    override suspend fun removeRoomsFromLabel(labelId: String, roomIds: List<RoomId>): Result<Unit> = withContext(dispatchers.io) {
        val updated = _labels.value.map { label ->
            if (label.id == labelId) {
                val remaining = label.roomIds.filterNot { it in roomIds }
                label.copy(roomIds = remaining.toImmutableList())
            } else {
                label
            }
        }
        saveLabels(updated)
    }

    override fun getLabelsForRooms(roomIds: Set<RoomId>): List<RoomLabel> {
        return _labels.value.filter { label -> label.roomIds.any { it in roomIds } }
    }

    override fun isRoomHiddenFromInbox(roomId: RoomId): Boolean {
        return roomId in _hiddenFromInboxRoomIds.value
    }
}
