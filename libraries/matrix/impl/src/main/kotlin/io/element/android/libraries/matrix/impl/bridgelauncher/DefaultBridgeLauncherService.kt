/*
 * Copyright (c) 2026 Element Creations Ltd. / FluffyBeep
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.bridgelauncher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.di.annotations.ApplicationContext
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.bridgelauncher.BridgeLauncherService
import io.element.android.libraries.matrix.api.core.RoomId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

private val WA_FALLBACK_PACKAGES = listOf(
    "com.whatsapp",
    "com.whatsapp.w4b",
    "com.gbwhatsapp",
    "com.yowhatsapp",
)

@SingleIn(SessionScope::class)
@ContributesBinding(SessionScope::class)
@Inject
class DefaultBridgeLauncherService(
    @ApplicationContext private val context: Context? = null,
    private val matrixClient: MatrixClient,
) : BridgeLauncherService {

    override fun extractWhatsAppPhone(roomId: RoomId): String? {
        // 1. Direct check on roomId string
        BridgeIdParser.extractWhatsAppPhone(roomId.value)?.let { return it }

        // 2. Check merged contact custom fields if available
        val merges = matrixClient.contactMergeService.mergedContacts.value
        val merged = merges.firstOrNull { roomId in it.roomIds }
        if (!merged?.customWhatsAppPhone.isNullOrBlank()) {
            return merged?.customWhatsAppPhone
        }

        // 3. Check sibling rooms in the merge
        if (merged != null) {
            for (siblingId in merged.roomIds) {
                if (siblingId == roomId) continue
                BridgeIdParser.extractWhatsAppPhone(siblingId.value)?.let { return it }
            }
        }

        return null
    }

    override fun extractInstagramId(roomId: RoomId): String? {
        BridgeIdParser.extractInstagramId(roomId.value)?.let { return it }

        val merges = matrixClient.contactMergeService.mergedContacts.value
        val merged = merges.firstOrNull { roomId in it.roomIds }
        if (!merged?.customInstagramHandle.isNullOrBlank()) {
            return merged?.customInstagramHandle
        }

        if (merged != null) {
            for (siblingId in merged.roomIds) {
                if (siblingId == roomId) continue
                BridgeIdParser.extractInstagramId(siblingId.value)?.let { return it }
            }
        }

        return null
    }

    override suspend fun openWhatsApp(phone: String?, packageName: String): Boolean = withContext(Dispatchers.IO) {
        val targetPackage = if (isPackageInstalled(packageName)) {
            packageName
        } else {
            WA_FALLBACK_PACKAGES.firstOrNull { isPackageInstalled(it) } ?: "com.whatsapp"
        }

        if (!phone.isNullOrBlank()) {
            val cleanPhone = phone.replace(Regex("""\D"""), "")
            if (cleanPhone.isNotEmpty()) {
                // Try whatsapp://send URI with target package
                val waUri = Uri.parse("whatsapp://send?phone=$cleanPhone")
                if (tryLaunchIntent(Intent(Intent.ACTION_VIEW, waUri).setPackage(targetPackage))) {
                    return@withContext true
                }
                // Try api.whatsapp.com URL with target package
                val apiUri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone")
                if (tryLaunchIntent(Intent(Intent.ACTION_VIEW, apiUri).setPackage(targetPackage))) {
                    return@withContext true
                }
                // Try without package restriction
                if (tryLaunchIntent(Intent(Intent.ACTION_VIEW, waUri))) {
                    return@withContext true
                }
            }
        }

        launchWhatsAppApp()
    }

    override suspend fun openInstagram(userOrId: String?, packageName: String): Boolean = withContext(Dispatchers.IO) {
        val targetPackage = if (isPackageInstalled(packageName)) packageName else "com.instagram.android"

        if (!userOrId.isNullOrBlank()) {
            val isNumeric = userOrId.matches(Regex("""\d+"""))
            val uri = if (isNumeric) {
                Uri.parse("instagram://user?user_id=$userOrId")
            } else {
                Uri.parse("https://instagram.com/$userOrId")
            }
            if (isPackageInstalled(targetPackage)) {
                if (tryLaunchIntent(Intent(Intent.ACTION_VIEW, uri).setPackage(targetPackage))) {
                    return@withContext true
                }
            }
            if (tryLaunchIntent(Intent(Intent.ACTION_VIEW, uri))) {
                return@withContext true
            }
        }

        val fallbackUri = Uri.parse("https://instagram.com")
        tryLaunchIntent(Intent(Intent.ACTION_VIEW, fallbackUri))
    }

    override suspend fun launchWhatsAppApp(): Boolean = withContext(Dispatchers.IO) {
        wakeScreen()

        val targetContext = context ?: return@withContext false
        val installedPackage = WA_FALLBACK_PACKAGES.firstOrNull { isPackageInstalled(it) } ?: "com.whatsapp"
        val launchIntent = targetContext.packageManager.getLaunchIntentForPackage(installedPackage)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            return@withContext tryLaunchIntent(launchIntent)
        }

        // Try candidate packages
        for (pkg in WA_FALLBACK_PACKAGES) {
            val intent = targetContext.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                if (tryLaunchIntent(intent)) return@withContext true
            }
        }

        false
    }

    override suspend fun dialPhone(phone: String): Boolean = withContext(Dispatchers.IO) {
        val clean = phone.replace(Regex("""\D"""), "")
        if (clean.isBlank()) return@withContext false
        tryLaunchIntent(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+$clean")))
    }

    override suspend fun canDrawOverlays(): Boolean = withContext(Dispatchers.IO) {
        val targetContext = context ?: return@withContext false
        Settings.canDrawOverlays(targetContext)
    }

    override suspend fun requestOverlayPermission(): Boolean {
        val ctx = context ?: return false
        return withContext(Dispatchers.Main) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${ctx.packageName}"),
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(intent)
                true
            } catch (e: Exception) {
                Timber.e(e, "BridgeLauncher: Failed to launch overlay settings")
                false
            }
        }
    }

    private val prefs by lazy {
        context?.getSharedPreferences("fluffybeep_bridge_launcher", Context.MODE_PRIVATE)
    }

    override suspend fun isAutoOpenWhatsAppOnCallEnabled(): Boolean {
        return prefs?.getBoolean("auto_open_wa_on_call", true) ?: true
    }

    override suspend fun setAutoOpenWhatsAppOnCallEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean("auto_open_wa_on_call", enabled)?.apply()
    }

    private fun isPackageInstalled(pkg: String): Boolean = runCatching {
        val targetContext = context ?: return false
        targetContext.packageManager.getPackageInfo(pkg, 0)
        true
    }.getOrDefault(false)

    private fun tryLaunchIntent(intent: Intent): Boolean = runCatching {
        val targetContext = context ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        targetContext.startActivity(intent)
        true
    }.getOrElse {
        Timber.w(it, "DefaultBridgeLauncherService: failed to launch intent $intent")
        false
    }

    private fun wakeScreen() {
        runCatching {
            val targetContext = context ?: return
            val pm = targetContext.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            @Suppress("DEPRECATION")
            val wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "fluffybeep:call_wake_lock"
            )
            wakeLock.acquire(3000)
        }.onFailure {
            Timber.w(it, "DefaultBridgeLauncherService: failed to acquire wake lock")
        }
    }
}
