package com.voiceflow.app.di

import com.voiceflow.app.audio.AudioRecorder
import com.voiceflow.app.audio.VoiceActivityDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides
    @Singleton
    fun provideAudioRecorder(): AudioRecorder =
        AudioRecorder(sampleRateHz = 16000)

    @Provides
    @Singleton
    fun provideVoiceActivityDetector(): VoiceActivityDetector =
        VoiceActivityDetector(
            energyThreshold = 500.0,
            silenceDurationMs = 1500L,
            sampleRateHz = 16000,
        )
}
