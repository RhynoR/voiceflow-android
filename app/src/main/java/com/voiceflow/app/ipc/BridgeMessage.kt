package com.voiceflow.app.ipc

/**
 * Message type constants for the Messenger IPC channel between
 * FlowService (main process) and AccessibilityBridgeService (:accessibility process).
 */
object BridgeMessage {
    /** Send transcribed text to the accessibility process for injection. */
    const val INJECT_TEXT = 1

    /** Liveness ping from the main process. */
    const val PING = 2

    /** Liveness pong reply from the accessibility process. */
    const val PONG = 3
}
