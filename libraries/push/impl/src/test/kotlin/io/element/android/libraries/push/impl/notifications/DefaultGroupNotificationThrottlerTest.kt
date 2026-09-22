/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.push.impl.notifications

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.preferences.api.store.GroupNotificationCooldown
import io.element.android.libraries.preferences.test.InMemoryAppPreferencesStore
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultGroupNotificationThrottlerTest {

    @Test
    fun `when cooldown is OFF, should never silence notifications`() = runTest {
        val appPreferencesStore = InMemoryAppPreferencesStore(
            groupNotificationCooldown = GroupNotificationCooldown.OFF
        )
        val throttler = DefaultGroupNotificationThrottler(appPreferencesStore, this)
        val roomId = RoomId("!room:matrix.org")

        val now = 1000000L
        assertThat(throttler.shouldSilenceGroupNotification(roomId, now)).isFalse()
        assertThat(throttler.shouldSilenceGroupNotification(roomId, now + 1000L)).isFalse()
        assertThat(throttler.shouldSilenceGroupNotification(roomId, now + 2000L)).isFalse()
    }

    @Test
    fun `when cooldown is 30 seconds, silence rapid messages and alert after cooldown passes`() = runTest {
        val appPreferencesStore = InMemoryAppPreferencesStore(
            groupNotificationCooldown = GroupNotificationCooldown.THIRTY_SECONDS
        )
        val throttler = DefaultGroupNotificationThrottler(appPreferencesStore, this)
        val roomId = RoomId("!room:matrix.org")

        val now = 1000000L
        // First message: alerts and starts cooldown window
        assertThat(throttler.shouldSilenceGroupNotification(roomId, now)).isFalse()

        // Message 5 seconds later: should be silenced
        assertThat(throttler.shouldSilenceGroupNotification(roomId, now + 5000L)).isTrue()

        // Message 29 seconds later: should still be silenced
        assertThat(throttler.shouldSilenceGroupNotification(roomId, now + 29000L)).isTrue()

        // Message 31 seconds later: cooldown has elapsed, alerts and starts new window
        assertThat(throttler.shouldSilenceGroupNotification(roomId, now + 31000L)).isFalse()

        // Message 2 seconds after the new alert: should be silenced again
        assertThat(throttler.shouldSilenceGroupNotification(roomId, now + 33000L)).isTrue()
    }

    @Test
    fun `cooldown tracks rooms independently`() = runTest {
        val appPreferencesStore = InMemoryAppPreferencesStore(
            groupNotificationCooldown = GroupNotificationCooldown.ONE_MINUTE
        )
        val throttler = DefaultGroupNotificationThrottler(appPreferencesStore, this)
        val room1 = RoomId("!room1:matrix.org")
        val room2 = RoomId("!room2:matrix.org")

        val now = 1000000L
        // Room 1 alerts
        assertThat(throttler.shouldSilenceGroupNotification(room1, now)).isFalse()
        // Room 1 silenced 10s later
        assertThat(throttler.shouldSilenceGroupNotification(room1, now + 10000L)).isTrue()

        // Room 2 receives its first message: alerts independently
        assertThat(throttler.shouldSilenceGroupNotification(room2, now + 10000L)).isFalse()

        // Room 2 silenced 5s later
        assertThat(throttler.shouldSilenceGroupNotification(room2, now + 15000L)).isTrue()
    }

    @Test
    fun `when cooldown setting is updated dynamically, throttler reflects the new value`() = runTest {
        val appPreferencesStore = InMemoryAppPreferencesStore(
            groupNotificationCooldown = GroupNotificationCooldown.OFF
        )
        val throttler = DefaultGroupNotificationThrottler(appPreferencesStore, this)
        val roomId = RoomId("!room:matrix.org")
        val now = 1000000L

        // Initially OFF
        assertThat(throttler.shouldSilenceGroupNotification(roomId, now)).isFalse()
        assertThat(throttler.shouldSilenceGroupNotification(roomId, now + 1000L)).isFalse()

        // Change to 30s
        appPreferencesStore.setGroupNotificationCooldown(GroupNotificationCooldown.THIRTY_SECONDS)
        testScheduler.advanceUntilIdle()

        // First message with 30s cooldown triggers window
        assertThat(throttler.shouldSilenceGroupNotification(roomId, now + 2000L)).isFalse()
        // Rapid message inside window is silenced
        assertThat(throttler.shouldSilenceGroupNotification(roomId, now + 5000L)).isTrue()
    }
}
