/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.preferences.api.store

/**
 * Cooldown duration options for throttling group notification sounds.
 */
enum class GroupNotificationCooldown(val durationSeconds: Long) {
    OFF(0L),
    THIRTY_SECONDS(30L),
    ONE_MINUTE(60L),
    THIRTY_MINUTES(1800L),
    ONE_HOUR(3600L),
    TWO_HOURS(7200L);

    companion object {
        val DEFAULT = OFF

        fun fromString(value: String?): GroupNotificationCooldown {
            return entries.firstOrNull { it.name == value } ?: DEFAULT
        }
    }
}
