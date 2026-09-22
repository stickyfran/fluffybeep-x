/*
 * Copyright (c) 2026 Element Creations Ltd. / FluffyBeep
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.AppScope
import io.element.android.libraries.di.annotations.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

data class NativeContactInfo(
    val id: String,
    val displayName: String,
    val phone: String?,
)

@SingleIn(AppScope::class)
@Inject
class NativeContactsResolver(
    @ApplicationContext private val context: Context,
) {
    private val nameCacheById = ConcurrentHashMap<String, String>()
    private val contactCacheByPhone = ConcurrentHashMap<String, NativeContactInfo>()

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun resolveContactName(phoneContactId: String): String? = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext null
        nameCacheById[phoneContactId]?.let { return@withContext it }

        runCatching {
            val cursor = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
                "${ContactsContract.Contacts._ID} = ?",
                arrayOf(phoneContactId),
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val name = it.getString(0)
                    if (!name.isNullOrBlank()) {
                        nameCacheById[phoneContactId] = name
                        return@withContext name
                    }
                }
            }
        }.onFailure {
            Timber.w(it, "NativeContactsResolver: error querying contact by ID $phoneContactId")
        }

        null
    }

    suspend fun resolveContactByPhone(phone: String): NativeContactInfo? = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext null
        val cleanPhone = phone.replace(Regex("""\D"""), "")
        if (cleanPhone.isBlank()) return@withContext null

        contactCacheByPhone[cleanPhone]?.let { return@withContext it }

        runCatching {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                ),
                "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?",
                arrayOf("%$cleanPhone%"),
                null
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getString(0)
                    val name = it.getString(1)
                    val num = it.getString(2)
                    if (!name.isNullOrBlank() && !num.isNullOrBlank()) {
                        val cleanNum = num.replace(Regex("""\D"""), "")
                        val isMatch = cleanNum == cleanPhone ||
                            (cleanNum.length >= 7 && cleanPhone.endsWith(cleanNum)) ||
                            (cleanPhone.length >= 7 && cleanNum.endsWith(cleanPhone))
                        if (isMatch) {
                            val info = NativeContactInfo(id = id, displayName = name, phone = num)
                            contactCacheByPhone[cleanPhone] = info
                            nameCacheById[id] = name
                            return@withContext info
                        }
                    }
                }
            }
        }.onFailure {
            Timber.w(it, "NativeContactsResolver: error querying contact by phone $phone")
        }

        null
    }
}
