package com.voiceflow.app.service

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Scheduled [JobService] that checks whether [FlowService] is running and restarts it if not.
 * Scheduled via JobScheduler in [FlowService.scheduleRecoveryJob].
 */
class FlowRecoveryJobService : JobService() {

    override fun onStartJob(params: JobParameters): Boolean {
        Log.d(TAG, "Recovery job triggered")
        if (!FlowService.isRunning) {
            Log.i(TAG, "FlowService not running — restarting")
            val intent = Intent(this, FlowService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
        jobFinished(params, false)
        return false
    }

    override fun onStopJob(params: JobParameters): Boolean = true // reschedule

    companion object {
        const val JOB_ID = 1001
        private const val TAG = "FlowRecoveryJobService"
    }
}
