/*
 * Copyright (c) 2026 Element Creations Ltd. / FluffyBeep
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.bridgelauncher

import io.element.android.libraries.matrix.api.core.RoomId

/**
 * Service for detecting bridge identities (WhatsApp, Instagram) and launching native apps or dialers.
 */
interface BridgeLauncherService {

    /**
     * Extracts WhatsApp phone number for a room, checking members/heroes.
     * Returns null if no valid phone number is detected.
     */
    fun extractWhatsAppPhone(roomId: RoomId): String?

    /**
     * Extracts Instagram user or numeric ID for a room.
     */
    fun extractInstagramId(roomId: RoomId): String?

    /**
     * Launches WhatsApp to chat with the specified [phone].
     * If [phone] is null or empty, opens the WhatsApp application.
     */
    suspend fun openWhatsApp(phone: String? = null, packageName: String = "com.whatsapp"): Boolean

    /**
     * Launches Instagram to open the profile of [userOrId].
     * If [userOrId] is null or empty, opens the Instagram main page.
     */
    suspend fun openInstagram(userOrId: String? = null, packageName: String = "com.instagram.android"): Boolean

    /**
     * Direct launch of WhatsApp app without any specific target.
     * Wakes up the screen if needed. Ideal for incoming call handling.
     */
    suspend fun launchWhatsAppApp(): Boolean

    /**
     * Dials a phone number using the native system dialer.
     */
    suspend fun dialPhone(phone: String): Boolean

    /**
     * Checks if SYSTEM_ALERT_WINDOW ("Display over other apps") permission is granted.
     */
    suspend fun canDrawOverlays(): Boolean

    /**
     * Opens system settings to request overlay permission.
     */
    suspend fun requestOverlayPermission(): Boolean

    /**
     * Whether incoming WhatsApp calls should automatically open the WhatsApp application.
     */
    suspend fun isAutoOpenWhatsAppOnCallEnabled(): Boolean

    /**
     * Sets whether incoming WhatsApp calls should automatically open the WhatsApp application.
     */
    suspend fun setAutoOpenWhatsAppOnCallEnabled(enabled: Boolean)
}
