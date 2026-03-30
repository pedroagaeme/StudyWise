package com.example.studywise.data.db.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.studywise.data.db.entity.QuizAttemptEntity
import com.example.studywise.data.db.entity.QuestionAttemptEntity
import com.example.studywise.data.db.entity.AnswerOptionEntity

data class QuizAttemptFullInfo(
    @Embedded val quizAttempt: QuizAttemptEntity,
    @Relation(
        entity = QuestionAttemptEntity::class,
        parentColumn = "id",
        entityColumn = "quizAttemptId"
    )
    val questionAttempts: List<QuestionAttemptWithAnswer>
) {
    val score: Int
        get() = questionAttempts.count { it.selectedAnswer?.isCorrect == true }
}
