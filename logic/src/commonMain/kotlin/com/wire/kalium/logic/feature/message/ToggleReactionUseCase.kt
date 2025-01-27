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

package com.wire.kalium.logic.feature.message

import com.benasher44.uuid.uuid4
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.message.UserReactions
import com.wire.kalium.logic.data.message.reaction.ReactionRepository
import com.wire.kalium.logic.data.sync.SlowSyncRepository
import com.wire.kalium.logic.data.sync.SlowSyncStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.CurrentClientIdProvider
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.flatMap
import com.wire.kalium.logic.functional.flatMapLeft
import com.wire.kalium.logic.functional.map
import com.wire.kalium.util.DateTimeUtil
import kotlinx.coroutines.flow.first

/**
 * Toggles a reaction on a message.
 * If the reaction already exists it will be removed, if not it will be added.
 */
class ToggleReactionUseCase internal constructor(
    private val currentClientIdProvider: CurrentClientIdProvider,
    private val userId: UserId,
    private val slowSyncRepository: SlowSyncRepository,
    private val reactionRepository: ReactionRepository,
    private val messageSender: MessageSender
) {
    /**
     * Operation to toggle a reaction on a message
     *
     * @param conversationId the id of the conversation the message is in
     * @param messageId the id of the message to toggle the reaction on/off
     * @param reaction the reaction "emoji" to toggle
     * @return [Either] [CoreFailure] or [Unit] //fixme: we should not return [Either]
     */
    suspend operator fun invoke(
        conversationId: ConversationId,
        messageId: String,
        reaction: String
    ): Either<CoreFailure, Unit> {
        slowSyncRepository.slowSyncStatus.first {
            it is SlowSyncStatus.Complete
        }
        val date = DateTimeUtil.currentIsoDateTimeString()

        return reactionRepository.getSelfUserReactionsForMessage(messageId, conversationId)
            .flatMap { reactions ->
                currentClientIdProvider().map { it to reactions }
            }
            .flatMap { (currentClientId, currentReactions) ->
                if (currentReactions.contains(reaction)) {
                    // Remove reaction
                    removeReaction(
                        currentClientId,
                        conversationId,
                        date,
                        messageId,
                        reaction,
                        currentReactions
                    )
                } else {
                    // Add reaction
                    addReaction(
                        currentClientId,
                        conversationId,
                        date,
                        messageId,
                        currentReactions,
                        reaction
                    )
                }
            }
    }

    private suspend fun addReaction(
        clientId: ClientId,
        conversationId: ConversationId,
        date: String,
        messageId: String,
        currentReactions: UserReactions,
        newReaction: String
    ): Either<CoreFailure, Unit> {

        return reactionRepository
            .persistReaction(messageId, conversationId, userId, date, newReaction).flatMap {
                val regularMessage = Message.Signaling(
                    id = uuid4().toString(),
                    content = MessageContent.Reaction(messageId = messageId, emojiSet = currentReactions + newReaction),
                    conversationId = conversationId,
                    date = date,
                    senderUserId = userId,
                    senderClientId = clientId,
                    status = Message.Status.PENDING,
                    isSelfMessage = true
                )
                messageSender.sendMessage(regularMessage)
            }
            .flatMapLeft {
                reactionRepository.deleteReaction(messageId, conversationId, userId, newReaction)
            }
    }

    private suspend fun removeReaction(
        clientId: ClientId,
        conversationId: ConversationId,
        date: String,
        messageId: String,
        removedReaction: String,
        currentReactions: UserReactions
    ): Either<CoreFailure, Unit> {
        return reactionRepository.deleteReaction(messageId, conversationId, userId, removedReaction)
            .flatMap {
                val regularMessage = Message.Signaling(
                    id = uuid4().toString(),
                    content = MessageContent.Reaction(messageId = messageId, emojiSet = currentReactions - removedReaction),
                    conversationId = conversationId,
                    date = date,
                    senderUserId = userId,
                    senderClientId = clientId,
                    status = Message.Status.PENDING,
                    isSelfMessage = true
                )
                messageSender.sendMessage(regularMessage)
            }.flatMapLeft {
                reactionRepository.persistReaction(messageId, conversationId, userId, date, removedReaction)
            }
    }
}
