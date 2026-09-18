/*
 * Copyright (c) 2026 Element Creations Ltd. / FluffyBeep
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.test.spaces

import io.element.android.libraries.matrix.api.spaces.QuickSpacesData
import io.element.android.libraries.matrix.api.spaces.QuickSpacesService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeQuickSpacesService(
    initialData: QuickSpacesData = QuickSpacesData(),
) : QuickSpacesService {

    private val _state = MutableStateFlow(initialData)
    override val state: StateFlow<QuickSpacesData> = _state.asStateFlow()

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
        _state.value = QuickSpacesData(newPinned, newHidden)
    }

    override suspend fun hideFromQuickBar(spaceId: String) {
        val current = _state.value
        _state.value = QuickSpacesData(current.pinnedSpaceIds - spaceId, current.hiddenSpaceIds + spaceId)
    }

    override suspend fun pinToQuickBar(spaceId: String) {
        val current = _state.value
        _state.value = QuickSpacesData(current.pinnedSpaceIds + spaceId, current.hiddenSpaceIds - spaceId)
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
