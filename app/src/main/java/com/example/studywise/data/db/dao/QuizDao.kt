package com.example.studywise.data.db.dao

import androidx.room.*
import com.example.studywise.data.db.entity.AnswerOptionEntity
import com.example.studywise.data.db.entity.QuestionEntity
import com.example.studywise.data.db.entity.QuizCollectionEntity
import com.example.studywise.data.db.entity.QuizEntity
import com.example.studywise.data.db.relation.CollectionWithQuizzes
import com.example.studywise.data.db.relation.QuizBasicInfo
import com.example.studywise.data.db.relation.QuizWithQuestions
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(quizCollection: QuizCollectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(quiz: QuizEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(question: QuestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(answerOptions: List<AnswerOptionEntity>)
    @Transaction
    suspend fun insertQuizWithQuestionsAndAnswers(
        quizCollection: QuizCollectionEntity,
        quiz: QuizEntity,
        questions: List<Pair<QuestionEntity, List<AnswerOptionEntity>>>
    ) {
        insert(quizCollection)
        insert(quiz)

        questions.forEach { (questionEntity, answerOptions) ->
            insert(questionEntity)
            insertAll(answerOptions)
        }
    }

    @Delete
    suspend fun delete(quiz: QuizEntity)

    @Transaction
    @Query("SELECT * FROM quiz WHERE id = :id")
    suspend fun getQuizWithQuestionsById(id: String): QuizWithQuestions?

    @Query("SELECT QC.* FROM quiz_collection QC JOIN quiz Q ON QC.id = Q.quizCollectionId WHERE Q.id = :quizId")
    suspend fun getQuizCollectionByQuizId(quizId: String): QuizCollectionEntity?

    @Query("SELECT * FROM QuizBasicInfoView ORDER BY lastAttemptedAt DESC LIMIT :limit")
    fun getMostRecentQuizzes(limit: Int): Flow<List<QuizBasicInfo>>


    @Transaction
    @Query("""
        SELECT QC.id, QC.name
        FROM quiz_collection QC
    """)
    fun getCollectionsWithQuizzes(): Flow<List<CollectionWithQuizzes>>
}