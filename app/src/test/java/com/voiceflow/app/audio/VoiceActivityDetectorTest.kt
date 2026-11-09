package com.voiceflow.app.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.sqrt

/**
 * Unit tests for VoiceActivityDetector.
 * Uses synthetic PCM data to verify energy-threshold logic.
 */
class VoiceActivityDetectorTest {

    private lateinit var vad: VoiceActivityDetector

    @Before
    fun setUp() {
        vad = VoiceActivityDetector(
            energyThreshold = 500.0,
            silenceDurationMs = 1500L,
            sampleRateHz = 16000
        )
    }

    // ── RMS energy helpers ───────────────────────────────────────────────────

    private fun silentBuffer(size: Int = 1600): ShortArray =
        ShortArray(size) { 0 }

    private fun loudBuffer(amplitude: Short = 10_000, size: Int = 1600): ShortArray =
        ShortArray(size) { amplitude }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    fun `silent buffer does not trigger voice activity`() {
        val result = vad.process(silentBuffer())
        assertFalse("Silent PCM should not detect voice", result.isVoiceActive)
    }

    @Test
    fun `loud buffer triggers voice activity`() {
        val result = vad.process(loudBuffer())
        assertTrue("Loud PCM should detect voice", result.isVoiceActive)
    }

    @Test
    fun `energy is zero for all-zero buffer`() {
        val result = vad.process(silentBuffer())
        assert(result.rmsEnergy == 0.0) { "RMS of silent buffer must be 0.0" }
    }

    @Test
    fun `energy is correct for constant amplitude buffer`() {
        val amplitude: Short = 1000
        val buffer = ShortArray(160) { amplitude }
        val result = vad.process(buffer)
        // RMS of constant signal = amplitude
        assert(result.rmsEnergy == amplitude.toDouble()) {
            "Expected RMS=$amplitude but got ${result.rmsEnergy}"
        }
    }

    @Test
    fun `silence after voice triggers silence detection after threshold`() {
        // Warm up with voice
        repeat(5) { vad.process(loudBuffer()) }
        // Feed silence for > 1500 ms worth of samples (1500ms @ 16kHz = 24000 samples)
        val silenceSamples = 24001
        val silenceChunks = silenceSamples / 1600
        var lastResult: VadResult? = null
        repeat(silenceChunks + 1) {
            lastResult = vad.process(silentBuffer())
        }
        assertTrue(
            "Should detect silence after threshold",
            lastResult?.silenceThresholdReached == true
        )
    }

    @Test
    fun `reset clears accumulated silence counter`() {
        repeat(20) { vad.process(silentBuffer()) }
        vad.reset()
        val result = vad.process(silentBuffer())
        assertFalse(
            "After reset, silence threshold should not be immediately reached",
            result.silenceThresholdReached
        )
    }

    @Test
    fun `threshold boundary — just below threshold is not voice`() {
        // threshold = 500.0, so amplitude 499 → RMS 499 < 500
        val buffer = ShortArray(1600) { 499 }
        val result = vad.process(buffer)
        assertFalse("Just below threshold must not be voice", result.isVoiceActive)
    }

    @Test
    fun `threshold boundary — at threshold is voice`() {
        // RMS of constant 500 == 500.0 which equals threshold
        val buffer = ShortArray(1600) { 500 }
        val result = vad.process(buffer)
        assertTrue("At threshold must be voice", result.isVoiceActive)
    }
}
