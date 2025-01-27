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

package com.wire.kalium.logic.feature.user.guestroomlink

import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.configuration.GuestRoomLinkStatus
import com.wire.kalium.logic.configuration.UserConfigRepository
import com.wire.kalium.logic.functional.Either
import io.mockative.Mock
import io.mockative.any
import io.mockative.eq
import io.mockative.given
import io.mockative.mock
import io.mockative.once
import io.mockative.verify
import kotlin.test.BeforeTest
import kotlin.test.Test

class MarkGuestLinkFeatureFlagAsNotChangedUseCaseTest {

    @Mock
    val userConfigRepository: UserConfigRepository = mock(UserConfigRepository::class)

    lateinit var markGuestLinkFeatureFlagAsNotChanged: MarkGuestLinkFeatureFlagAsNotChangedUseCase

    @BeforeTest
    fun setUp() {
        markGuestLinkFeatureFlagAsNotChanged = MarkGuestLinkFeatureFlagAsNotChangedUseCaseImpl(userConfigRepository)
    }

    @Test
    fun givenRepositoryReturnsFailure_whenInvokingUseCase_thenDoNotUpdateGuestStatus() {
        given(userConfigRepository).invocation { getGuestRoomLinkStatus() }
            .thenReturn(Either.Left(StorageFailure.DataNotFound))

        markGuestLinkFeatureFlagAsNotChanged()

        verify(userConfigRepository).function(userConfigRepository::getGuestRoomLinkStatus)
            .wasInvoked(exactly = once)

        verify(userConfigRepository).function(userConfigRepository::setGuestRoomStatus).with(any(), eq(false)).wasNotInvoked()
    }

    @Test
    fun givenRepositoryReturnsSuccess_whenInvokingUseCase_thenUpdateGuestStatus() {
        given(userConfigRepository).invocation { getGuestRoomLinkStatus() }
            .thenReturn(Either.Right(GuestRoomLinkStatus(isGuestRoomLinkEnabled = true, isStatusChanged = false)))
        given(userConfigRepository).invocation { setGuestRoomStatus(status = false, isStatusChanged = false) }
            .thenReturn(Either.Right(Unit))

        markGuestLinkFeatureFlagAsNotChanged()

        verify(userConfigRepository).function(userConfigRepository::getGuestRoomLinkStatus)
            .wasInvoked(exactly = once)
        verify(userConfigRepository).function(userConfigRepository::setGuestRoomStatus).with(any(), eq(false)).wasInvoked(once)

    }
}
