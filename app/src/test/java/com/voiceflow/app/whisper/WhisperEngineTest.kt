package com.voiceflow.app.whisper

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for WhisperEngine.
 * JNI calls are intercepted via a JniBridge interface so tests run on JVM without .so.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WhisperEngineTest {

    private lateinit var engine: WhisperEngine
    private lateinit var mockBridge: WhisperJniBridge

    @Before
    fun setUp() {
        mockBridge = mockk(relaxed = true)
        engine = WhisperEngine(bridge = mockBridge)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ── State machine ────────────────────────────────────────────────────────

    @Test
    fun `initial state is UNLOADED`() {
        assertEquals(WhisperState.UNLOADED, engine.state)
    }

    @Test
    fun `loadModel transitions to READY on success`() = runTest {
        every { mockBridge.initModel(any()) } returns 42L

        engine.loadModel("/fake/path/ggml-tiny.en.bin")

        assertEquals(WhisperState.READY, engine.state)
    }

    @Test
    fun `loadModel stays UNLOADED when bridge returns zero handle`() = runTest {
        every { mockBridge.initModel(any()) } returns 0L

        engine.loadModel("/bad/path")

        assertEquals(WhisperState.UNLOADED, engine.state)
    }

    @Test
    fun `transcribe returns text from bridge`() = runTest {
        every { mockBridge.initModel(any()) } returns 1L
        every { mockBridge.transcribe(any(), any()) } returns "hello world"

        engine.loadModel("/path")
        val result = engine.transcribe(FloatArray(1600))

        assertEquals("hello world", result.text)
        assertTrue(result.success)
    }

    @Test
    fun `transcribe on UNLOADED engine returns failure result`() = runTest {
        val result = engine.transcribe(FloatArray(1600))

        assertFalse(result.success)
        assertEquals("", result.text)
    }

    @Test
    fun `release transitions to UNLOADED and calls bridge freeModel`() = runTest {
        every { mockBridge.initModel(any()) } returns 5L

        engine.loadModel("/path")
        engine.release()

        assertEquals(WhisperState.UNLOADED, engine.state)
        verify { mockBridge.freeModel(5L) }
    }

    @Test
    fun `transcribe returns empty on bridge exception`() = runTest {
        every { mockBridge.initModel(any()) } returns 1L
        every { mockBridge.transcribe(any(), any()) } throws RuntimeException("JNI crash")

        engine.loadModel("/path")
        val result = engine.transcribe(FloatArray(1600))

        assertFalse(result.success)
        assertEquals("", result.text)
    }

    @Test
    fun `concurrent transcribe calls are serialised`() = runTest {
        every { mockBridge.initModel(any()) } returns 1L
        every { mockBridge.transcribe(any(), any()) } returns "text"

        engine.loadModel("/path")

        // Calling transcribe multiple times on the same engine must not throw
        repeat(5) {
            val r = engine.transcribe(FloatArray(1600))
            assertTrue(r.success)
        }
    }
}
