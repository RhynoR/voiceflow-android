package com.voiceflow.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.PixelFormat
import android.os.Binder
import android.os.IBinder
import android.os.Messenger
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.compositionContext
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.voiceflow.app.R
import com.voiceflow.app.accessibility.AccessibilityBridgeService
import com.voiceflow.app.ipc.BridgeProtocol
import com.voiceflow.app.ui.bubble.BubbleOverlay
import com.voiceflow.app.ui.bubble.BubbleViewModel
import com.voiceflow.app.ui.theme.VoiceFlowTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that owns the floating bubble overlay and coordinates
 * audio recording, transcription, and IPC to the accessibility process.
 *
 * Uses a [ComposeView] hosted in [WindowManager] for the overlay UI.
 */
@AndroidEntryPoint
class FlowService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    // ── Lifecycle / ViewModelStore / SavedState (required by ComposeView) ───

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val _viewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = _viewModelStore

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    // ── DI ───────────────────────────────────────────────────────────────────

    @Inject
    lateinit var bubbleViewModel: BubbleViewModel

    // ── Overlay ──────────────────────────────────────────────────────────────

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var isOverlayAttached = false

    private val overlayParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 24
        y = 200
    }

    // ── Accessibility IPC ────────────────────────────────────────────────────

    private var bridgeMessenger: Messenger? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + AndroidUiDispatcher.Main)

    private val bridgeConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            bridgeMessenger = Messenger(binder)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            bridgeMessenger = null
            // Rebind asynchronously
            bindToBridgeService()
        }
    }

    // ── Binder ───────────────────────────────────────────────────────────────

    inner class LocalBinder : Binder() {
        fun getService(): FlowService = this@FlowService
    }

    private val binder = LocalBinder()
    override fun onBind(intent: Intent): IBinder = binder

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        startForeground(NOTIFICATION_ID, buildNotification())
        addOverlay()
        bindToBridgeService()
        scheduleRecoveryJob()
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        removeOverlay()
        try { unbindService(bridgeConnection) } catch (_: Exception) {}
        serviceScope.cancel()
        _viewModelStore.clear()
        super.onDestroy()
    }

    // ── Overlay management ───────────────────────────────────────────────────

    private fun addOverlay() {
        if (isOverlayAttached) return
        val view = ComposeView(this).also {
            it.setViewTreeLifecycleOwner(this)
            it.setViewTreeViewModelStoreOwner(this)
            it.setViewTreeSavedStateRegistryOwner(this)
        }

        val recomposer = Recomposer(AndroidUiDispatcher.Main)
        view.compositionContext = recomposer
        serviceScope.launch { recomposer.runRecomposeAndApplyChanges() }

        view.setContent {
            VoiceFlowTheme {
                BubbleOverlay(
                    viewModel = bubbleViewModel,
                    onPositionChanged = { offset ->
                        overlayParams.x = offset.x
                        overlayParams.y = offset.y
                        windowManager?.updateViewLayout(view, overlayParams)
                    },
                )
            }
        }

        windowManager?.addView(view, overlayParams)
        composeView = view
        isOverlayAttached = true
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun stopOverlay() {
        removeOverlay()
    }

    private fun removeOverlay() {
        if (!isOverlayAttached) return
        composeView?.let { windowManager?.removeView(it) }
        composeView = null
        isOverlayAttached = false
    }

    // ── IPC ──────────────────────────────────────────────────────────────────

    private fun bindToBridgeService() {
        val intent = Intent(this, AccessibilityBridgeService::class.java)
        bindService(intent, bridgeConnection, Context.BIND_AUTO_CREATE)
    }

    fun sendTextToAccessibility(text: String) {
        val messenger = bridgeMessenger ?: return
        try {
            messenger.send(BridgeProtocol.buildInjectTextMessage(text))
        } catch (e: Exception) {
            // Bridge not connected — text will be lost; consider a retry queue
        }
    }

    // ── Notification ─────────────────────────────────────────────────────────

    private fun buildNotification(): Notification {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "VoiceFlow",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "VoiceFlow dictation service" }
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VoiceFlow")
            .setContentText("Tap the floating bubble to dictate")
            .setSmallIcon(R.drawable.ic_mic)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    // ── JobScheduler recovery ─────────────────────────────────────────────────

    private fun scheduleRecoveryJob() {
        val scheduler = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
        val job = JobInfo.Builder(
            FlowRecoveryJobService.JOB_ID,
            ComponentName(this, FlowRecoveryJobService::class.java),
        )
            .setPeriodic(15 * 60 * 1000L) // 15 minutes (minimum allowed)
            .setPersisted(true)
            .build()
        scheduler.schedule(job)
    }

    companion object {
        @Volatile
        var isRunning: Boolean = false
            private set

        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "voiceflow_service"
    }
}
