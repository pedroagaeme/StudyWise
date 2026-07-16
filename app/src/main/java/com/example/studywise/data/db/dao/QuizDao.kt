package com.example.studywise.data.db.dao

import androidx.room.*
import com.example.studywise.data.db.entity.AnswerOptionEntity
import com.example.studywise.data.db.entity.QuestionAttemptEntity
import com.example.studywise.data.db.entity.QuestionEntity
import com.example.studywise.data.db.entity.QuizAttemptEntity
import com.example.studywise.data.db.entity.QuizCollectionEntity
import com.example.studywise.data.db.entity.QuizEntity
import com.example.studywise.data.db.relation.CollectionWithQuizzes
import com.example.studywise.data.db.relation.QuizAttemptFullInfo
import com.example.studywise.data.db.relation.QuizBasicInfo
import com.example.studywise.data.db.relation.QuizWithQuestions
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface QuizDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(quizCollection: QuizCollectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(quiz: QuizEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(question: QuestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(quizAttempt: QuizAttemptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(questionAttempt: QuestionAttemptEntity)

    @Update
    suspend fun update(questionAttempt: QuestionAttemptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(answerOptions: List<AnswerOptionEntity>)
    @Transaction
    suspend fun insertQuizWithQuestionsAndAnswers(
        quizCollection: QuizCollectionEntity,
        quiz: QuizEntity,
        questions: List<Pair<QuestionEntity, List<AnswerOptionEntity>>>
    ): String? {
        insert(quizCollection)
        insert(quiz)

        questions.forEach { (questionEntity, answerOptions) ->
            insert(questionEntity)
            insertAll(answerOptions)
        }

        return quiz.id
    }
    @Transaction
    suspend fun populateQuizAttempt(
        quizAttempt: QuizAttemptEntity,
        questionAttempts: List<QuestionAttemptEntity>
    ) {
        insert(quizAttempt)
        questionAttempts.forEachIndexed { index, questionAttempt ->
            insert(
                questionAttempt
            )
        }
    }

    @Transaction
    suspend fun createIfDoesNotExistCollection(
        collectionName: String,
        generateNewId: () -> String,
        now: () -> String
    ): QuizCollectionEntity {
        val existingCollection = getCollectionByName(collectionName)
        if (existingCollection != null) {
            return existingCollection
        }
        val newCollection = QuizCollectionEntity(
            id = generateNewId(),
            name = collectionName,
            createdAt = now(),
            updatedAt = now()
        )
        insert(newCollection)
        return newCollection

    }

    @Delete
    suspend fun delete(quiz: QuizEntity)

    @Transaction
    @Query("SELECT * FROM quiz WHERE id = :id")
    suspend fun getQuizWithQuestionsById(id: String): QuizWithQuestions?

    @Query("SELECT QC.* FROM quiz_collection QC JOIN quiz Q ON QC.id = Q.quizCollectionId WHERE Q.id = :quizId")
    suspend fun getQuizCollectionByQuizId(quizId: String): QuizCollectionEntity?

    @Query("SELECT * FROM quiz_collection WHERE name = :name LIMIT 1")
    suspend fun getCollectionByName(name: String): QuizCollectionEntity?

    @Query("SELECT * FROM QuizBasicInfoView ORDER BY lastAttemptedAt DESC LIMIT :limit")
    fun getMostRecentQuizzes(limit: Int): Flow<List<QuizBasicInfo>>

    @Query("SELECT * FROM QuizBasicInfoView WHERE id = :quizId LIMIT 1")
    fun getQuizById(quizId: String): Flow<QuizBasicInfo?>

    @Transaction
    @Query("""
        SELECT QC.id, QC.name
        FROM quiz_collection QC
    """)
    fun getCollectionsWithQuizzes(): Flow<List<CollectionWithQuizzes>>

    @Query("SELECT * FROM quiz_collection")
    fun getCollections(): Flow<List<QuizCollectionEntity>>

    @Query("SELECT * FROM QuizBasicInfoView WHERE title LIKE '%' || :query || '%' OR collectionName LIKE '%' || :query || '%'")
    fun getFilteredQuizzes(query: String): Flow<List<QuizBasicInfo>>
    @Query("SELECT * FROM quiz_attempt WHERE quizId = :quizId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLastQuizAttemptWithQuestionsById(quizId: String): QuizAttemptFullInfo?

    @Transaction
    @Query("SELECT * FROM quiz_attempt WHERE quizId = :quizId ORDER BY createdAt DESC LIMIT 1")
    fun getLastQuizAttemptWithQuestionsByIdFlow(quizId: String): Flow<QuizAttemptFullInfo?>

    @Transaction
    @Query("SELECT * FROM quiz_attempt WHERE quizId = :quizId ORDER BY createdAt DESC")
    fun getQuizAttemptsByQuizIdFlow(quizId: String): Flow<List<QuizAttemptFullInfo>>

    @Transaction
    @Query("SELECT * FROM quiz_attempt WHERE id = :attemptId LIMIT 1")
    fun getQuizAttemptByIdFlow(attemptId: String): Flow<QuizAttemptFullInfo?>


    @Query("SELECT * FROM question_attempt WHERE quizAttemptId = :quizAttemptId ORDER BY sortOrder")
    suspend fun getQuestionsAttemptedByQuizAttemptId(quizAttemptId: String): List<QuestionAttemptEntity>

    @Query("UPDATE quiz SET title = :title, quizCollectionId = :collectionId, updatedAt = :updatedAt WHERE id = :quizId")
    suspend fun updateQuizData(
        quizId: String,
        title: String,
        collectionId: String,
        updatedAt: String
    )
}