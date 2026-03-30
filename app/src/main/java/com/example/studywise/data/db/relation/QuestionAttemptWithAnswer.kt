package com.example.studywise.data.db.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.studywise.data.db.entity.QuestionAttemptEntity
import com.example.studywise.data.db.entity.AnswerOptionEntity

// Relation: QuestionAttempt + selected AnswerOption
// selectedAnswerId can be null, so AnswerOptionEntity is nullable

data class QuestionAttemptWithAnswer(
    @Embedded val questionAttempt: QuestionAttemptEntity,
    @Relation(
        parentColumn = "selectedAnswerId",
        entityColumn = "id"
    )
    val selectedAnswer: AnswerOptionEntity?
)

