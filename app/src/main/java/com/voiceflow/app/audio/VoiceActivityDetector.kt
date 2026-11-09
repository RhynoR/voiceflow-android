package com.voiceflow.app.audio

import kotlin.math.sqrt

/**
 * Result of a single VoiceActivityDetector.process() call.
 *
 * @param isVoiceActive     true if the buffer's RMS energy meets/exceeds the threshold
 * @param rmsEnergy         computed RMS for the buffer
 * @param silenceThresholdReached  true once cumulative silence exceeds [silenceDurationMs]
 */
data class VadResult(
    val isVoiceActive: Boolean,
    val rmsEnergy: Double,
    val silenceThresholdReached: Boolean,
)

/**
 * Energy-based Voice Activity Detector.
 *
 * @param energyThreshold    RMS energy level (0–32767) above which a frame is considered speech
 * @param silenceDurationMs  how long (ms) consecutive silence must last before [VadResult.silenceThresholdReached]
 * @param sampleRateHz       sample rate — used to convert silence duration to sample count
 */
class VoiceActivityDetector(
    val energyThreshold: Double = 500.0,
    private val silenceDurationMs: Long = 1500L,
    private val sampleRateHz: Int = 16000,
) {
    // Number of samples that correspond to [silenceDurationMs]
    private val silenceSampleThreshold: Long =
        (sampleRateHz.toLong() * silenceDurationMs) / 1000L

    private var accumulatedSilenceSamples: Long = 0L
    private var hasSeenVoice: Boolean = false

    /**
     * Process a PCM buffer and return a [VadResult].
     */
    fun process(buffer: ShortArray): VadResult {
        val rms = computeRms(buffer)
        val isVoice = rms >= energyThreshold

        if (isVoice) {
            hasSeenVoice = true
            accumulatedSilenceSamples = 0L
        } else {
            accumulatedSilenceSamples += buffer.size
        }

        val silenceThresholdReached =
            hasSeenVoice && accumulatedSilenceSamples >= silenceSampleThreshold

        return VadResult(
            isVoiceActive = isVoice,
            rmsEnergy = rms,
            silenceThresholdReached = silenceThresholdReached,
        )
    }

    /** Resets accumulated silence state (call before a new recording session). */
    fun reset() {
        accumulatedSilenceSamples = 0L
        hasSeenVoice = false
    }

    private fun computeRms(buffer: ShortArray): Double {
        if (buffer.isEmpty()) return 0.0
        var sumSquares = 0.0
        for (sample in buffer) {
            sumSquares += sample.toDouble() * sample.toDouble()
        }
        return sqrt(sumSquares / buffer.size)
    }
}
