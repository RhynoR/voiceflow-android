package com.voiceflow.app.service

import android.annotation.SuppressLint
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import android.util.Log

/**
 * Scheduled [JobService] that checks whether [FlowService] is running and restarts it if not.
 * Scheduled via JobScheduler in [FlowService.scheduleRecoveryJob].
 */
@SuppressLint("SpecifyJobSchedulerIdRange") // JOB_ID=1001 is static; no WorkManager conflict
class FlowRecoveryJobService : JobService() {

    override fun onStartJob(params: JobParameters): Boolean {
        Log.d(TAG, "Recovery job triggered")
        if (!FlowService.isRunning) {
            Log.i(TAG, "FlowService not running — restarting")
            val intent = Intent(this, FlowService::class.java)
            startForegroundService(intent)
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
