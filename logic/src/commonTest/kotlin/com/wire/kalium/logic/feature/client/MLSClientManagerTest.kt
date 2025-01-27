/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.kalium.logic.feature.client

import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.client.ClientRepository
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.sync.InMemoryIncrementalSyncRepository
import com.wire.kalium.logic.data.sync.IncrementalSyncRepository
import com.wire.kalium.logic.data.sync.IncrementalSyncStatus
import com.wire.kalium.logic.data.sync.SlowSyncRepository
import com.wire.kalium.logic.feature.CurrentClientIdProvider
import com.wire.kalium.logic.feature.user.IsMLSEnabledUseCase
import com.wire.kalium.logic.framework.TestClient
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.test_util.TestKaliumDispatcher
import io.mockative.Mock
import io.mockative.any
import io.mockative.anything
import io.mockative.classOf
import io.mockative.given
import io.mockative.mock
import io.mockative.once
import io.mockative.verify
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test

class MLSClientManagerTest {

    @Test
    fun givenMLSSupportIsDisabled_whenObservingSyncFinishes_thenMLSClientIsNotRegistered() =
        runTest(TestKaliumDispatcher.default) {
            val (arrangement, _) = Arrangement()
                .withIsMLSEnabled(false)
                .arrange()

            arrangement.incrementalSyncRepository.updateIncrementalSyncState(IncrementalSyncStatus.Live)
            yield()

            verify(arrangement.registerMLSClient)
                .suspendFunction(arrangement.registerMLSClient::invoke)
                .with(any())
                .wasNotInvoked()
        }

    @Test
    fun givenMLSClientIsNotRegistered_whenObservingSyncFinishes_thenMLSClientIsRegistered() =
        runTest(TestKaliumDispatcher.default) {
            val (arrangement, _) = Arrangement()
                .withIsMLSEnabled(true)
                .withHasRegisteredMLSClient(Either.Right(false))
                .withCurrentClientId(Either.Right(TestClient.CLIENT_ID))
                .withRegisterMLSClientSuccessful()
                .arrange()

            arrangement.incrementalSyncRepository.updateIncrementalSyncState(IncrementalSyncStatus.Live)
            yield()

            verify(arrangement.registerMLSClient)
                .suspendFunction(arrangement.registerMLSClient::invoke)
                .with(any())
                .wasInvoked(once)

            verify(arrangement.slowSyncRepository)
                .suspendFunction(arrangement.slowSyncRepository::clearLastSlowSyncCompletionInstant)
                .wasInvoked(once)
        }

    @Test
    fun givenMLSClientIsRegistered_whenObservingSyncFinishes_thenMLSClientIsNotRegistered() =
        runTest(TestKaliumDispatcher.default) {
            val (arrangement, _) = Arrangement()
                .withIsMLSEnabled(true)
                .withHasRegisteredMLSClient(Either.Right(true))
                .arrange()

            arrangement.incrementalSyncRepository.updateIncrementalSyncState(IncrementalSyncStatus.Live)
            yield()

            verify(arrangement.registerMLSClient)
                .suspendFunction(arrangement.registerMLSClient::invoke)
                .with(any())
                .wasNotInvoked()
        }

    private class Arrangement {

        val incrementalSyncRepository: IncrementalSyncRepository = InMemoryIncrementalSyncRepository()

        @Mock
        var slowSyncRepository = mock(classOf<SlowSyncRepository>())

        @Mock
        var clientIdProvider = mock(classOf<CurrentClientIdProvider>())

        @Mock
        val clientRepository = mock(classOf<ClientRepository>())

        @Mock
        val isMLSEnabled = mock(classOf<IsMLSEnabledUseCase>())

        @Mock
        val registerMLSClient = mock(classOf<RegisterMLSClientUseCase>())

        fun withCurrentClientId(result: Either<CoreFailure, ClientId>) = apply {
            given(clientIdProvider)
                .suspendFunction(clientIdProvider::invoke)
                .whenInvoked()
                .thenReturn(result)
        }

        fun withHasRegisteredMLSClient(result: Either<CoreFailure, Boolean>) = apply {
            given(clientRepository)
                .suspendFunction(clientRepository::hasRegisteredMLSClient)
                .whenInvoked()
                .thenReturn(result)
        }

        fun withRegisterMLSClientSuccessful() = apply {
            given(registerMLSClient)
                .suspendFunction(registerMLSClient::invoke)
                .whenInvokedWith(anything())
                .thenReturn(Either.Right(Unit))
        }

        fun withIsMLSEnabled(enabled: Boolean) = apply {
            given(isMLSEnabled)
                .function(isMLSEnabled::invoke)
                .whenInvoked()
                .thenReturn(enabled)
        }

        fun arrange() = this to MLSClientManagerImpl(
            clientIdProvider,
            isMLSEnabled,
            incrementalSyncRepository,
            lazy { slowSyncRepository },
            lazy { clientRepository },
            lazy { registerMLSClient },
            TestKaliumDispatcher
        )
    }
}
