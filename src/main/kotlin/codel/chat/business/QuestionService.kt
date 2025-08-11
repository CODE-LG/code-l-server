package codel.chat.business

import codel.chat.domain.Chat
import codel.chat.domain.ChatContentType
import codel.chat.domain.ChatRoomQuestion
import codel.chat.exception.ChatException
import codel.chat.infrastructure.ChatJpaRepository
import codel.chat.infrastructure.ChatRoomJpaRepository
import codel.chat.infrastructure.ChatRoomQuestionJpaRepository
import codel.chat.infrastructure.ChatRoomMemberJpaRepository
import codel.chat.presentation.response.ChatResponse
import codel.chat.presentation.response.QuestionSendResult
import codel.chat.presentation.response.ChatRoomResponse
import codel.question.infrastructure.QuestionJpaRepository
import codel.member.domain.Member
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Transactional
@Service
class QuestionService(
    private val questionJpaRepository: QuestionJpaRepository,
    private val chatRoomJpaRepository: ChatRoomJpaRepository,
    private val chatRoomQuestionJpaRepository: ChatRoomQuestionJpaRepository,
    private val chatRoomMemberJpaRepository: ChatRoomMemberJpaRepository,
    private val chatJpaRepository: ChatJpaRepository
) {

    /**
     * 랜덤 질문을 채팅방에 즉시 전송
     * 버튼 클릭 -> 바로 질문 생성 & 전송
     */
    fun sendRandomQuestion(
        chatRoomId: Long,
        requester: Member
    ): QuestionSendResult {
        // 채팅방 및 권한 확인
        val chatRoom = chatRoomJpaRepository.findById(chatRoomId)
            .orElseThrow { ChatException(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다.") }
        
        val chatRoomMember = chatRoomMemberJpaRepository.findByChatRoomIdAndMember(chatRoomId, requester)
            ?: throw ChatException(HttpStatus.FORBIDDEN, "해당 채팅방에 접근할 권한이 없습니다.")

        // 사용하지 않은 활성 질문들 조회
        val unusedQuestions = questionJpaRepository.findUnusedQuestionsByChatRoom(chatRoomId)
        
        if (unusedQuestions.isEmpty()) {
            throw ChatException(HttpStatus.NO_CONTENT, "더 이상 사용할 수 있는 질문이 없습니다.")
        }

        // 랜덤하게 질문 선택
        val selectedQuestion = unusedQuestions.random()

        // 채팅방-질문 관계 저장 (요청자 정보 포함)
        val chatRoomQuestion = ChatRoomQuestion.create(
            chatRoom = chatRoom,
            question = selectedQuestion,
            requestedBy = requester
        )
        chatRoomQuestionJpaRepository.save(chatRoomQuestion)

        // 시스템 메시지로 질문 저장 (요청자 정보 포함)
        val systemMessage = Chat.createSystemMessage(
            chatRoom = chatRoom,
            message = "💭 ${selectedQuestion.content}\n\n_${requester.getProfileOrThrow().codeName}님이 질문을 추천했습니다._",
            chatContentType = ChatContentType.CODE_QUESTION
        )
        systemMessage.sentAt = LocalDateTime.now()
        
        val savedChat = chatJpaRepository.save(systemMessage)
        
        // 채팅방의 최근 메시지 업데이트
        chatRoom.updateRecentChat(savedChat)

        // 채팅방 멤버들 조회
        val chatRoomMembers = chatRoomMemberJpaRepository.findByChatRoomId(chatRoomId)
            .map { it.member }

        // 업데이트된 채팅방 정보 생성
        val partner = chatRoomMembers.first { it != requester }
        val updatedChatRoom = ChatRoomResponse.toResponse(
            chatRoom, 
            requester, 
            savedChat.getIdOrThrow(), 
            partner, 
            0 // 새 메시지이므로 읽지 않은 메시지 수는 0
        )

        return QuestionSendResult(
            chatResponse = ChatResponse.toResponse(requester, savedChat),
            chatRoomMember = partner,
            updatedChatRoom = updatedChatRoom
        )
    }
}
