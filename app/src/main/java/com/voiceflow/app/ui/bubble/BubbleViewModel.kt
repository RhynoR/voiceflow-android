package com.voiceflow.app.ui.bubble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voiceflow.app.audio.AudioRecorder
import com.voiceflow.app.audio.VoiceActivityDetector
import com.voiceflow.app.data.repository.TranscriptionRepository
import com.voiceflow.app.whisper.WhisperEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BubbleState { IDLE, LISTENING, PROCESSING, DONE }

data class BubbleUiState(
    val bubbleState: BubbleState = BubbleState.IDLE,
    val lastTranscription: String = "",
    val errorMessage: String? = null,
)

@HiltViewModel
class BubbleViewModel @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val vad: VoiceActivityDetector,
    private val whisperEngine: WhisperEngine,
    private val repository: TranscriptionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BubbleUiState())
    val uiState: StateFlow<BubbleUiState> = _uiState.asStateFlow()

    // Raw PCM accumulator
    private val pcmBuffer = mutableListOf<Short>()

    fun onMicTouchDown() {
        if (_uiState.value.bubbleState != BubbleState.IDLE) return
        vad.reset()
        pcmBuffer.clear()
        _uiState.value = _uiState.value.copy(bubbleState = BubbleState.LISTENING)
        startRecording()
    }

    fun onMicTouchUp() {
        audioRecorder.stopRecording()
        processBuffer()
    }

    private fun startRecording() {
        viewModelScope.launch {
            audioRecorder.startRecording().collect { chunk ->
                pcmBuffer.addAll(chunk.toList())
                val vadResult = vad.process(chunk)
                if (vadResult.silenceThresholdReached) {
                    audioRecorder.stopRecording()
                }
            }
        }
    }

    private fun processBuffer() {
        viewModelScope.launch {
            if (pcmBuffer.isEmpty()) {
                _uiState.value = _uiState.value.copy(bubbleState = BubbleState.IDLE)
                return@launch
            }

            _uiState.value = _uiState.value.copy(bubbleState = BubbleState.PROCESSING)

            // Convert ShortArray PCM-16 to FloatArray normalized [-1.0, 1.0]
            val floatSamples = FloatArray(pcmBuffer.size) { i ->
                pcmBuffer[i].toFloat() / Short.MAX_VALUE
            }

            val result = whisperEngine.transcribe(floatSamples)

            if (result.success && result.text.isNotBlank()) {
                repository.insert(result.text, wasInjected = true)
                _uiState.value = _uiState.value.copy(
                    bubbleState = BubbleState.DONE,
                    lastTranscription = result.text,
                    errorMessage = null,
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    bubbleState = BubbleState.IDLE,
                    errorMessage = result.errorMessage ?: "Transcription failed",
                )
            }
        }
    }

    fun onDoneAcknowledged() {
        _uiState.value = _uiState.value.copy(
            bubbleState = BubbleState.IDLE,
            errorMessage = null,
        )
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.release()
    }
}
