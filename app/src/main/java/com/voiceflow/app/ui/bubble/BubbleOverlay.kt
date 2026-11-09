package com.voiceflow.app.ui.bubble

import android.annotation.SuppressLint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.voiceflow.app.R
import kotlin.math.roundToInt

/**
 * Floating bubble overlay composable.
 * Hosts mic icon, pulsing listening indicator, spinner, and done checkmark.
 * Draggable via pointer input.
 */
@SuppressLint("MissingPermission") // RECORD_AUDIO is checked before FlowService starts
@Composable
fun BubbleOverlay(
    viewModel: BubbleViewModel,
    onPositionChanged: (IntOffset) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(64.dp)
            .background(
                color = bubbleColor(uiState.bubbleState),
                shape = CircleShape,
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                        onPositionChanged(
                            IntOffset(offsetX.roundToInt(), offsetY.roundToInt())
                        )
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        viewModel.onMicTouchDown()
                        tryAwaitRelease()
                        viewModel.onMicTouchUp()
                    }
                )
            },
    ) {
        when (uiState.bubbleState) {
            BubbleState.IDLE -> {
                Icon(
                    painter = painterResource(R.drawable.ic_mic),
                    contentDescription = "Start recording",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }

            BubbleState.LISTENING -> {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "pulseScale",
                )
                Icon(
                    painter = painterResource(R.drawable.ic_mic),
                    contentDescription = "Listening",
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .scale(scale),
                )
            }

            BubbleState.PROCESSING -> {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }

            BubbleState.DONE -> {
                Icon(
                    painter = painterResource(R.drawable.ic_bubble),
                    contentDescription = "Done",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

private fun bubbleColor(state: BubbleState): Color = when (state) {
    BubbleState.IDLE -> Color(0xFF1A73E8)
    BubbleState.LISTENING -> Color(0xFFD93025)
    BubbleState.PROCESSING -> Color(0xFFF29900)
    BubbleState.DONE -> Color(0xFF188038)
}
