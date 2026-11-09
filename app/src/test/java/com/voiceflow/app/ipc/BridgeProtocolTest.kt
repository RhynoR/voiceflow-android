package com.voiceflow.app.ipc

import android.os.Bundle
import android.os.Message
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for BridgeProtocol — message serialization / deserialization.
 * Uses Robolectric to get a real Android Bundle without an emulator.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BridgeProtocolTest {

    // ── INJECT_TEXT ──────────────────────────────────────────────────────────

    @Test
    fun `buildInjectTextMessage sets correct what code`() {
        val msg = BridgeProtocol.buildInjectTextMessage("hello")
        assertEquals(BridgeMessage.INJECT_TEXT, msg.what)
    }

    @Test
    fun `buildInjectTextMessage encodes text in data bundle`() {
        val msg = BridgeProtocol.buildInjectTextMessage("hello world")
        val text = msg.data.getString(BridgeProtocol.KEY_TEXT)
        assertEquals("hello world", text)
    }

    @Test
    fun `extractText returns text from INJECT_TEXT message`() {
        val msg = BridgeProtocol.buildInjectTextMessage("test text")
        val extracted = BridgeProtocol.extractText(msg)
        assertEquals("test text", extracted)
    }

    @Test
    fun `extractText returns null for wrong message type`() {
        val msg = Message.obtain()
        msg.what = 9999
        val extracted = BridgeProtocol.extractText(msg)
        assertNull(extracted)
    }

    @Test
    fun `extractText handles empty string`() {
        val msg = BridgeProtocol.buildInjectTextMessage("")
        val extracted = BridgeProtocol.extractText(msg)
        assertEquals("", extracted)
    }

    // ── PING / PONG ──────────────────────────────────────────────────────────

    @Test
    fun `buildPingMessage sets PING what code`() {
        val msg = BridgeProtocol.buildPingMessage()
        assertEquals(BridgeMessage.PING, msg.what)
    }

    @Test
    fun `buildPongMessage sets PONG what code`() {
        val msg = BridgeProtocol.buildPongMessage()
        assertEquals(BridgeMessage.PONG, msg.what)
    }

    // ── Round-trip ───────────────────────────────────────────────────────────

    @Test
    fun `round-trip preserves unicode text`() {
        val unicodeText = "Hello 🎤 世界 привет"
        val msg = BridgeProtocol.buildInjectTextMessage(unicodeText)
        val extracted = BridgeProtocol.extractText(msg)
        assertEquals(unicodeText, extracted)
    }

    @Test
    fun `round-trip preserves very long text`() {
        val longText = "a".repeat(5000)
        val msg = BridgeProtocol.buildInjectTextMessage(longText)
        val extracted = BridgeProtocol.extractText(msg)
        assertEquals(longText, extracted)
    }
}
