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

package com.wire.kalium.persistence.dao

import kotlinx.coroutines.flow.Flow

interface MetadataDAO {
    suspend fun insertValue(value: String, key: String)
    suspend fun deleteValue(key: String)
    suspend fun valueByKeyFlow(key: String): Flow<String?>
    suspend fun valueByKey(key: String): String?
    suspend fun clear(keysToKeep: List<String>?)
}
