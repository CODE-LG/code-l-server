package codel.question.presentation

import codel.question.presentation.response.QuestionResponse
import codel.config.argumentresolver.LoginMember
import codel.member.domain.Member
import codel.question.business.QuestionService
import codel.question.presentation.swagger.QuestionControllerSwagger
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/questions")
class QuestionController(
    val questionService : QuestionService
) : QuestionControllerSwagger {

    @GetMapping
    override fun findActiveQuestion(
        @LoginMember member : Member
    ) : ResponseEntity<List<QuestionResponse>>{
        val findActiveQuestions = questionService.findActiveQuestions()
        return ResponseEntity.ok(findActiveQuestions.map { question -> QuestionResponse.from(question) })
    }
}