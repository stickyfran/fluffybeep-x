/*
 * Copyright (c) 2026 Element Creations Ltd. / FluffyBeep
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.spaces

import kotlinx.coroutines.flow.StateFlow

data class QuickSpacesData(
    val pinnedSpaceIds: Set<String> = emptySet(),
    val hiddenSpaceIds: Set<String> = emptySet(),
)

interface QuickSpacesService {
    val state: StateFlow<QuickSpacesData>
    suspend fun togglePin(spaceId: String)
    suspend fun hideFromQuickBar(spaceId: String)
    suspend fun pinToQuickBar(spaceId: String)
    fun isSpaceInQuickBar(spaceId: String, displayName: String?, alias: String?): Boolean
}
