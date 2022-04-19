package com.wire.kalium.logic.data.conversation

import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.PlainId
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.publicuser.model.OtherUser
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.User
import com.wire.kalium.logic.data.user.UserId

data class Conversation(
    val id: ConversationId, val name: String?, val type: Type, val teamId: TeamId?, val mutedStatus: MutedConversationStatus
) {
    enum class Type { SELF, ONE_ON_ONE, GROUP }
}

sealed class ConversationDetails(open val conversation: Conversation) {

    data class Self(override val conversation: Conversation) : ConversationDetails(conversation)

    data class OneOne(
        override val conversation: Conversation,
        val otherUser: OtherUser,
        val connectionState: ConnectionState,
        val legalHoldStatus: LegalHoldStatus
    ) : ConversationDetails(conversation)

    data class Group(override val conversation: Conversation) : ConversationDetails(conversation)
}

class MembersInfo(val self: Member, val otherMembers: List<Member>)

class Member(override val id: UserId) : User()

sealed class MemberDetails {
    data class Self(val selfUser: SelfUser) : MemberDetails()
    data class Other(val otherUser: OtherUser) : MemberDetails()
}

typealias ClientId = PlainId

data class Recipient(val member: Member, val clients: List<ClientId>)
