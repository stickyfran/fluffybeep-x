/*
 * Copyright (c) 2026 Element Creations Ltd. / FluffyBeep
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.stickers

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.parcelize.Parcelize

@Immutable
@Parcelize
data class StickerPack(
    val packId: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val stickers: ImmutableList<StickerItem>,
) : Parcelable
