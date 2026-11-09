package com.voiceflow.app.ipc

import android.os.Bundle
import android.os.Message

/**
 * Serialization/deserialization helpers for the Messenger IPC bridge.
 *
 * All message construction and parsing goes through this object so the
 * encoding contract lives in exactly one place.
 */
object BridgeProtocol {

    const val KEY_TEXT = "text"

    /**
     * Builds an INJECT_TEXT message carrying [text].
     */
    fun buildInjectTextMessage(text: String): Message {
        val msg = Message.obtain()
        msg.what = BridgeMessage.INJECT_TEXT
        val data = Bundle()
        data.putString(KEY_TEXT, text)
        msg.data = data
        return msg
    }

    /**
     * Extracts the text payload from an INJECT_TEXT message.
     * Returns null if [msg] is not an INJECT_TEXT message or has no text key.
     */
    fun extractText(msg: Message): String? {
        if (msg.what != BridgeMessage.INJECT_TEXT) return null
        return msg.data.getString(KEY_TEXT)
    }

    fun buildPingMessage(): Message = Message.obtain().also { it.what = BridgeMessage.PING }

    fun buildPongMessage(): Message = Message.obtain().also { it.what = BridgeMessage.PONG }
}
