package com.example.studywise.data.db.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.studywise.data.db.entity.QuizEntity
import com.example.studywise.data.db.entity.QuestionEntity
import com.example.studywise.data.db.relation.QuestionWithAnswers

// A quiz with all its questions (each with their answers)
data class QuizWithQuestions(
    @Embedded val quiz: QuizEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "quizId",
        entity = QuestionEntity::class,
    )
    val questions: List<QuestionWithAnswers>,
)
