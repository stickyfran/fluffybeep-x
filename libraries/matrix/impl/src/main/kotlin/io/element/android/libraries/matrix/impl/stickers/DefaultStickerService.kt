/*
 * Copyright (c) 2026 Element Creations Ltd. / FluffyBeep
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.stickers

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.stickers.StickerItem
import io.element.android.libraries.matrix.api.stickers.StickerPack
import io.element.android.libraries.matrix.api.stickers.StickerService
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.json.JSONObject
import timber.log.Timber

@SingleIn(SessionScope::class)
@ContributesBinding(SessionScope::class)
@Inject
class DefaultStickerService(
    private val client: MatrixClient,
) : StickerService {

    override suspend fun getStickerPacks(roomId: RoomId): ImmutableList<StickerPack> {
        val packs = mutableListOf<StickerPack>()
        try {
            // 1. Account data MSC2545: im.ponies.user_emotes
            val accountData = client.getAccountData("im.ponies.user_emotes").getOrNull()
            if (!accountData.isNullOrBlank()) {
                parseImagePackJson("user_emotes", accountData)?.let { packs.add(it) }
            }

            // 2. Alternative account data key: org.matrix.msc2545.user_emotes
            val altAccountData = client.getAccountData("org.matrix.msc2545.user_emotes").getOrNull()
            if (!altAccountData.isNullOrBlank()) {
                parseImagePackJson("alt_user_emotes", altAccountData)?.let { packs.add(it) }
            }
        } catch (e: Exception) {
            Timber.w(e, "DefaultStickerService: error loading sticker packs")
        }
        return packs.toImmutableList()
    }

    override suspend fun sendSticker(roomId: RoomId, sticker: StickerItem): Result<Unit> {
        return try {
            val room = client.getRoom(roomId) ?: return Result.failure(IllegalStateException("Room not found: $roomId"))
            room.use { r ->
                // Send as message with plain description to the timeline
                r.liveTimeline.sendMessage(
                    body = sticker.body.ifBlank { sticker.key },
                    htmlBody = null,
                    intentionalMentions = emptyList(),
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.w(e, "DefaultStickerService: failed to send sticker to room $roomId")
            Result.failure(e)
        }
    }

    private fun parseImagePackJson(packId: String, jsonStr: String): StickerPack? {
        return try {
            val json = JSONObject(jsonStr)
            val packObj = json.optJSONObject("pack")
            val displayName = packObj?.optString("display_name") ?: "Stickers"
            val avatarUrl = packObj?.optString("avatar_url")
            val imagesObj = json.optJSONObject("images") ?: return null

            val stickers = mutableListOf<StickerItem>()
            val keys = imagesObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val itemObj = imagesObj.optJSONObject(key) ?: continue
                val url = itemObj.optString("url")
                if (url.isNotBlank()) {
                    val body = itemObj.optString("body", key)
                    stickers.add(
                        StickerItem(
                            key = key,
                            body = body,
                            url = url,
                            info = emptyMap(),
                        )
                    )
                }
            }
            if (stickers.isNotEmpty()) {
                StickerPack(
                    packId = packId,
                    displayName = displayName,
                    avatarUrl = avatarUrl,
                    stickers = stickers.toImmutableList(),
                )
            } else null
        } catch (e: Exception) {
            Timber.w(e, "Error parsing image pack JSON")
            null
        }
    }
}
