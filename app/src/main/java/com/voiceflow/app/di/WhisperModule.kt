package com.voiceflow.app.di

import com.voiceflow.app.whisper.WhisperEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WhisperModule {

    @Provides
    @Singleton
    fun provideWhisperEngine(): WhisperEngine = WhisperEngine()
}
