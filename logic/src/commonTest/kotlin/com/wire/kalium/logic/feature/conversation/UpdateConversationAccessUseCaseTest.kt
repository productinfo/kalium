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

package com.wire.kalium.logic.feature.conversation

import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.Conversation.ProtocolInfo
import com.wire.kalium.logic.data.conversation.ConversationRepository
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.functional.Either
import io.mockative.Mock
import io.mockative.any
import io.mockative.given
import io.mockative.mock
import io.mockative.once
import io.mockative.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okio.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateConversationAccessUseCaseTest {

    @Test
    fun givenConversation_whenDisablingServices_thenUpdateAccessInfoIsCalledWithTheCorrectRoles() = runTest {
        val conversation = conversationStub.copy(
            accessRole = listOf(
                Conversation.AccessRole.TEAM_MEMBER,
                Conversation.AccessRole.NON_TEAM_MEMBER,
                Conversation.AccessRole.GUEST,
                Conversation.AccessRole.SERVICE
            ),
            access = listOf(Conversation.Access.INVITE, Conversation.Access.CODE),
            )

        val (arrangement, updateConversationAccess) = Arrangement()
            .withbaseInfoByIdReturning(Either.Right(conversation))
            .withUpdateAccessInfoRetuning(Either.Right(Unit))
            .arrange()

        updateConversationAccess(
            conversationId = conversation.id,
            accessRoles = Conversation
                .accessRolesFor(
                    guestAllowed = true,
                    servicesAllowed = false,
                    nonTeamMembersAllowed = true
                ),
            access = Conversation.accessFor(guestsAllowed = true)
        ).also { result ->
            assertIs<UpdateConversationAccessRoleUseCase.Result.Success>(result)
        }

        verify(arrangement.conversationRepository).coroutine {
            arrangement
                .conversationRepository
                .updateAccessInfo(
                    conversation.id,
                    access = setOf(Conversation.Access.INVITE, Conversation.Access.CODE),
                    accessRole = Conversation
                        .defaultGroupAccessRoles
                        .toMutableSet()
                        .apply { add(Conversation.AccessRole.GUEST) }
                )
        }.wasInvoked(exactly = once)
    }

    @Test
    fun givenConversation_whenEnablingServices_thenUpdateAccessInfoIsCalledWithTheCorrectRoles() = runTest {

        // Given a conversation where TEAM_MEMBER(s), NON_TEAM_MEMBER(s) and GUEST(s) but not SERVICE(s)
        val givenAccessRoles = Conversation
            .defaultGroupAccessRoles
            .toMutableList()
            .apply {
                add(Conversation.AccessRole.SERVICE)
            }

        // Given the access mode is CODE only
        val conversation = conversationStub.copy(
            accessRole = givenAccessRoles,
            access = Conversation.defaultGroupAccess.toMutableList().apply { add(Conversation.Access.INVITE) }
        )

        val (arrangement, updateConversationAccess) = Arrangement()
            .withbaseInfoByIdReturning(Either.Right(conversation))
            .withUpdateAccessInfoRetuning(Either.Right(Unit))
            .arrange()

        updateConversationAccess(
            conversationId = conversation.id,
            accessRoles = Conversation
                .accessRolesFor(
                    guestAllowed = true,
                    servicesAllowed = true,
                    nonTeamMembersAllowed = true
                ),
            access = Conversation.accessFor(guestsAllowed = true)
        ).also { result ->
            assertIs<UpdateConversationAccessRoleUseCase.Result.Success>(result)
        }

        verify(arrangement.conversationRepository).coroutine {
            arrangement
                .conversationRepository
                .updateAccessInfo(
                    conversationID = conversation.id,
                    accessRole = setOf(
                        Conversation.AccessRole.TEAM_MEMBER,
                        Conversation.AccessRole.NON_TEAM_MEMBER,
                        Conversation.AccessRole.SERVICE,
                        Conversation.AccessRole.GUEST
                    ),
                    access = setOf(Conversation.Access.CODE, Conversation.Access.INVITE)
                )
        }.wasInvoked(exactly = once)
    }

    @Test
    fun givenConversation_whenDisablingNonTeamMembers_thenUpdateAccessInfoIsCalledWithTheCorrectRoles() = runTest {
        val conversation = conversationStub.copy(
            accessRole = listOf(
                Conversation.AccessRole.TEAM_MEMBER,
                Conversation.AccessRole.NON_TEAM_MEMBER,
                Conversation.AccessRole.SERVICE,
                Conversation.AccessRole.GUEST
            )
        )

        val (arrangement, updateConversationAccess) = Arrangement()
            .withbaseInfoByIdReturning(Either.Right(conversation))
            .withUpdateAccessInfoRetuning(Either.Right(Unit))
            .arrange()

        updateConversationAccess(
            conversationId = conversation.id,
            accessRoles = Conversation
                .accessRolesFor(
                    guestAllowed = true,
                    servicesAllowed = true,
                    nonTeamMembersAllowed = false
                ),
            Conversation.accessFor(guestsAllowed = true)
        ).also { result ->
            assertIs<UpdateConversationAccessRoleUseCase.Result.Success>(result)
        }

        verify(arrangement.conversationRepository).coroutine {
            arrangement.conversationRepository.updateAccessInfo(
                conversationID = conversation.id,
                accessRole = setOf(
                    Conversation.AccessRole.TEAM_MEMBER,
                    Conversation.AccessRole.SERVICE,
                    Conversation.AccessRole.GUEST
                ),
                access = conversation.access.toSet()
            )
        }.wasInvoked(exactly = once)
    }

    @Test
    fun givenConversation_whenEnablingNonTeamMembers_thenUpdateAccessInfoIsCalledWithTheCorrectRoles() = runTest {
        val conversation = conversationStub.copy(
            accessRole = listOf(
                Conversation.AccessRole.TEAM_MEMBER, Conversation.AccessRole.SERVICE, Conversation.AccessRole.GUEST
            )
        )

        val (arrangement, updateConversationAccess) = Arrangement().withbaseInfoByIdReturning(Either.Right(conversation))
            .withUpdateAccessInfoRetuning(Either.Right(Unit)).arrange()

        updateConversationAccess(
            conversationId = conversation.id,
            accessRoles = Conversation
                .accessRolesFor(
                    guestAllowed = true,
                    servicesAllowed = true,
                    nonTeamMembersAllowed = true
                ),
            access = Conversation.accessFor(guestsAllowed = true)
        ).also { result ->
            assertIs<UpdateConversationAccessRoleUseCase.Result.Success>(result)
        }

        verify(arrangement.conversationRepository).coroutine {
            arrangement.conversationRepository.updateAccessInfo(
                conversationID = conversation.id,
                accessRole = setOf(
                    Conversation.AccessRole.TEAM_MEMBER,
                    Conversation.AccessRole.SERVICE,
                    Conversation.AccessRole.GUEST,
                    Conversation.AccessRole.NON_TEAM_MEMBER
                ),
                access = conversation.access.toSet()
            )
        }.wasInvoked(exactly = once)
    }

    @Test
    fun givenConversation_whenDisablingGuests_thenUpdateAccessInfoIsCalledWithTheCorrectRoles() = runTest {
        val conversation = conversationStub.copy(
            accessRole = listOf(
                Conversation.AccessRole.TEAM_MEMBER,
                Conversation.AccessRole.NON_TEAM_MEMBER,
                Conversation.AccessRole.SERVICE,
                Conversation.AccessRole.GUEST
            ),
            access = listOf(Conversation.Access.INVITE)
        )

        val (arrangement, updateConversationAccess) = Arrangement()
            .withbaseInfoByIdReturning(Either.Right(conversation))
            .withUpdateAccessInfoRetuning(Either.Right(Unit))
            .arrange()

        updateConversationAccess(
            conversationId = conversation.id,
            accessRoles = Conversation
                .accessRolesFor(
                    guestAllowed = false,
                    servicesAllowed = true,
                    nonTeamMembersAllowed = true
                ),
            access = Conversation.accessFor(guestsAllowed = false)
        ).also { result ->
            assertIs<UpdateConversationAccessRoleUseCase.Result.Success>(result)
        }

        verify(arrangement.conversationRepository).coroutine {
            arrangement.conversationRepository.updateAccessInfo(
                conversationID = conversation.id,
                accessRole = setOf(
                    Conversation.AccessRole.TEAM_MEMBER,
                    Conversation.AccessRole.NON_TEAM_MEMBER,
                    Conversation.AccessRole.SERVICE
                ),
                access = Conversation.defaultGroupAccess
            )
        }.wasInvoked(exactly = once)
    }

    @Test
    fun givenConversation_whenEnablingGuests_thenUpdateAccessInfoIsCalledWithTheCorrectRoles() = runTest {

        // Given a conversation where TEAM_MEMBER(s), NON_TEAM_MEMBER(s) and SERVICE(s) have access and GUEST(s) don't
        val givenAccessRoles = Conversation
            .defaultGroupAccessRoles
            .toMutableList()
            .apply {
                add(Conversation.AccessRole.SERVICE)
            }

        // Given the access mode is CODE only
        val conversation = conversationStub.copy(
            accessRole = givenAccessRoles,
            access = Conversation.defaultGroupAccess.toList()
        )

        // Given
        val (arrangement, updateConversationAccess) = Arrangement()
            .withbaseInfoByIdReturning(Either.Right(conversation))
            .withUpdateAccessInfoRetuning(Either.Right(Unit))
            .arrange()

        // When Guests are allowed
        updateConversationAccess(
            conversationId = conversation.id,
            accessRoles = Conversation.accessRolesFor(guestAllowed = true, servicesAllowed = true, nonTeamMembersAllowed = true),
            access = Conversation.accessFor(guestsAllowed = true)
        ).also { result ->
            assertIs<UpdateConversationAccessRoleUseCase.Result.Success>(result)
        }

        // Then
        verify(arrangement.conversationRepository).coroutine {
            arrangement.conversationRepository.updateAccessInfo(
                conversationID = conversation.id,
                accessRole = Conversation.defaultGroupAccessRoles.toMutableSet().apply {
                    add(Conversation.AccessRole.GUEST)
                    add(Conversation.AccessRole.SERVICE)
                },
                access = setOf(Conversation.Access.INVITE, Conversation.Access.CODE)
            )
        }.wasInvoked(exactly = once)
    }

    @Test
    fun givenError_whenCallingUpdateAccessInfo_thenFailureIsPropagated() = runTest {
        val conversation = conversationStub

        val (arrangement, updateConversationAccess) = Arrangement().withbaseInfoByIdReturning(Either.Right(conversation))
            .withUpdateAccessInfoRetuning(Either.Left(NetworkFailure.NoNetworkConnection(IOException()))).arrange()

        // allowGuest = false, allowServices = true, allowNonTeamMember = true
        updateConversationAccess(
            conversationId = conversation.id,
            accessRoles = Conversation.accessRolesFor(guestAllowed = false, servicesAllowed = true, nonTeamMembersAllowed = true),
            access = Conversation.accessFor(guestsAllowed = false)
        ).also { result ->
            assertIs<UpdateConversationAccessRoleUseCase.Result.Failure>(result)
            assertIs<NetworkFailure.NoNetworkConnection>(result.cause)
        }

        verify(arrangement.conversationRepository).suspendFunction(arrangement.conversationRepository::updateAccessInfo)
            .with(any(), any(), any()).wasInvoked(exactly = once)
    }

    companion object {
        val conversationStub = Conversation(
            ConversationId(value = "someId", domain = "someDomain"),
            "GROUP Conversation",
            Conversation.Type.GROUP,
            TeamId("someTeam"),
            ProtocolInfo.Proteus,
            MutedConversationStatus.AllAllowed,
            null,
            null,
            null,
            access = listOf(
                Conversation.Access.CODE,
                Conversation.Access.INVITE
            ),
            accessRole = listOf(
                Conversation.AccessRole.NON_TEAM_MEMBER,
                Conversation.AccessRole.TEAM_MEMBER,
                Conversation.AccessRole.GUEST
            ),
            lastReadDate = "2022.01.02",
            creatorId = "someCreatorId",
            receiptMode = Conversation.ReceiptMode.DISABLED
        )
    }

    private class Arrangement {
        @Mock
        val conversationRepository = mock(ConversationRepository::class)

        val updateConversationAccess: UpdateConversationAccessRoleUseCase = UpdateConversationAccessRoleUseCase(conversationRepository)

        fun withbaseInfoByIdReturning(either: Either<StorageFailure, Conversation>) = apply {
            given(conversationRepository).suspendFunction(conversationRepository::baseInfoById).whenInvokedWith(any()).thenReturn(either)
        }

        fun withUpdateAccessInfoRetuning(either: Either<CoreFailure, Unit>) = apply {
            given(conversationRepository)
                .suspendFunction(conversationRepository::updateAccessInfo)
                .whenInvokedWith(any(), any(), any())
                .thenReturn(either)
        }

        fun arrange() = this to updateConversationAccess
    }
}
