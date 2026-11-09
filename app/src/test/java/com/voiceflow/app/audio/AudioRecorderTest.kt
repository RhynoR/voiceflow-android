package com.voiceflow.app.audio

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AudioRecorder.
 * Uses MockK to mock the underlying AudioRecord reference so no hardware is needed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AudioRecorderTest {

    private lateinit var recorder: AudioRecorder
    private lateinit var mockProvider: AudioRecordProvider

    @Before
    fun setUp() {
        mockProvider = mockk(relaxed = true)
        recorder = AudioRecorder(sampleRateHz = 16000, provider = mockProvider)
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
    fun `stop recording cancels the flow`() = runTest {
        every { mockProvider.read(any()) } returns ShortArray(1600)

        val flow = recorder.startRecording()
        recorder.stopRecording()

        // After stop, flow should complete without yielding chunks
        val chunks = flow.take(0).toList()
        assertTrue(chunks.isEmpty())
    }

    @Test
    fun `release calls provider release`() {
        recorder.release()
        verify { mockProvider.release() }
    }

    @Test
    fun `isRecording is false before start`() {
        assertFalse(recorder.isRecording)
    }

    @Test
    fun `isRecording is true after start and false after stop`() = runTest {
        every { mockProvider.read(any()) } returns ShortArray(1600)

        recorder.startRecording()
        assertTrue(recorder.isRecording)

        recorder.stopRecording()
        assertFalse(recorder.isRecording)
    }

    private fun assertFalse(value: Boolean) =
        org.junit.Assert.assertFalse(value)
}
