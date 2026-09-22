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
import io.element.android.libraries.di.annotations.AppCoroutineScope
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.preferences.api.store.AppPreferencesStore
import io.element.android.libraries.preferences.api.store.GroupNotificationCooldown
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.ConcurrentHashMap

interface GroupNotificationThrottler {
    suspend fun shouldSilenceGroupNotification(roomId: RoomId, now: Long = System.currentTimeMillis()): Boolean
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultGroupNotificationThrottler @Inject constructor(
    private val appPreferencesStore: AppPreferencesStore,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
) : GroupNotificationThrottler {

    private val lastAlertTimestampByRoom = ConcurrentHashMap<RoomId, Long>()

    private val cooldownFlow: StateFlow<GroupNotificationCooldown> =
        appPreferencesStore.getGroupNotificationCooldownFlow()
            .stateIn(appCoroutineScope, SharingStarted.Eagerly, GroupNotificationCooldown.OFF)

    override suspend fun shouldSilenceGroupNotification(roomId: RoomId, now: Long): Boolean {
        val cooldown = cooldownFlow.value
        if (cooldown == GroupNotificationCooldown.OFF || cooldown.durationSeconds <= 0L) {
            return false
        }

        val cooldownMillis = cooldown.durationSeconds * 1000L
        var silenced = false
        lastAlertTimestampByRoom.compute(roomId) { _, prev ->
            if (prev != null && now - prev < cooldownMillis) {
                silenced = true
                prev
            } else {
                silenced = false
                now
            }
        }

        return silenced
    }
}
