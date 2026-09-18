/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.contactmerge

import androidx.compose.runtime.Immutable
import io.element.android.libraries.matrix.api.core.RoomId

@Immutable
data class MergedRoomSummary(
    val roomId: RoomId,
    val name: String,
    val avatarUrl: String? = null,
    val isActive: Boolean = false,
    val network: String = "Matrix",
)

fun detectNetwork(roomId: RoomId, roomName: String? = null): String {
    val idStr = roomId.value.lowercase()
    val nameStr = (roomName ?: "").lowercase()
    return when {
        idStr.contains("whatsapp") || nameStr.contains("whatsapp") || idStr.contains("_wa_") -> "WhatsApp"
        idStr.contains("instagram") || nameStr.contains("instagram") || idStr.contains("_ig_") -> "Instagram"
        idStr.contains("telegram") || nameStr.contains("telegram") || idStr.contains("_tg_") -> "Telegram"
        idStr.contains("signal") || nameStr.contains("signal") || idStr.contains("_signal_") -> "Signal"
        else -> "Matrix"
    }
}
