/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.contactmerge

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import io.element.android.libraries.matrix.api.core.RoomId

@Immutable
data class MergedRoomSummary(
    val roomId: RoomId,
    val name: String,
    val avatarUrl: String? = null,
    val isActive: Boolean = false,
    val network: String = "Matrix",
)

fun detectNetwork(
    roomId: RoomId,
    roomName: String? = null,
    alias: String? = null,
    heroIds: List<String> = emptyList(),
): String {
    val idStr = roomId.value.lowercase()
    val nameStr = (roomName ?: "").lowercase()
    val aliasStr = (alias ?: "").lowercase()
    val heroesCombined = heroIds.joinToString(" ").lowercase()
    val all = "$idStr $nameStr $aliasStr $heroesCombined"
    return when {
        all.contains("whatsapp") || all.contains("_wa_") -> "WhatsApp"
        all.contains("instagram") || all.contains("_ig_") -> "Instagram"
        all.contains("telegram") || all.contains("_tg_") -> "Telegram"
        all.contains("signal") || all.contains("_signal_") -> "Signal"
        all.contains("discord") || all.contains("_discord_") -> "Discord"
        all.contains("facebook") || all.contains("messenger") || all.contains("_fb_") -> "Facebook"
        all.contains("googlechat") || all.contains("_gchat_") -> "Google Chat"
        all.contains("slack") || all.contains("_slack_") -> "Slack"
        else -> "Matrix"
    }
}

fun getNetworkColorHex(network: String): Long {
    return when (network) {
        "WhatsApp" -> 0xFF25D366
        "Instagram" -> 0xFFE1306C
        "Telegram" -> 0xFF2AABEE
        "Signal" -> 0xFF3A76F0
        "Discord" -> 0xFF5865F2
        "Facebook" -> 0xFF00B2FF
        "Google Chat" -> 0xFF34A853
        "Slack" -> 0xFF4A154B
        else -> 0xFF0DBD8B
    }
}

fun getNetworkColor(network: String): Color = Color(getNetworkColorHex(network))
