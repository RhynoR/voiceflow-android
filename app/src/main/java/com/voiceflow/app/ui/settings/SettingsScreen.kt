package com.voiceflow.app.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.voiceflow.app.R
import com.voiceflow.app.accessibility.FlowAccessibilityService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onRequestMicPermission: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onStartService: () -> Unit = {},
) {
    val context = LocalContext.current

    val hasMic = ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECORD_AUDIO
    ) == PermissionChecker.PERMISSION_GRANTED

    val hasOverlay = Settings.canDrawOverlays(context)

    val hasAccessibility = run {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: ""
        enabledServices.contains(context.packageName, ignoreCase = true)
    }

    val hasNotification = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PermissionChecker.PERMISSION_GRANTED
    } else {
        true
    }

    val allGranted = hasMic && hasOverlay && hasAccessibility && hasNotification

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("VoiceFlow Settings") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Text("Permissions", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            PermissionRow(
                label = "Microphone",
                granted = hasMic,
                onAction = onRequestMicPermission,
            )

            Spacer(modifier = Modifier.height(8.dp))

            PermissionRow(
                label = "Display over other apps",
                granted = hasOverlay,
                onAction = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}"),
                        )
                    )
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            PermissionRow(
                label = "Accessibility service",
                granted = hasAccessibility,
                onAction = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            PermissionRow(
                label = "Notifications (foreground service)",
                granted = hasNotification,
                onAction = onRequestNotificationPermission,
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (allGranted) {
                Button(
                    onClick = onStartService,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF188038)),
                ) {
                    Text("Start VoiceFlow", color = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "All permissions granted. Tap Start to show the floating bubble.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF188038),
                )
            } else {
                Text(
                    "Grant all permissions above, then come back here to start.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("About", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "VoiceFlow — on-device voice dictation overlay.\nAll transcription is performed locally using whisper.cpp. No audio ever leaves your device.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    onAction: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = if (granted) "✓" else "✗",
            color = if (granted) Color(0xFF188038) else Color(0xFFD93025),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(28.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        if (!granted) {
            Button(onClick = onAction) {
                Text("Grant")
            }
        }
    }
}
