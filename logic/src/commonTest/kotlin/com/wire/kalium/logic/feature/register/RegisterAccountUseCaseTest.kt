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

package com.wire.kalium.logic.feature.register

import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.auth.login.ProxyCredentials
import com.wire.kalium.logic.data.register.RegisterAccountRepository
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.SsoId
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AuthTokens
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.test_util.TestNetworkException
import com.wire.kalium.logic.util.stubs.newServerConfig
import com.wire.kalium.network.exceptions.KaliumException
import io.mockative.Mock
import io.mockative.classOf
import io.mockative.given
import io.mockative.mock
import io.mockative.once
import io.mockative.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterAccountUseCaseTest {
    @Mock
    private val registerAccountRepository = mock(classOf<RegisterAccountRepository>())

    private val serverConfig = TEST_SERVER_CONFIG
    private val proxyCredentials = PROXY_CREDENTIALS

    private lateinit var registerAccountUseCase: RegisterAccountUseCase

    @BeforeTest
    fun setup() {
        registerAccountUseCase = RegisterAccountUseCase(registerAccountRepository, serverConfig, proxyCredentials)
    }

    @Test
    fun givenRepositoryCallIsSuccessful_whenRegisteringPersonalAccount_thenSuccessIsPropagated() = runTest {
        val param = TEST_PRIVATE_ACCOUNT_PARAM
        val ssoId = TEST_SSO_ID
        val authTokens = TEST_AUTH_TOKENS
        val userServerConfig = TEST_SERVER_CONFIG
        val expected = Pair(ssoId, authTokens)

        given(registerAccountRepository).coroutine {
            registerPersonalAccountWithEmail(param.email, param.emailActivationCode, param.name, param.password, param.cookieLabel)
        }.then { Either.Right(Pair(ssoId, authTokens)) }

        val actual = registerAccountUseCase(param)

        assertIs<RegisterResult.Success>(actual)
        assertEquals(expected.first, actual.ssoID)
        assertEquals(expected.second, actual.authData)
        assertEquals(userServerConfig.id, actual.serverConfigId)

        verify(registerAccountRepository).coroutine {
            registerPersonalAccountWithEmail(param.email, param.emailActivationCode, param.name, param.password, param.cookieLabel)
        }.wasInvoked(exactly = once)
    }

    @Test
    fun givenRepositoryCallIsSuccessful_whenRegisteringTeamAccount_thenSuccessIsPropagated() = runTest {
        val param = TEST_TEAM_ACCOUNT_PARAM
        val ssoId = TEST_SSO_ID
        val authTokens = TEST_AUTH_TOKENS
        val userServerConfig = TEST_SERVER_CONFIG
        val expected = Pair(ssoId, authTokens)

        given(registerAccountRepository).coroutine {
            registerTeamWithEmail(
                param.email, param.emailActivationCode, param.name, param.password, param.teamName, param.teamIcon, param.cookieLabel
            )
        }.then { Either.Right(Pair(ssoId, authTokens)) }

        val actual = registerAccountUseCase(param)

        assertIs<RegisterResult.Success>(actual)
        assertEquals(expected.first, actual.ssoID)
        assertEquals(expected.second, actual.authData)
        assertEquals(userServerConfig.id, actual.serverConfigId)

        verify(registerAccountRepository).coroutine {
            registerTeamWithEmail(
                param.email, param.emailActivationCode, param.name, param.password, param.teamName, param.teamIcon, param.cookieLabel
            )
        }.wasInvoked(exactly = once)
    }

    @Test
    fun givenRepositoryCallFailWithGenericError_whenRegisteringPersonalAccount_thenErrorIsPropagated() = runTest {
        val param = TEST_PRIVATE_ACCOUNT_PARAM
        val expected = NetworkFailure.ServerMiscommunication(TestNetworkException.generic)

        given(registerAccountRepository).coroutine {
            registerPersonalAccountWithEmail(param.email, param.emailActivationCode, param.name, param.password, param.cookieLabel)
        }.then { Either.Left(expected) }

        val actual = registerAccountUseCase(param)

        assertIs<RegisterResult.Failure.Generic>(actual)
        assertIs<NetworkFailure.ServerMiscommunication>(actual.failure)
        assertEquals(expected.kaliumException, (actual.failure as NetworkFailure.ServerMiscommunication).kaliumException)

        verify(registerAccountRepository).coroutine {
            registerPersonalAccountWithEmail(
                param.email,
                param.emailActivationCode,
                param.name,
                param.password,
                cookieLabel = param.cookieLabel
            )
        }.wasInvoked(exactly = once)
    }

    @Test
    fun givenRepositoryCallFailWithInvalidEmail_whenRegisteringPersonalAccount_thenErrorIsPropagated() =
        testSpecificError(TestNetworkException.invalidEmail, RegisterResult.Failure.InvalidEmail)

    @Test
    fun givenRepositoryCallFailWithInvalidCode_whenRegisteringPersonalAccount_thenErrorIsPropagated() =
        testSpecificError(TestNetworkException.invalidCode, RegisterResult.Failure.InvalidActivationCode)

    @Test
    fun givenRepositoryCallFailWithKeyExists_whenRegisteringPersonalAccount_thenErrorIsPropagated() =
        testSpecificError(TestNetworkException.blackListedEmail, RegisterResult.Failure.BlackListed)

    @Test
    fun givenRepositoryCallFailWithUserCreationRestricted_whenRegisteringPersonalAccount_thenErrorIsPropagated() =
        testSpecificError(TestNetworkException.userCreationRestricted, RegisterResult.Failure.UserCreationRestricted)

    @Test
    fun givenRepositoryCallFailWithTooMAnyMembers_whenRegisteringPersonalAccount_thenErrorIsPropagated() =
        testSpecificError(TestNetworkException.tooManyTeamMembers, RegisterResult.Failure.TeamMembersLimitReached)

    @Test
    fun givenRepositoryCallFailWithDomainBlockedForRegistration_whenRegisteringPersonalAccount_thenErrorIsPropagated() =
        testSpecificError(TestNetworkException.domainBlockedForRegistration, RegisterResult.Failure.EmailDomainBlocked)

    private fun testSpecificError(kaliumException: KaliumException, error: RegisterResult.Failure) = runTest {
        val param = TEST_PRIVATE_ACCOUNT_PARAM
        val expected = NetworkFailure.ServerMiscommunication(kaliumException)

        given(registerAccountRepository).coroutine {
            registerPersonalAccountWithEmail(param.email, param.emailActivationCode, param.name, param.password, param.cookieLabel)
        }.then { Either.Left(expected) }

        val actual = registerAccountUseCase(param)

        assertIs<RegisterResult.Failure>(actual)
        assertEquals(error, actual)

        verify(registerAccountRepository).coroutine {
            registerPersonalAccountWithEmail(param.email, param.emailActivationCode, param.name, param.password, param.cookieLabel)
        }.wasInvoked(exactly = once)
    }

    private companion object {
        const val TEST_EMAIL = """user@domain.com"""
        const val TEST_CODE = "123456"
        const val TEST_PASSWORD = "password"
        val TEST_SERVER_CONFIG: ServerConfig = newServerConfig(1)
        val TEST_PRIVATE_ACCOUNT_PARAM = RegisterParam.PrivateAccount(
            firstName = "first",
            lastName = "last",
            email = TEST_EMAIL,
            password = TEST_PASSWORD,
            emailActivationCode = TEST_CODE,
            cookieLabel = "cookie_label"
        )
        val TEST_TEAM_ACCOUNT_PARAM = RegisterParam.Team(
            firstName = "first",
            lastName = "last",
            email = TEST_EMAIL,
            password = TEST_PASSWORD,
            emailActivationCode = TEST_CODE,
            teamName = "teamName",
            teamIcon = "teamIcon",
            cookieLabel = "cookie_label"
        )
        val TEST_SELF_USER = SelfUser(
            id = UserId(value = "user_id", domain = "domain.com"),
            name = TEST_PRIVATE_ACCOUNT_PARAM.name,
            handle = null,
            email = TEST_PRIVATE_ACCOUNT_PARAM.email,
            phone = null,
            accentId = 3,
            teamId = null,
            connectionStatus = ConnectionState.ACCEPTED,
            previewPicture = null,
            completePicture = null,
            availabilityStatus = UserAvailabilityStatus.NONE
        )
        val TEST_AUTH_TOKENS = AuthTokens(
            accessToken = "access_token",
            refreshToken = "refresh_token",
            tokenType = "token_type",
            userId = TEST_SELF_USER.id,
            cookieLabel = "cookie_label"
        )
        val TEST_SSO_ID = SsoId(null, null, null)
        val PROXY_CREDENTIALS = ProxyCredentials("user_name", "password")

    }
}
