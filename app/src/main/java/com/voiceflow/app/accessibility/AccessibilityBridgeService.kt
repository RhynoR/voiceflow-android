package com.voiceflow.app.accessibility

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.voiceflow.app.ipc.BridgeMessage
import com.voiceflow.app.ipc.BridgeProtocol

/**
 * Bound service running in the :accessibility process.
 *
 * The main process binds to this service and sends a [BridgeMessage.INJECT_TEXT] message.
 * This service forwards the text to [FlowAccessibilityService] via a shared singleton queue.
 */
class AccessibilityBridgeService : Service() {

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                BridgeMessage.INJECT_TEXT -> {
                    val text = BridgeProtocol.extractText(msg)
                    if (text != null) {
                        Log.d(TAG, "Received INJECT_TEXT: ${text.take(50)}")
                        FlowAccessibilityService.enqueueText(text)
                    }
                }
                BridgeMessage.PING -> {
                    msg.replyTo?.send(BridgeProtocol.buildPongMessage())
                }
                else -> super.handleMessage(msg)
            }
        }
    }

    private val messenger = Messenger(handler)

    override fun onBind(intent: Intent): IBinder = messenger.binder

    companion object {
        private const val TAG = "AccessibilityBridgeSvc"
    }
}
