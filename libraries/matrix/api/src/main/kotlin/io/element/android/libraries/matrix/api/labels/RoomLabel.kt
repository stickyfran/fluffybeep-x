/*
 * Copyright (c) 2026 Element Creations Ltd. / FluffyBeep
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.labels

import androidx.compose.runtime.Immutable
import io.element.android.libraries.matrix.api.core.RoomId
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class RoomLabel(
    val id: String,
    val title: String,
    val emoji: String? = null,
    val isShownInInbox: Boolean = true,
    val roomIds: ImmutableList<RoomId>,
    val createdAt: Long = 0L,
) {
    val displayText: String
        get() = if (emoji != null) "$emoji $title" else title
}
