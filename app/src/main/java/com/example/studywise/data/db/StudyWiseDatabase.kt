package com.example.studywise.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.studywise.data.db.dao.QuizDao
import com.example.studywise.data.db.entity.AnswerOptionEntity
import com.example.studywise.data.db.entity.QuestionAttemptEntity
import com.example.studywise.data.db.entity.QuestionEntity
import com.example.studywise.data.db.entity.QuizAttemptEntity
import com.example.studywise.data.db.entity.QuizCollectionEntity
import com.example.studywise.data.db.entity.QuizEntity
import com.example.studywise.data.db.relation.QuizBasicInfo

@Database(
    entities = [
        QuizCollectionEntity::class,
        QuizEntity::class,
        QuestionEntity::class,
        AnswerOptionEntity::class,
        QuizAttemptEntity::class,
        QuestionAttemptEntity::class
    ],
    views = [QuizBasicInfo::class],
    version = 4,
    exportSchema = false,
)
abstract class StudyWiseDatabase : RoomDatabase() {

    abstract fun quizDao(): QuizDao

    companion object {
        @Volatile private var INSTANCE: StudyWiseDatabase? = null

        fun getInstance(context: Context): StudyWiseDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    StudyWiseDatabase::class.java,
                    "studywise_db",
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}