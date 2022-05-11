package com.wire.kalium.logic.feature.auth.sso

import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.configuration.ServerConfig
import com.wire.kalium.logic.data.auth.login.SSOLoginRepository
import com.wire.kalium.logic.functional.fold
import com.wire.kalium.network.api.user.login.SSOSettingsResponse

sealed class SSOSettingsResult {
    data class Success(val ssoSettings: SSOSettingsResponse) : SSOSettingsResult()

    sealed class Failure : SSOSettingsResult() {
        class Generic(val genericFailure: CoreFailure) : Failure()
    }
}

interface SSOSettingsUseCase {
    suspend operator fun invoke(serverConfig: ServerConfig): SSOSettingsResult
}

internal class SSOSettingsUseCaseImpl(
    private val ssoLoginRepository: SSOLoginRepository
) : SSOSettingsUseCase {

    override suspend fun invoke(serverConfig: ServerConfig): SSOSettingsResult =
        ssoLoginRepository.settings(serverConfig).fold({ SSOSettingsResult.Failure.Generic(it) }, { SSOSettingsResult.Success(it) })
}
