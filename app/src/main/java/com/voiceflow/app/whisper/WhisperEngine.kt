package com.voiceflow.app.whisper

import com.voiceflow.app.model.TranscriptionResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class WhisperState { UNLOADED, LOADING, READY, TRANSCRIBING }

/**
 * Thin interface over the JNI surface — enables mocking in tests without .so.
 */
interface WhisperJniBridge {
    fun initModel(modelPath: String): Long
    fun transcribe(ctxHandle: Long, pcmSamples: FloatArray): String
    fun freeModel(ctxHandle: Long)
}

/**
 * Production bridge that delegates to the native library.
 */
internal object NativeWhisperBridge : WhisperJniBridge {
    init {
        System.loadLibrary("voiceflow-jni")
    }

    override fun initModel(modelPath: String): Long =
        WhisperEngine.initModelNative(modelPath)

    override fun transcribe(ctxHandle: Long, pcmSamples: FloatArray): String =
        WhisperEngine.transcribeNative(ctxHandle, pcmSamples)

    override fun freeModel(ctxHandle: Long) =
        WhisperEngine.freeModelNative(ctxHandle)
}

/**
 * Manages the lifecycle of a whisper.cpp context and serialises transcription calls.
 *
 * Inject a [WhisperJniBridge] to run on JVM without native .so (for unit tests).
 */
class WhisperEngine(
    private val bridge: WhisperJniBridge = NativeWhisperBridge,
) {
    private val mutex = Mutex()
    private var ctxHandle: Long = 0L

    @Volatile
    var state: WhisperState = WhisperState.UNLOADED
        private set

    // ── Lifecycle ────────────────────────────────────────────────────────────

    suspend fun loadModel(modelPath: String) {
        mutex.withLock {
            state = WhisperState.LOADING
            val handle = try {
                bridge.initModel(modelPath)
            } catch (e: Exception) {
                state = WhisperState.UNLOADED
                return
            }
            if (handle == 0L) {
                state = WhisperState.UNLOADED
            } else {
                ctxHandle = handle
                state = WhisperState.READY
            }
        }
    }

    suspend fun release() {
        mutex.withLock {
            if (ctxHandle != 0L) {
                bridge.freeModel(ctxHandle)
                ctxHandle = 0L
            }
            state = WhisperState.UNLOADED
        }
    }

    // ── Transcription ────────────────────────────────────────────────────────

    suspend fun transcribe(pcmSamples: FloatArray): TranscriptionResult {
        return mutex.withLock {
            if (state != WhisperState.READY || ctxHandle == 0L) {
                return@withLock TranscriptionResult(text = "", success = false, errorMessage = "Engine not ready")
            }
            state = WhisperState.TRANSCRIBING
            try {
                val text = bridge.transcribe(ctxHandle, pcmSamples)
                state = WhisperState.READY
                TranscriptionResult(text = text, success = true)
            } catch (e: Exception) {
                state = WhisperState.READY
                TranscriptionResult(text = "", success = false, errorMessage = e.message)
            }
        }
    }

    // ── JNI declarations (called by NativeWhisperBridge) ────────────────────

    companion object {
        @JvmStatic
        external fun initModelNative(modelPath: String): Long

        @JvmStatic
        external fun transcribeNative(ctx: Long, pcmSamples: FloatArray): String

        @JvmStatic
        external fun freeModelNative(ctx: Long)
    }
}
