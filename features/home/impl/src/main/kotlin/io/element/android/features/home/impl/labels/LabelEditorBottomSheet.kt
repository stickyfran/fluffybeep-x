/*
 * Copyright (c) 2026 Element Creations Ltd. / FluffyBeep
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.labels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.components.preferences.PreferenceSwitch
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.ModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.OutlinedButton
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.utils.scaffoldScrollableContentInsets
import io.element.android.libraries.matrix.api.labels.RoomLabel

private val SUGGESTED_EMOJIS = listOf(
    "💼", "🏠", "👥", "⭐", "📌", "🔥", "🛒", "🎉", "✈️", "💡", "🎮", "❤️", "🏷️"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelEditorBottomSheet(
    labelToEdit: RoomLabel? = null,
    onSave: (title: String, emoji: String?, isShownInInbox: Boolean) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember { mutableStateOf(labelToEdit?.title.orEmpty()) }
    var selectedEmoji by remember { mutableStateOf(labelToEdit?.emoji ?: "🏷️") }
    var isShownInInbox by remember { mutableStateOf(labelToEdit?.isShownInInbox ?: true) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        scrollable = false,
        contentWindowInsets = { scaffoldScrollableContentInsets },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (labelToEdit != null) "Editar etiqueta" else "Nueva etiqueta",
                style = ElementTheme.typography.fontHeadingSmMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Emoji selector row
            Text(
                text = "Ícono / Emoji",
                style = ElementTheme.typography.fontBodyMdMedium,
                color = ElementTheme.colors.textSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                SUGGESTED_EMOJIS.forEach { emoji ->
                    val isSelected = selectedEmoji == emoji
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { selectedEmoji = emoji }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            style = if (isSelected) ElementTheme.typography.fontHeadingMdBold else ElementTheme.typography.fontBodyLgMedium,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Nombre de la etiqueta") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Show in inbox toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isShownInInbox = !isShownInInbox }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mostrar en bandeja principal",
                        style = ElementTheme.typography.fontBodyLgMedium,
                    )
                    Text(
                        text = "Si se desactiva, los chats con esta etiqueta no saturarán tu inbox principal.",
                        style = ElementTheme.typography.fontBodyXsRegular,
                        color = ElementTheme.colors.textSecondary,
                    )
                }
                Switch(
                    checked = isShownInInbox,
                    onCheckedChange = { isShownInInbox = it },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (labelToEdit != null && onDelete != null) {
                    OutlinedButton(
                        text = "Eliminar",
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        destructive = true,
                    )
                }
                Button(
                    text = "Guardar",
                    onClick = {
                        if (title.isNotBlank()) {
                            onSave(title.trim(), selectedEmoji, isShownInInbox)
                        }
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
