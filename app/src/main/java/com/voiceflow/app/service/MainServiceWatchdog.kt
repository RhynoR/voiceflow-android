package com.voiceflow.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * BroadcastReceiver that listens for BOOT_COMPLETED and restarts [FlowService].
 */
class MainServiceWatchdog : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.i(TAG, "Boot completed — starting FlowService")

        // Android 14+ forbids starting a microphone foreground service from
        // BOOT_COMPLETED (SecurityException), and Android 12+ may refuse
        // background FGS starts entirely. Never let that crash the app.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Log.i(TAG, "Skipping mic service start at boot (not allowed on Android 14+)")
            return
        }
        try {
            context.startForegroundService(Intent(context, FlowService::class.java))
        } catch (e: Exception) {
            Log.w(TAG, "Could not start FlowService at boot", e)
        }
    }

    companion object {
        private const val TAG = "MainServiceWatchdog"
    }
}
