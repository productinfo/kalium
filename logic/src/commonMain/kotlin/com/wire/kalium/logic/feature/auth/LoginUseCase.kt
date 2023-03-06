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

package com.wire.kalium.logic.feature.auth

import com.benasher44.uuid.uuid4
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.auth.login.LoginRepository
import com.wire.kalium.logic.data.auth.login.ProxyCredentials
import com.wire.kalium.logic.data.user.SsoId
import com.wire.kalium.logic.functional.fold
import com.wire.kalium.logic.functional.map
import com.wire.kalium.network.exceptions.AuthenticationCodeFailure
import com.wire.kalium.network.exceptions.KaliumException
import com.wire.kalium.network.exceptions.authenticationCodeFailure
import com.wire.kalium.network.exceptions.isBadRequest
import com.wire.kalium.network.exceptions.isInvalidCredentials

sealed class AuthenticationResult {
    data class Success(
        val authData: AuthTokens,
        val ssoID: SsoId?,
        val serverConfigId: String,
        val proxyCredentials: ProxyCredentials?
    ) : AuthenticationResult()

    sealed class Failure : AuthenticationResult() {
        object SocketError : Failure()
        sealed class InvalidCredentials : Failure() {
            /**
             * The team has enabled 2FA but the user has not entered it yet
             */
            object Missing2FA : InvalidCredentials()

            /**
             * The user has entered an invalid 2FA code, or the 2FA code has expired
             */
            object Invalid2FA : InvalidCredentials()

            /**
             * The user has entered an invalid email/handle or password combination
             */
            object InvalidPasswordIdentityCombination : InvalidCredentials()
        }

        /**
         * The user has entered a text that isn't considered a valid email or handle
         */
        object InvalidUserIdentifier : Failure()
        class Generic(val genericFailure: CoreFailure) : Failure()
    }
}

interface LoginUseCase {
    /**
     * Login with user credentials and return the session
     * Be noticed that session won't be stored locally, to store it
     * @see AddAuthenticatedUserUseCase
     */
    suspend operator fun invoke(
        userIdentifier: String,
        password: String,
        shouldPersistClient: Boolean,
        cookieLabel: String? = uuid4().toString()
    ): AuthenticationResult
}

internal class LoginUseCaseImpl internal constructor(
    private val loginRepository: LoginRepository,
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val validateUserHandleUseCase: ValidateUserHandleUseCase,
    private val serverConfig: ServerConfig,
    private val proxyCredentials: ProxyCredentials?
) : LoginUseCase {
    override suspend operator fun invoke(
        userIdentifier: String,
        password: String,
        shouldPersistClient: Boolean,
        cookieLabel: String?
    ): AuthenticationResult {
        // remove White Spaces around userIdentifier
        val cleanUserIdentifier = userIdentifier.trim()

        return when {
            validateEmailUseCase(cleanUserIdentifier) -> {
                loginRepository.loginWithEmail(cleanUserIdentifier, password, cookieLabel, shouldPersistClient)
            }

            validateUserHandleUseCase(cleanUserIdentifier).isValidAllowingDots -> {
                loginRepository.loginWithHandle(cleanUserIdentifier, password, cookieLabel, shouldPersistClient)
            }

            else -> return AuthenticationResult.Failure.InvalidUserIdentifier
        }.map { (authTokens, ssoId) -> AuthenticationResult.Success(authTokens, ssoId, serverConfig.id, proxyCredentials) }
            .fold({
                when (it) {
                    is NetworkFailure.ProxyError -> AuthenticationResult.Failure.SocketError
                    is NetworkFailure.ServerMiscommunication -> handleServerMiscommunication(it)
                    is NetworkFailure.NoNetworkConnection, NetworkFailure.FederatedBackendFailure ->
                        AuthenticationResult.Failure.Generic(it)
                }
            }, {
                it
            })
    }

    private fun handleServerMiscommunication(error: NetworkFailure.ServerMiscommunication): AuthenticationResult.Failure {
        fun genericError() = AuthenticationResult.Failure.Generic(error)

        val kaliumException = error.kaliumException

        return when {
            kaliumException !is KaliumException.InvalidRequestError -> genericError()
            kaliumException.isInvalidCredentials() || kaliumException.isBadRequest() -> {
                AuthenticationResult.Failure.InvalidCredentials.InvalidPasswordIdentityCombination
            }

            else -> when (kaliumException.authenticationCodeFailure) {
                AuthenticationCodeFailure.MISSING_AUTHENTICATION_CODE ->
                    AuthenticationResult.Failure.InvalidCredentials.Missing2FA

                AuthenticationCodeFailure.INVALID_OR_EXPIRED_AUTHENTICATION_CODE ->
                    AuthenticationResult.Failure.InvalidCredentials.Invalid2FA

                else -> genericError()
            }
        }
    }
}
