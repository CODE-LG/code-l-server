package codel.chat.infrastructure

import codel.chat.domain.ChatRoom
import codel.chat.domain.ChatRoomMember
import codel.member.domain.Member

data class ChatRoomWithMemberInfo(
    val chatRoom: ChatRoom,
    val requesterChatRoomMember: ChatRoomMember,
    val partner: Member,
    val partnerChatRoomMember: ChatRoomMember?
)