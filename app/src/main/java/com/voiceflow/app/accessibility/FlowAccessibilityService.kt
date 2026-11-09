package com.voiceflow.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Accessibility service running in the :accessibility process.
 *
 * Receives text from [AccessibilityBridgeService] via a shared queue and injects
 * it into the currently focused text field using ACTION_SET_TEXT, falling back to
 * clipboard + paste if the node does not support ACTION_SET_TEXT.
 */
class FlowAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Drain any pending text injections when focus changes
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            drainQueue()
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "FlowAccessibilityService connected")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    // ── Text injection ───────────────────────────────────────────────────────

    private fun drainQueue() {
        var text: String?
        while (textQueue.poll().also { text = it } != null) {
            injectText(text!!)
        }
    }

    private fun injectText(text: String) {
        val root = rootInActiveWindow ?: run {
            Log.w(TAG, "No active window root — queuing text back")
            textQueue.add(text)
            return
        }

        val focusedNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focusedNode == null) {
            Log.w(TAG, "No focused input node — queuing text back")
            textQueue.add(text)
            root.recycle()
            return
        }

        val injected = trySetText(focusedNode, text) || tryClipboardPaste(focusedNode, text)
        if (!injected) {
            Log.e(TAG, "All injection methods failed for text: ${text.take(20)}")
        }

        focusedNode.recycle()
        root.recycle()
    }

    private fun trySetText(node: AccessibilityNodeInfo, text: String): Boolean {
        val actions = node.actionList.map { it.id }
        if (AccessibilityNodeInfo.ACTION_SET_TEXT !in actions) return false

        val args = Bundle()
        args.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            text,
        )
        val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        Log.d(TAG, "ACTION_SET_TEXT result: $success")
        return success
    }

    private fun tryClipboardPaste(node: AccessibilityNodeInfo, text: String): Boolean {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("voiceflow", text))

        val pasted = node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        Log.d(TAG, "Clipboard paste result: $pasted")
        return pasted
    }

    companion object {
        private const val TAG = "FlowAccessibilitySvc"

        // Shared queue — written from AccessibilityBridgeService, drained here
        private val textQueue = ConcurrentLinkedQueue<String>()

        @Volatile
        private var instance: FlowAccessibilityService? = null

        fun enqueueText(text: String) {
            textQueue.add(text)
            // If service is live, drain immediately
            instance?.drainQueue()
        }
    }
}
