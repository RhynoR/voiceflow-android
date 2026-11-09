package com.voiceflow.app.di

import android.content.Context
import androidx.room.Room
import com.voiceflow.app.data.db.TranscriptionDao
import com.voiceflow.app.data.db.TranscriptionDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTranscriptionDatabase(
        @ApplicationContext context: Context,
    ): TranscriptionDatabase =
        Room.databaseBuilder(
            context,
            TranscriptionDatabase::class.java,
            "transcriptions.db",
        ).build()

    @Provides
    @Singleton
    fun provideTranscriptionDao(db: TranscriptionDatabase): TranscriptionDao =
        db.transcriptionDao()
}
