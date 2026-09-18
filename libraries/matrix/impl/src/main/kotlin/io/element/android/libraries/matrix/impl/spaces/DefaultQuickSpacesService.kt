/*
 * Copyright (c) 2026 Element Creations Ltd. / FluffyBeep
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.spaces

import io.element.android.libraries.androidutils.json.JsonProvider
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.spaces.QuickSpacesData
import io.element.android.libraries.matrix.api.spaces.QuickSpacesService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import timber.log.Timber

private const val ACCOUNT_DATA_QUICK_SPACES = "m.fluffybeep.quick_spaces"

@Serializable
private data class QuickSpacesDto(
    val pinnedSpaceIds: List<String> = emptyList(),
    val hiddenSpaceIds: List<String> = emptyList(),
)

class DefaultQuickSpacesService(
    private val matrixClient: MatrixClient,
    private val dispatchers: CoroutineDispatchers,
    private val jsonProvider: JsonProvider,
    private val sessionCoroutineScope: CoroutineScope,
) : QuickSpacesService {

    private val _state = MutableStateFlow(QuickSpacesData())
    override val state: StateFlow<QuickSpacesData> = _state.asStateFlow()

    init {
        sessionCoroutineScope.launch(dispatchers.io) {
            load()
        }
    }

    private suspend fun load() = withContext(dispatchers.io) {
        val rawJson = matrixClient.getAccountData(ACCOUNT_DATA_QUICK_SPACES).getOrNull()
        if (!rawJson.isNullOrBlank()) {
            try {
                val dto = jsonProvider().decodeFromString<QuickSpacesDto>(rawJson)
                _state.value = QuickSpacesData(
                    pinnedSpaceIds = dto.pinnedSpaceIds.toSet(),
                    hiddenSpaceIds = dto.hiddenSpaceIds.toSet(),
                )
                Timber.i("Loaded quick spaces: pinned=${dto.pinnedSpaceIds.size}, hidden=${dto.hiddenSpaceIds.size}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse $ACCOUNT_DATA_QUICK_SPACES")
            }
        }
    }

    private suspend fun save(data: QuickSpacesData) = withContext(dispatchers.io) {
        try {
            val dto = QuickSpacesDto(
                pinnedSpaceIds = data.pinnedSpaceIds.toList(),
                hiddenSpaceIds = data.hiddenSpaceIds.toList(),
            )
            val json = jsonProvider().encodeToString(dto)
            matrixClient.setAccountData(ACCOUNT_DATA_QUICK_SPACES, json)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save $ACCOUNT_DATA_QUICK_SPACES")
        }
    }

    override suspend fun togglePin(spaceId: String) {
        val current = _state.value
        val isCurrentlyIn = isSpaceInQuickBar(spaceId, null, null)
        val newPinned = current.pinnedSpaceIds.toMutableSet()
        val newHidden = current.hiddenSpaceIds.toMutableSet()

        if (isCurrentlyIn) {
            newPinned.remove(spaceId)
            newHidden.add(spaceId)
        } else {
            newHidden.remove(spaceId)
            newPinned.add(spaceId)
        }

        val updated = QuickSpacesData(newPinned, newHidden)
        _state.value = updated
        save(updated)
    }

    override suspend fun hideFromQuickBar(spaceId: String) {
        val current = _state.value
        val newPinned = current.pinnedSpaceIds - spaceId
        val newHidden = current.hiddenSpaceIds + spaceId
        val updated = QuickSpacesData(newPinned, newHidden)
        _state.value = updated
        save(updated)
    }

    override suspend fun pinToQuickBar(spaceId: String) {
        val current = _state.value
        val newPinned = current.pinnedSpaceIds + spaceId
        val newHidden = current.hiddenSpaceIds - spaceId
        val updated = QuickSpacesData(newPinned, newHidden)
        _state.value = updated
        save(updated)
    }

    override fun isSpaceInQuickBar(spaceId: String, displayName: String?, alias: String?): Boolean {
        val current = _state.value
        if (spaceId in current.hiddenSpaceIds) return false
        if (spaceId in current.pinnedSpaceIds) return true

        val nameLower = (displayName ?: "").lowercase()
        val aliasLower = (alias ?: "").lowercase()
        val idLower = spaceId.lowercase()
        val isBridge = nameLower.contains("whatsapp") || nameLower.contains("instagram") ||
            aliasLower.contains("whatsapp") || aliasLower.contains("instagram") ||
            idLower.contains("whatsapp") || idLower.contains("instagram") ||
            idLower.contains("_wa_") || idLower.contains("_ig_")
        return !isBridge
    }
}
