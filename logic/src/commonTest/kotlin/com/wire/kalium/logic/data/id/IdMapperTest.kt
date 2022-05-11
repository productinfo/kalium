package com.wire.kalium.logic.data.id

import com.wire.kalium.network.api.user.client.DeviceTypeDTO
import com.wire.kalium.network.api.user.client.SimpleClientResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import com.wire.kalium.protobuf.messages.QualifiedConversationId

class IdMapperTest {

    private val idMapper = IdMapperImpl()

    @Test
    fun givenAQualifiedId_whenMappingToApiModel_thenTheFieldsShouldBeMappedCorrectly() {
        val qualifiedID = QualifiedID("value", "domain")

        val networkID = idMapper.toApiModel(qualifiedID)

        assertEquals(qualifiedID.value, networkID.value)
        assertEquals(qualifiedID.domain, networkID.domain)
    }

    @Test
    fun givenAnAPIQualifiedId_whenMappingFromApiModel_thenTheFieldsShouldBeMappedCorrectly() {
        val networkID = NetworkQualifiedId("value", "domain")

        val qualifiedID = idMapper.fromApiModel(networkID)

        assertEquals(networkID.value, qualifiedID.value)
        assertEquals(networkID.domain, qualifiedID.domain)
    }

    @Test
    fun givenASimpleClientResponse_whenMappingFromSimpleClientResponse_thenTheIDShouldBeMappedCorrectly() {
        val simpleClientResponse = SimpleClientResponse("an ID", DeviceTypeDTO.Desktop)

        val clientID = idMapper.fromSimpleClientResponse(simpleClientResponse)

        assertEquals(simpleClientResponse.id, clientID.value)
    }

    @Test
    fun givenProtoQualifiedConversationId_whenMappingFromProtoModel_thenTheIDShouldBeMappedCorrectly() {
        val qualifiedConversationId = QualifiedConversationId("Test", "Test")

        val conversationId = idMapper.fromProtoModel(qualifiedConversationId)

        assertEquals(qualifiedConversationId.id, conversationId.value)
        assertEquals(qualifiedConversationId.domain, conversationId.domain)
    }

    @Test
    fun givenConversationId_whenMappingToProtoModel_thenTheIDShouldBeMappedCorrectly() {
        val conversationId = ConversationId("Test", "Test")

        val qualifiedConversationId = idMapper.toProtoModel(conversationId)

        assertEquals(qualifiedConversationId.id, conversationId.value)
        assertEquals(qualifiedConversationId.domain, conversationId.domain)
    }


}
