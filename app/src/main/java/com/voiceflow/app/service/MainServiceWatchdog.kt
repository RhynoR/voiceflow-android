package com.voiceflow.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver that listens for BOOT_COMPLETED and restarts [FlowService].
 */
class MainServiceWatchdog : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.i(TAG, "Boot completed — starting FlowService")

        val serviceIntent = Intent(context, FlowService::class.java)
        context.startForegroundService(serviceIntent)
    }

    companion object {
        private const val TAG = "MainServiceWatchdog"
    }
}
