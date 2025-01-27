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

package com.wire.kalium.model

import com.wire.kalium.api.json.ValidJsonProvider
import com.wire.kalium.network.api.base.authenticated.client.Capabilities
import com.wire.kalium.network.api.base.authenticated.client.ClientCapabilityDTO
import com.wire.kalium.network.api.base.authenticated.client.ClientResponse
import com.wire.kalium.network.api.base.authenticated.client.ClientTypeDTO
import com.wire.kalium.network.api.base.authenticated.client.DeviceTypeDTO
import com.wire.kalium.network.api.base.model.LocationResponse

object ClientResponseJson {
    private val jsonProvider = { serializable: ClientResponse ->
        """
        |{
        |   "id": "${serializable.clientId}",
        |   "type": "${serializable.type}",
        |   "time": "${serializable.registrationTime}",
        |   "class": "${serializable.deviceType}",
        |   "label": "${serializable.label}",
        |   "cookie": "${serializable.cookie}",
        |   "location": {
        |     "lat": ${serializable.location!!.latitude},
        |     "lon": ${serializable.location!!.longitude}
        |   },
        |   "model": "${serializable.model}",
        |   "capabilities": {
        |     "capabilities": [
        |        "${serializable.capabilities!!.capabilities[0]}"
        |     ]
        |  }
        |}
        """.trimMargin()
    }

    val valid = ValidJsonProvider(
        ClientResponse(
            clientId = "defkrr8e7grgsoufhg8",
            type = ClientTypeDTO.Permanent,
            deviceType = DeviceTypeDTO.Phone,
            registrationTime = "2021-05-12T10:52:02.671Z",
            location = LocationResponse(latitude = "1.2345", longitude = "6.7890"),
            label = "label",
            cookie = "sldkfmdeklmwldwlek23kl44mntiuepfojfndkjd",
            capabilities = Capabilities(listOf(ClientCapabilityDTO.LegalHoldImplicitConsent)),
            model = "model",
            mlsPublicKeys = null
        ),
        jsonProvider
    )

    fun createValid(clientResponse: ClientResponse) = ValidJsonProvider(clientResponse, jsonProvider)
}
