package com.voiceflow.app.model

/**
 * Result returned by WhisperEngine.transcribe().
 */
data class TranscriptionResult(
    val text: String,
    val success: Boolean,
    val errorMessage: String? = null,
)
