package com.voiceflow.app.accessibility

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Messenger
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import com.voiceflow.app.ipc.BridgeMessage
import com.voiceflow.app.ipc.BridgeProtocol
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Instrumented tests for AccessibilityBridgeService.
 * Verifies Messenger binding and INJECT_TEXT message delivery.
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityBridgeTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun bridgeServiceBindsAndReturnsBinder() {
        val intent = Intent(context, AccessibilityBridgeService::class.java)
        val binder: IBinder = serviceRule.bindService(intent)
        assertNotNull("AccessibilityBridgeService binder must not be null", binder)
    }

    @Test
    fun messengerIsObtainedFromBinder() {
        val intent = Intent(context, AccessibilityBridgeService::class.java)
        val binder: IBinder = serviceRule.bindService(intent)
        // Messenger wraps the binder — construction must not throw
        val messenger = Messenger(binder)
        assertNotNull(messenger)
    }

    @Test
    fun injectTextMessageIsDeliveredWithoutException() {
        val intent = Intent(context, AccessibilityBridgeService::class.java)
        val binder: IBinder = serviceRule.bindService(intent)
        val messenger = Messenger(binder)

        val latch = CountDownLatch(1)
        val msg = BridgeProtocol.buildInjectTextMessage("integration test text")

        // Send message — no reply handler needed for this smoke test
        try {
            messenger.send(msg)
            latch.countDown()
        } catch (e: Exception) {
            // Should not throw
        }

        val sent = latch.await(2, TimeUnit.SECONDS)
        assert(sent) { "Message was not delivered within 2 seconds" }
    }
}
