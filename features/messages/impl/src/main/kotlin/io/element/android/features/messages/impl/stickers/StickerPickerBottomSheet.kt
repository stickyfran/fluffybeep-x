/*
 * Copyright (c) 2026 Element Creations Ltd. / FluffyBeep
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.stickers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.theme.components.ModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.stickers.StickerItem
import io.element.android.libraries.matrix.api.stickers.StickerPack
import io.element.android.libraries.matrix.ui.media.MediaRequestData
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerPickerBottomSheet(
    packs: ImmutableList<StickerPack>,
    onStickerSelected: (StickerItem) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        scrollable = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(bottom = 16.dp),
        ) {
            // Title Header
            Text(
                text = "Stickers",
                style = ElementTheme.typography.fontHeadingSmMedium,
                color = ElementTheme.colors.textPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (packs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Sin stickers disponibles",
                        style = ElementTheme.typography.fontBodyMdRegular,
                        color = ElementTheme.colors.textSecondary,
                    )
                }
            } else {
                var selectedPackIndex by remember { mutableIntStateOf(0) }
                val currentPack = packs.getOrNull(selectedPackIndex) ?: packs.first()

                // Pack Tabs Row (Using Row + horizontalScroll to avoid intrinsic measurement crashes in BottomSheet)
                if (packs.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        packs.forEachIndexed { index, pack ->
                            val isSelected = index == selectedPackIndex
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) ElementTheme.colors.bgActionPrimaryRest
                                        else ElementTheme.colors.bgSubtleSecondary
                                    )
                                    .clickable { selectedPackIndex = index }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = pack.displayName,
                                    style = if (isSelected) ElementTheme.typography.fontBodySmMedium else ElementTheme.typography.fontBodySmRegular,
                                    color = if (isSelected) ElementTheme.colors.textOnSolidPrimary else ElementTheme.colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Grid of stickers in current pack chunked by 4 per row
                val chunkedStickers = remember(currentPack) {
                    currentPack.stickers.chunked(4)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(chunkedStickers) { rowStickers ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            for (sticker in rowStickers) {
                                StickerCell(
                                    sticker = sticker,
                                    onClick = {
                                        onStickerSelected(sticker)
                                        onDismissRequest()
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            // Fill remaining space if last row has less than 4 items
                            val emptySlots = 4 - rowStickers.size
                            for (i in 0 until emptySlots) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StickerCell(
    sticker: StickerItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ElementTheme.colors.bgSubtleSecondary)
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (sticker.url.isNotBlank()) {
            AsyncImage(
                model = MediaRequestData(
                    source = MediaSource(sticker.url),
                    kind = MediaRequestData.Kind.File(
                        fileName = sticker.key,
                        mimeType = "image/webp",
                    ),
                ),
                contentDescription = sticker.body,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = sticker.body.take(3),
                style = ElementTheme.typography.fontHeadingSmMedium,
                color = ElementTheme.colors.textPrimary,
                maxLines = 1,
            )
        }
    }
}
