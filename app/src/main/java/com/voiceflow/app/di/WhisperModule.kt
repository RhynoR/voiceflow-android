package com.voiceflow.app.di

import android.content.Context
import android.util.Log
import com.voiceflow.app.whisper.ModelLoader
import com.voiceflow.app.whisper.WhisperEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WhisperModule {

    @Provides
    @Singleton
    fun provideWhisperEngine(
        @ApplicationContext context: Context,
    ): WhisperEngine {
        val engine = WhisperEngine()
        // Kick off model extraction + load in the background at app start.
        // The engine stays in UNLOADED state until the coroutine completes;
        // BubbleViewModel.transcribe() checks state and returns a failure result
        // gracefully while the model loads (~1–2s on first run, instant thereafter).
        CoroutineScope(Dispatchers.IO).launch {
            // Any failure here (model asset missing from the APK, disk full,
            // native lib problems) must not crash the app — the engine simply
            // stays UNLOADED and transcribe() reports "Engine not ready".
            try {
                val modelPath = ModelLoader.ensureModel(context)
                engine.loadModel(modelPath)
            } catch (t: Throwable) {
                Log.e("WhisperModule", "Failed to load Whisper model", t)
            }
        }
        return engine
    }
}
