package com.example.studywise.data.db.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.studywise.data.db.entity.AnswerOptionEntity
import com.example.studywise.data.db.entity.QuestionEntity

data class QuestionWithAnswers (
    @Embedded val question: QuestionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "questionId"
    )
    val answers: List<AnswerOptionEntity>
)