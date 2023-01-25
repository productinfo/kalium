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

import com.wire.kalium.plugins.commonDokkaConfig
import com.wire.kalium.plugins.commonJvmConfig

@Suppress("DSL_SCOPE_VIOLATION")

plugins {
    application
    kotlin("multiplatform")
}
val mainFunctionClassName = "com.wire.kalium.cli.MainKt"

application {
    mainClass.set(mainFunctionClassName)
}

kotlin {
    jvm() {
        commonJvmConfig(includeNativeInterop = false)
        tasks.named("run", JavaExec::class) {
            isIgnoreExitValue = true
            standardInput = System.`in`
            standardOutput = System.out
        }
    }
    macosX64() {
        binaries {
            executable()
        }
    }
    macosArm64() {
        binaries {
            executable()
        }
    }

    sourceSets {
        val commonMain by sourceSets.getting {
            dependencies {
                implementation(project(":network"))
                implementation(project(":cryptography"))
                implementation(project(":logic"))
                implementation(project(":util"))

                implementation(libs.cliKt)
                implementation(libs.ktor.utils)
                implementation(libs.coroutines.core)
                implementation(libs.ktxDateTime)
            }
        }
        val jvmMain by getting {
            dependsOn(commonMain)

             dependencies {
                 implementation(libs.ktor.okHttp)
                 implementation(libs.okhttp.loggingInterceptor)
             }
        }
        val darwinMain by creating {
            dependsOn(commonMain)

            dependencies {
                implementation(libs.ktor.iosHttp)
            }
        }
        val macosX64Main by getting {
            dependsOn(darwinMain)
        }
        val macosArm64Main by getting {
            dependsOn(darwinMain)
        }
    }
}

commonDokkaConfig()

tasks.withType<Wrapper> {
    gradleVersion = "7.3.1"
    distributionType = Wrapper.DistributionType.BIN
}
