package com.example.studywise.data.db.relation

import androidx.room.DatabaseView
import androidx.room.Embedded
import com.example.studywise.data.db.entity.QuizEntity

@DatabaseView(
    viewName = "QuizBasicInfoView",
    value = """
        SELECT Q.*,
        COALESCE(QA.lastAttemptedAt, Q.createdAt) as lastAttemptedAt,
        (SELECT COUNT(*) FROM question WHERE quizId = Q.id) as questionCount,
        (SELECT AVG(score) FROM quiz_attempt WHERE quizId = Q.id) as averageScore,
        QC.name as collectionName
        FROM quiz Q
        LEFT JOIN (
        SELECT quizId, MAX(createdAt) as lastAttemptedAt
        FROM quiz_attempt
        GROUP BY quizId
        ) QA ON Q.id = QA.quizId
        LEFT JOIN quiz_collection QC ON Q.quizCollectionId = QC.id
    """
)
data class QuizBasicInfo(
    @Embedded val quiz: QuizEntity,
    val lastAttemptedAt: String?,
    val questionCount: Int,
    val averageScore: Float?,
    val collectionName: String?
)