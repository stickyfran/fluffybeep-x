/*
 * Copyright (c) 2026 Element Creations Ltd. / FluffyBeep
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.stickers

import io.element.android.libraries.matrix.api.core.RoomId
import kotlinx.collections.immutable.ImmutableList

interface StickerService {
    /**
     * Returns all sticker packs available for this room (from user account data and room state).
     */
    suspend fun getStickerPacks(roomId: RoomId): ImmutableList<StickerPack>

    /**
     * Sends a sticker to the given room.
     */
    suspend fun sendSticker(roomId: RoomId, sticker: StickerItem): Result<Unit>
}
