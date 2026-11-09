package com.voiceflow.app.audio

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AudioRecorder.
 * Uses MockK to mock the underlying AudioRecord reference so no hardware is needed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class AudioRecorderTest {

    private lateinit var recorder: AudioRecorder
    private lateinit var mockProvider: AudioRecordProvider

    @Before
    fun setUp() {
        mockProvider = mockk(relaxed = true)
        // Inject UnconfinedTestDispatcher so flowOn(ioDispatcher) runs synchronously
        // in the test coroutine scope — avoids real-thread timing races.
        recorder = AudioRecorder(
            sampleRateHz = 16000,
            provider = mockProvider,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun `recorder uses 16000 Hz sample rate`() {
        assertEquals(16000, recorder.sampleRateHz)
    }

    @Test
    fun `recorder uses mono channel`() {
        assertEquals(1, recorder.channelCount)
    }

    @Test
    fun `start recording emits non-empty flow`() = runTest {
        val fakeChunk = ShortArray(1600) { it.toShort() }
        every { mockProvider.read(any()) } returns fakeChunk

        val chunks = recorder.startRecording().take(1).toList()

        assertNotNull(chunks)
        assertEquals(1, chunks.size)
        assertEquals(1600, chunks[0].size)
    }

    @Test
    fun `stop recording cancels the flow after collection begins`() = runTest {
        every { mockProvider.read(any()) } returns ShortArray(1600)

        val collected = mutableListOf<ShortArray>()
        // With UnconfinedTestDispatcher, the launched coroutine runs eagerly
        // until it suspends; stopRecording() interrupts the loop.
        val job = launch {
            recorder.startRecording().collect { chunk ->
                collected.add(chunk)
                // Stop after the first chunk so the test completes quickly
                recorder.stopRecording()
            }
        }
        advanceUntilIdle()

        assertFalse(recorder.isRecording)
        assertTrue(collected.isNotEmpty())
        job.cancel()
    }

    @Test
    fun `release calls provider release after recording starts`() = runTest {
        every { mockProvider.read(any()) } returns ShortArray(1600)

        val job = launch {
            recorder.startRecording().collect {
                // activeProvider is set before the first read; release from inside
                recorder.release()
            }
        }
        advanceUntilIdle()

        verify { mockProvider.release() }
        job.cancel()
    }

    @Test
    fun `isRecording is false before start`() {
        assertFalse(recorder.isRecording)
    }

    @Test
    fun `isRecording is false after flow completes`() = runTest {
        every { mockProvider.read(any()) } returns ShortArray(1600)

        // take(1) collects one chunk then cancels upstream, triggering the
        // finally block which sets isRecording = false.
        var capturedDuringCollection = false
        recorder.startRecording().take(1).collect {
            capturedDuringCollection = recorder.isRecording
        }

        assertTrue(capturedDuringCollection)   // was true while flow body ran
        assertFalse(recorder.isRecording)      // false after finally block ran
    }

    private fun assertFalse(value: Boolean) =
        org.junit.Assert.assertFalse(value)
}
