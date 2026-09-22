/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.push.impl.notifications

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.preferences.api.store.AppPreferencesStore
import io.element.android.libraries.preferences.api.store.GroupNotificationCooldown
import kotlinx.coroutines.flow.first
import java.util.concurrent.ConcurrentHashMap

interface GroupNotificationThrottler {
    suspend fun shouldSilenceGroupNotification(roomId: RoomId, now: Long = System.currentTimeMillis()): Boolean
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultGroupNotificationThrottler @Inject constructor(
    private val appPreferencesStore: AppPreferencesStore,
) : GroupNotificationThrottler {

    private val lastAlertTimestampByRoom = ConcurrentHashMap<RoomId, Long>()

    override suspend fun shouldSilenceGroupNotification(roomId: RoomId, now: Long): Boolean {
        val cooldown = appPreferencesStore.getGroupNotificationCooldownFlow().first()
        if (cooldown == GroupNotificationCooldown.OFF || cooldown.durationSeconds <= 0L) {
            return false
        }

        val cooldownMillis = cooldown.durationSeconds * 1000L
        val lastTimestamp = lastAlertTimestampByRoom[roomId]

        if (lastTimestamp != null && now - lastTimestamp < cooldownMillis) {
            // Still in cooldown period; silence this notification
            return true
        }

        // Cooldown passed or first notification in this room; record timestamp and allow sound
        lastAlertTimestampByRoom[roomId] = now
        return false
    }
}
