package com.example.studywise.data.db.di

import android.content.Context
import com.example.studywise.data.db.StudyWiseDatabase
import com.example.studywise.data.db.dao.QuizDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideStudyWiseDatabase(
        @ApplicationContext context: Context
    ): StudyWiseDatabase {
        return StudyWiseDatabase.getInstance(context)
    }

    @Provides
    fun provideQuizDao(
        database: StudyWiseDatabase
    ): QuizDao {
        return database.quizDao()
    }
}