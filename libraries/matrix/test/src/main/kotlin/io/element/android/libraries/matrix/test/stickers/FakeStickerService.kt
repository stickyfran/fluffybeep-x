/*
 * Copyright (c) 2026 Element Creations Ltd. / FluffyBeep
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.test.stickers

import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.stickers.StickerItem
import io.element.android.libraries.matrix.api.stickers.StickerPack
import io.element.android.libraries.matrix.api.stickers.StickerService
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class FakeStickerService(
    private val packs: List<StickerPack> = emptyList(),
) : StickerService {
    val sentStickers = mutableListOf<Pair<RoomId, StickerItem>>()

    override suspend fun getStickerPacks(roomId: RoomId): ImmutableList<StickerPack> = packs.toImmutableList()

    override suspend fun sendSticker(roomId: RoomId, sticker: StickerItem): Result<Unit> {
        sentStickers.add(roomId to sticker)
        return Result.success(Unit)
    }
}
