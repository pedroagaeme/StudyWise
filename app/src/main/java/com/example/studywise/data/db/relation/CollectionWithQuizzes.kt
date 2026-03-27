package com.example.studywise.data.db.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.studywise.data.db.entity.QuizEntity

// Holds a collection and its quizzes (as QuizBasicInfo)
data class CollectionWithQuizzes(
    @Embedded val collection: CollectionInfo,
    @Relation(
        parentColumn = "id",
        entityColumn = "quizCollectionId",
        entity = QuizBasicInfo::class,
        projection = ["id", "title", "lastAttemptedAt", "questionCount", "averageScore", "collectionName", "createdAt", "updatedAt"]
    )
    val quizzes: List<QuizBasicInfo>
)

data class CollectionInfo(
    val id: String,
    val name: String
)
