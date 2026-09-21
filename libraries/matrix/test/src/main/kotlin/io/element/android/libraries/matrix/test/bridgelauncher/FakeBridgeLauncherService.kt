/*
 * Copyright (c) 2026 Element Creations Ltd. / FluffyBeep
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.test.bridgelauncher

import io.element.android.libraries.matrix.api.bridgelauncher.BridgeLauncherService
import io.element.android.libraries.matrix.api.core.RoomId

class FakeBridgeLauncherService(
    private val waPhoneByRoom: Map<RoomId, String> = emptyMap(),
    private val igIdByRoom: Map<RoomId, String> = emptyMap(),
    var canDrawOverlaysResult: Boolean = true,
) : BridgeLauncherService {

    val openWhatsAppCalls = mutableListOf<String?>()
    val openInstagramCalls = mutableListOf<String?>()
    var launchWhatsAppAppCallCount = 0
    val dialPhoneCalls = mutableListOf<String>()

    override fun extractWhatsAppPhone(roomId: RoomId): String? = waPhoneByRoom[roomId]

    override fun extractInstagramId(roomId: RoomId): String? = igIdByRoom[roomId]

    override suspend fun openWhatsApp(phone: String?, packageName: String): Boolean {
        openWhatsAppCalls.add(phone)
        return true
    }

    override suspend fun openInstagram(userOrId: String?, packageName: String): Boolean {
        openInstagramCalls.add(userOrId)
        return true
    }

    override suspend fun launchWhatsAppApp(): Boolean {
        launchWhatsAppAppCallCount++
        return true
    }

    override suspend fun dialPhone(phone: String): Boolean {
        dialPhoneCalls.add(phone)
        return true
    }

    override suspend fun canDrawOverlays(): Boolean = canDrawOverlaysResult

    override suspend fun requestOverlayPermission(): Boolean = true
}
