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

package com.wire.kalium.logic.data.auth.login

import com.wire.kalium.logic.framework.TestUser
import com.wire.kalium.network.api.base.model.SessionDTO
import com.wire.kalium.network.api.base.model.UserDTO
import com.wire.kalium.network.api.base.unauthenticated.LoginApi
import com.wire.kalium.network.utils.NetworkResponse
import io.ktor.http.HttpStatusCode
import io.mockative.Mock
import io.mockative.any
import io.mockative.classOf
import io.mockative.eq
import io.mockative.given
import io.mockative.mock
import io.mockative.once
import io.mockative.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class LoginRepositoryTest {

    @Mock
    val loginApi = mock(classOf<LoginApi>())

    private lateinit var loginRepository: LoginRepository


    @BeforeTest
    fun setup() {
        loginRepository = LoginRepositoryImpl(loginApi)
    }

    @Test
    fun givenAnEmail_whenCallingLoginWithEmail_ThenShouldCallTheApiWithTheCorrectParameters() = runTest {
        val (arrangement, loginRepository) = Arrangement().arrange()

        loginRepository.loginWithEmail(
            email = TEST_EMAIL,
            password = TEST_PASSWORD,
            label = TEST_LABEL,
            shouldPersistClient = TEST_PERSIST_CLIENT,
            secondFactorVerificationCode = TEST_SECOND_FACTOR_CODE
        )

        val expectedParam = LoginApi.LoginParam.LoginWithEmail(
            email = TEST_EMAIL,
            password = TEST_PASSWORD,
            label = TEST_LABEL,
            verificationCode = TEST_SECOND_FACTOR_CODE
        )
        verify(arrangement.loginApi)
            .suspendFunction(arrangement.loginApi::login)
            .with(eq(expectedParam), eq(TEST_PERSIST_CLIENT))
            .wasInvoked(exactly = once)
    }

    @Test
    fun givenAHandle_whenCallingLogin_ThenShouldCallTheApiWithTheCorrectParameters() = runTest {
        val (arrangement, loginRepository) = Arrangement().arrange()

        loginRepository.loginWithHandle(
            handle = TEST_HANDLE,
            password = TEST_PASSWORD,
            label = TEST_LABEL,
            shouldPersistClient = TEST_PERSIST_CLIENT,
        )

        val expectedParam = LoginApi.LoginParam.LoginWithHandle(
            handle = TEST_HANDLE,
            password = TEST_PASSWORD,
            label = TEST_LABEL,
        )
        verify(arrangement.loginApi)
            .suspendFunction(arrangement.loginApi::login)
            .with(eq(expectedParam), eq(TEST_PERSIST_CLIENT))
            .wasInvoked(exactly = once)
    }

    private class Arrangement {

        @Mock
        val loginApi = mock(classOf<LoginApi>())

        init {
            withLoginReturning(
                NetworkResponse.Success(value = SESSION_DTO to USER_DTO, mapOf(), HttpStatusCode.OK.value)
            )
        }

        fun withLoginReturning(response: NetworkResponse<Pair<SessionDTO, UserDTO>>) = apply {
            given(loginApi)
                .suspendFunction(loginApi::login)
                .whenInvokedWith(any(), any())
                .thenReturn(response)
        }

        fun arrange(): Pair<Arrangement, LoginRepository> = this to LoginRepositoryImpl(loginApi)

    }

    private companion object {
        const val TEST_LABEL = "test_label"
        const val TEST_EMAIL = "user@example.org"
        const val TEST_HANDLE = "cool_user"
        const val TEST_SECOND_FACTOR_CODE = "123456"
        const val TEST_PASSWORD = "123456"
        const val TEST_PERSIST_CLIENT = false
        val USER_DTO: UserDTO = TestUser.USER_DTO
        val SESSION_DTO: SessionDTO = SessionDTO(
            userId = USER_DTO.id,
            tokenType = "tokenType",
            accessToken = "access_token",
            refreshToken = "refresh_token",
            cookieLabel = "cookieLabel"
        )
    }
}
