package com.voiceflow.app.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/**
 * Abstraction over AudioRecord to allow injection in tests.
 */
interface AudioRecordProvider {
    fun read(buffer: ShortArray): ShortArray
    fun release()
}

/**
 * Production AudioRecordProvider backed by [AudioRecord].
 */
class DefaultAudioRecordProvider(
    sampleRateHz: Int,
    bufferSize: Int,
) : AudioRecordProvider {

    private val audioRecord = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        sampleRateHz,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        bufferSize,
    ).also { it.startRecording() }

    override fun read(buffer: ShortArray): ShortArray {
        audioRecord.read(buffer, 0, buffer.size)
        return buffer
    }

    override fun release() {
        if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            audioRecord.stop()
        }
        audioRecord.release()
    }
}

/**
 * Coroutine-based audio recorder that emits [ShortArray] chunks at 16 kHz mono PCM-16.
 *
 * Inject a custom [AudioRecordProvider] for unit tests.
 */
class AudioRecorder(
    val sampleRateHz: Int = 16000,
    private val chunkSizeMs: Int = 100,
    private val provider: AudioRecordProvider? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    val channelCount: Int = 1

    private val samplesPerChunk: Int = (sampleRateHz * chunkSizeMs) / 1000

    private val minBufferSize: Int = AudioRecord.getMinBufferSize(
        sampleRateHz,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
    ).coerceAtLeast(samplesPerChunk * 2)

    @Volatile
    var isRecording: Boolean = false
        private set

    private var activeProvider: AudioRecordProvider? = null

    /**
     * Starts recording and emits PCM chunks as a cold [Flow].
     * The flow completes when [stopRecording] is called.
     */
    fun startRecording(): Flow<ShortArray> = flow {
        isRecording = true
        val p = provider ?: DefaultAudioRecordProvider(sampleRateHz, minBufferSize)
        activeProvider = p

        try {
            while (coroutineContext.isActive && isRecording) {
                val buffer = ShortArray(samplesPerChunk)
                val chunk = p.read(buffer)
                emit(chunk.copyOf())
            }
        } finally {
            isRecording = false
        }
    }.flowOn(ioDispatcher)

    fun stopRecording() {
        isRecording = false
    }

    fun release() {
        isRecording = false
        activeProvider?.release()
        activeProvider = null
    }
}
