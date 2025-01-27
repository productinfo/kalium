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

package com.wire.kalium.logic.sync.receiver.conversation

import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.data.conversation.ConversationRepository
import com.wire.kalium.logic.data.event.Event
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.id.toModel
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.message.PersistMessageUseCase
import com.wire.kalium.logic.data.user.UserRepository
import com.wire.kalium.logic.feature.SelfTeamIdProvider
import com.wire.kalium.logic.feature.user.IsSelfATeamMemberUseCaseImpl
import com.wire.kalium.logic.framework.TestConversation
import com.wire.kalium.logic.framework.TestTeam
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.test_util.wasInTheLastSecond
import com.wire.kalium.network.api.base.authenticated.conversation.ReceiptMode
import io.mockative.Mock
import io.mockative.any
import io.mockative.classOf
import io.mockative.eq
import io.mockative.given
import io.mockative.matching
import io.mockative.mock
import io.mockative.once
import io.mockative.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NewConversationEventHandlerTest {

    @Test
    fun givenNewConversationOriginatedFromEvent_whenHandlingIt_thenPersistConversationShouldBeCalled() = runTest {
        val event = Event.Conversation.NewConversation(
            id = "eventId",
            conversationId = TestConversation.ID,
            transient = false,
            timestampIso = "timestamp",
            conversation = TestConversation.CONVERSATION_RESPONSE,
        )
        val members = event.conversation.members.otherMembers.map { it.id.toModel() }.toSet()
        val teamIdValue = "teamId"
        val teamId = TeamId(teamIdValue)
        val creatorQualifiedId = QualifiedID(
            value = "creator",
            domain = ""
        )

        val (arrangement, eventHandler) = Arrangement()
            .withUpdateConversationModifiedDateReturning(Either.Right(Unit))
            .withPersistingConversations(Either.Right(Unit))
            .withFetchUsersIfUnknownIds(members)
            .withSelfUserTeamId(Either.Right(teamId))
            .withPersistSystemMessage()
            .withQualifiedId(creatorQualifiedId)
            .arrange()

        eventHandler.handle(event)

        verify(arrangement.conversationRepository)
            .suspendFunction(arrangement.conversationRepository::persistConversations)
            .with(eq(listOf(event.conversation)), eq(teamIdValue), eq(true))
            .wasInvoked(exactly = once)

        verify(arrangement.userRepository)
            .suspendFunction(arrangement.userRepository::fetchUsersIfUnknownByIds)
            .with(eq(members))
            .wasInvoked(exactly = once)
    }

    @Test
    fun givenNewConversationEvent_whenHandlingIt_thenConversationLastModifiedShouldBeUpdated() = runTest {
        val event = Event.Conversation.NewConversation(
            id = "eventId",
            conversationId = TestConversation.ID,
            false,
            timestampIso = "timestamp",
            conversation = TestConversation.CONVERSATION_RESPONSE
        )

        val members = event.conversation.members.otherMembers.map { it.id.toModel() }.toSet()
        val teamId = TestTeam.TEAM_ID
        val creatorQualifiedId = QualifiedID(
            value = "creator",
            domain = ""
        )

        val (arrangement, eventHandler) = Arrangement()
            .withUpdateConversationModifiedDateReturning(Either.Right(Unit))
            .withPersistingConversations(Either.Right(Unit))
            .withFetchUsersIfUnknownIds(members)
            .withSelfUserTeamId(Either.Right(teamId))
            .withPersistSystemMessage()
            .withQualifiedId(creatorQualifiedId)
            .arrange()

        eventHandler.handle(event)

        verify(arrangement.conversationRepository)
            .suspendFunction(arrangement.conversationRepository::updateConversationModifiedDate)
            .with(eq(event.conversationId), matching { it.wasInTheLastSecond })
            .wasInvoked(exactly = once)
    }

    @Test
    fun givenNewGroupConversationEvent_whenHandlingIt_thenPersistSystemMessageForReceiptMode() = runTest {
        // given
        val event = Event.Conversation.NewConversation(
            id = "eventId",
            conversationId = TestConversation.ID,
            transient = false,
            timestampIso = "timestamp",
            conversation = TestConversation.CONVERSATION_RESPONSE.copy(
                creator = "creatorId@creatorDomain",
                receiptMode = ReceiptMode.ENABLED
            )
        )

        val members = event.conversation.members.otherMembers.map { it.id.toModel() }.toSet()
        val teamId = TestTeam.TEAM_ID
        val creatorQualifiedId = QualifiedID(
            value = "creatorId",
            domain = "creatorDomain"
        )

        val (arrangement, eventHandler) = Arrangement()
            .withUpdateConversationModifiedDateReturning(Either.Right(Unit))
            .withPersistingConversations(Either.Right(Unit))
            .withFetchUsersIfUnknownIds(members)
            .withSelfUserTeamId(Either.Right(teamId))
            .withPersistSystemMessage()
            .withQualifiedId(creatorQualifiedId)
            .arrange()

        // when
        eventHandler.handle(event)

        // then
        verify(arrangement.persistMessage)
            .suspendFunction(arrangement.persistMessage::invoke)
            .with(matching {
                val content = it.content as MessageContent.NewConversationReceiptMode
                content.receiptMode
            })
            .wasInvoked(exactly = once)
    }

    private class Arrangement {
        @Mock
        val conversationRepository = mock(classOf<ConversationRepository>())

        @Mock
        val userRepository = mock(classOf<UserRepository>())

        @Mock
        val selfTeamIdProvider = mock(classOf<SelfTeamIdProvider>())

        @Mock
        val persistMessage = mock(classOf<PersistMessageUseCase>())

        @Mock
        private val qualifiedIdMapper = mock(classOf<QualifiedIdMapper>())

        private val isSelfATeamMember: IsSelfATeamMemberUseCaseImpl = IsSelfATeamMemberUseCaseImpl(selfTeamIdProvider)

        private val newConversationEventHandler: NewConversationEventHandler = NewConversationEventHandlerImpl(
            conversationRepository,
            userRepository,
            selfTeamIdProvider,
            persistMessage,
            qualifiedIdMapper,
            isSelfATeamMember
        )

        fun withUpdateConversationModifiedDateReturning(result: Either<StorageFailure, Unit>) = apply {
            given(conversationRepository)
                .suspendFunction(conversationRepository::updateConversationModifiedDate)
                .whenInvokedWith(any(), any())
                .thenReturn(result)
        }

        fun withPersistingConversations(result: Either<StorageFailure, Unit>) = apply {
            given(conversationRepository)
                .suspendFunction(conversationRepository::persistConversations)
                .whenInvokedWith(any(), any(), any())
                .thenReturn(result)
        }

        suspend fun withFetchUsersIfUnknownIds(members: Set<QualifiedID>) = apply {
            given(userRepository)
                .suspendFunction(userRepository::fetchUsersIfUnknownByIds)
                .whenInvokedWith(eq(members))
                .thenReturn(Either.Right(Unit))
        }

        fun withSelfUserTeamId(either: Either<CoreFailure, TeamId?>) = apply {
            given(selfTeamIdProvider)
                .suspendFunction(selfTeamIdProvider::invoke)
                .whenInvoked()
                .then { either }
        }

        fun withPersistSystemMessage() = apply {
            given(persistMessage)
                .suspendFunction(persistMessage::invoke)
                .whenInvokedWith(any())
                .thenReturn(Either.Right(Unit))
        }

        fun withQualifiedId(qualifiedId: QualifiedID) = apply {
            given(qualifiedIdMapper)
                .function(qualifiedIdMapper::fromStringToQualifiedID)
                .whenInvokedWith(any())
                .thenReturn(qualifiedId)
        }

        fun arrange() = this to newConversationEventHandler
    }

}
