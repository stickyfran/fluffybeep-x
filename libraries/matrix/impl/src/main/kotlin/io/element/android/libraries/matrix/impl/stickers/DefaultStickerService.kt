/*
 * Copyright (c) 2026 Element Creations Ltd. / FluffyBeep
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.stickers

import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.room.IntentionalMention
import io.element.android.libraries.matrix.api.stickers.StickerItem
import io.element.android.libraries.matrix.api.stickers.StickerPack
import io.element.android.libraries.matrix.api.stickers.StickerService
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.json.JSONObject
import timber.log.Timber

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
            val room = client.getJoinedRoom(roomId) ?: return Result.failure(IllegalStateException("Room not found: $roomId"))
            room.use { r ->
                val alt = sticker.body.ifBlank { sticker.key }
                val html = """<img data-mx-emoticon="" src="${sticker.url}" alt="$alt" title="$alt" />"""
                // Send as rich message with HTML image tag conforming to MSC2545
                r.liveTimeline.sendMessage(
                    body = alt,
                    htmlBody = html,
                    intentionalMentions = emptyList<IntentionalMention>(),
                )
            }
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
