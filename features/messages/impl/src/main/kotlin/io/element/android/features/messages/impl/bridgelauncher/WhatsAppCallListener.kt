/*
 * Copyright (c) 2026 Element Creations Ltd. / FluffyBeep
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.bridgelauncher

import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.timeline.item.event.EventTimelineItem
import timber.log.Timber

/**
 * Listens for incoming WhatsApp bridge call messages and automatically launches
 * the native WhatsApp application with screen wakeup.
 */
class WhatsAppCallListener(
    private val client: MatrixClient,
) {
    companion object {
        const val WA_INCOMING_CALL_BODY = "Incoming call. Use the WhatsApp app to answer."
        private const val MAX_CALL_FRESHNESS_MS = 45_000L
    }

    /**
     * Checks if a timeline event represents an active incoming WhatsApp call.
     */
    suspend fun shouldAutoLaunch(event: EventTimelineItem): Boolean {
        if (!client.bridgeLauncherService.isAutoOpenWhatsAppOnCallEnabled()) {
            return false
        }

        val body = event.content.let {
            // Check text-based content
            val textContent = (it as? io.element.android.libraries.matrix.api.timeline.item.event.MessageContent)
            textContent?.body
        } ?: return false

        if (!body.contains(WA_INCOMING_CALL_BODY)) return false

        val age = System.currentTimeMillis() - event.timestamp
        if (age > MAX_CALL_FRESHNESS_MS) {
            Timber.d("WhatsAppCallListener: ignoring stale call event (age: ${age}ms)")
            return false
        }

        return true
    }

    /**
     * Executes the call redirect action.
     */
    suspend fun onIncomingCallDetected() {
        Timber.i("WhatsAppCallListener: Incoming WhatsApp call detected! Launching app...")
        client.bridgeLauncherService.launchWhatsAppApp()
    }
}
