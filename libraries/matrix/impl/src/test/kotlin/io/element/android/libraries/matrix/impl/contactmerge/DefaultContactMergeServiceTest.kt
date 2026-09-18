/*
 * Copyright (c) 2026 Element Creations Ltd. / FluffyBeep
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.contactmerge

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.androidutils.json.DefaultJsonProvider
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.tests.testutils.testCoroutineDispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultContactMergeServiceTest {

    private val room1 = RoomId("!room1:server.org")
    private val room2 = RoomId("!room2:server.org")
    private val room3 = RoomId("!room3:server.org")

    @Test
    fun `mergeRooms - saves merged contact to account_data and emits to state flow`() = runTest {
        val matrixClient = FakeMatrixClient()
        val service = createService(matrixClient, this)

        val result = service.mergeRooms(
            displayName = "Papá",
            roomIds = listOf(room1, room2),
            activeRoomId = room1,
        )

        assertThat(result.isSuccess).isTrue()
        val merged = result.getOrThrow()
        assertThat(merged.displayName).isEqualTo("Papá")
        assertThat(merged.roomIds).containsExactly(room1, room2)
        assertThat(merged.activeRoomId).isEqualTo(room1)

        // Check state flow
        assertThat(service.mergedContacts.value).containsExactly(merged)

        // Check getMergedContactForRoom
        assertThat(service.getMergedContactForRoom(room1)).isEqualTo(merged)
        assertThat(service.getMergedContactForRoom(room2)).isEqualTo(merged)
        assertThat(service.getMergedContactForRoom(room3)).isNull()
    }

    @Test
    fun `setActiveRoom - changes active room for contact`() = runTest {
        val matrixClient = FakeMatrixClient()
        val service = createService(matrixClient, this)

        val merged = service.mergeRooms("Papá", listOf(room1, room2), room1).getOrThrow()
        val setResult = service.setActiveRoom(merged.id, room2)

        assertThat(setResult.isSuccess).isTrue()
        val updated = service.getMergedContactForRoom(room1)
        assertThat(updated?.activeRoomId).isEqualTo(room2)
    }

    @Test
    fun `addRoomToMerge and removeRoomFromMerge work properly`() = runTest {
        val matrixClient = FakeMatrixClient()
        val service = createService(matrixClient, this)

        val merged = service.mergeRooms("Papá", listOf(room1, room2), room1).getOrThrow()

        // Add room3
        val addResult = service.addRoomToMerge(merged.id, room3)
        assertThat(addResult.isSuccess).isTrue()
        assertThat(service.getMergedContactForRoom(room3)).isNotNull()

        // Remove room3
        val removeResult = service.removeRoomFromMerge(merged.id, room3)
        assertThat(removeResult.isSuccess).isTrue()
        assertThat(service.getMergedContactForRoom(room3)).isNull()

        // Remove room2 -> only room1 left, should auto unmerge
        service.removeRoomFromMerge(merged.id, room2)
        assertThat(service.getMergedContactForRoom(room1)).isNull()
        assertThat(service.mergedContacts.value).isEmpty()
    }

    @Test
    fun `unmergeContact - removes merged contact completely`() = runTest {
        val matrixClient = FakeMatrixClient()
        val service = createService(matrixClient, this)

        val merged = service.mergeRooms("Papá", listOf(room1, room2), room1).getOrThrow()
        service.unmergeContact(merged.id)

        assertThat(service.mergedContacts.value).isEmpty()
        assertThat(service.getMergedContactForRoom(room1)).isNull()
    }

    private fun createService(
        matrixClient: FakeMatrixClient,
        testScope: TestScope,
    ): DefaultContactMergeService {
        return DefaultContactMergeService(
            matrixClient = matrixClient,
            dispatchers = testScope.testCoroutineDispatchers(),
            jsonProvider = DefaultJsonProvider(),
            sessionCoroutineScope = testScope,
        )
    }
}
