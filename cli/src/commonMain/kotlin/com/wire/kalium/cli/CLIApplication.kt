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

@file:Suppress("TooManyFunctions")
package com.wire.kalium.cli

import co.touchlab.kermit.LogWriter
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logic.CoreLogger
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import kotlinx.coroutines.runBlocking

class CLIApplication : CliktCommand(allowMultipleSubcommands = true) {

    private val logLevel by option(help = "log level").enum<KaliumLogLevel>().default(KaliumLogLevel.WARN)
    private val logOutputFile by option(help = "output file for logs")
    private val developmentApiEnabled by option(help = "use development API if supported by backend").flag(default = false)
    private val encryptProteusStorage by option(help = "use encrypted storage for proteus sessions and identity").flag(default = false)
    private val fileLogger: LogWriter by lazy { fileLogger(logOutputFile ?: "kalium.log") }

    override fun run() = runBlocking {
        currentContext.findOrSetObject {
            coreLogic(
                rootPath = "$HOME_DIRECTORY/.kalium/accounts",
                kaliumConfigs = KaliumConfigs(
                    developmentApiEnabled = developmentApiEnabled,
                    encryptProteusStorage = encryptProteusStorage
                )
            )
        }

        if (logOutputFile != null) {
            CoreLogger.setLoggingLevel(logLevel, fileLogger)
        } else {
            CoreLogger.setLoggingLevel(logLevel)
        }

        currentContext.findObject<CoreLogic>()?.updateApiVersionsScheduler?.scheduleImmediateApiVersionUpdate()
        Unit
    }

    companion object {
        val HOME_DIRECTORY: String = homeDirectory()
    }

}

expect fun fileLogger(filePath: String): LogWriter

expect fun homeDirectory(): String

expect fun coreLogic(rootPath: String, kaliumConfigs: KaliumConfigs): CoreLogic
