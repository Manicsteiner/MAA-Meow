package com.aliothmoon.maameow.presentation.view.background

import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.presentation.state.PreviewTouchMarker

@Composable
fun TouchPreviewOverlay(
    markers: List<PreviewTouchMarker>,
    displayWidth: Int,
    displayHeight: Int,
    modifier: Modifier = Modifier,
) {
    if (markers.isEmpty()) {
        return
    }

    Canvas(modifier = modifier) {
        val now = SystemClock.elapsedRealtime()
        val maxX = (displayWidth - 1).coerceAtLeast(1)
        val maxY = (displayHeight - 1).coerceAtLeast(1)

        markers.forEach { marker ->
            val age = (now - marker.createdAtMs).coerceAtLeast(0L)
            val progress = (age / PreviewTouchMarker.TTL_MS.toFloat()).coerceIn(0f, 1f)
            val alpha = (1f - progress).coerceIn(0f, 1f)
            if (alpha <= 0f) {
                return@forEach
            }

            val center = Offset(
                x = size.width * marker.x.coerceIn(0, maxX) / maxX.toFloat(),
                y = size.height * marker.y.coerceIn(0, maxY) / maxY.toFloat()
            )

            // Refined Colors
            val colorGreen = Color(0xFF81C784)
            val colorAmber = Color(0xFFFFD54F)
            val colorRed = Color(0xFFE57373)
            val colorWhite = Color.White

            when (marker.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Subtle outer expanding ripple
                    drawCircle(
                        color = colorGreen.copy(alpha = alpha * 0.3f),
                        radius = (8.dp.toPx() + (12.dp.toPx() * progress)),
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx() * alpha)
                    )
                    // Soft glow core
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(colorGreen.copy(alpha = alpha * 0.8f), colorGreen.copy(alpha = 0f)),
                            center = center,
                            radius = 12.dp.toPx()
                        ),
                        radius = 12.dp.toPx(),
                        center = center
                    )
                    // Precise center point
                    drawCircle(
                        color = colorWhite.copy(alpha = alpha * 0.9f),
                        radius = 2.5.dp.toPx(),
                        center = center
                    )
                }

                MotionEvent.ACTION_MOVE -> {
                    // Compact glowing dot for movement
                    drawCircle(
                        color = colorAmber.copy(alpha = alpha * 0.5f),
                        radius = 5.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = colorWhite.copy(alpha = alpha * 0.4f),
                        radius = 1.5.dp.toPx(),
                        center = center
                    )
                }

                MotionEvent.ACTION_UP -> {
                    // Quick, crisp burst ring
                    drawCircle(
                        color = colorRed.copy(alpha = alpha * 0.6f),
                        radius = (6.dp.toPx() + (18.dp.toPx() * progress)),
                        center = center,
                        style = Stroke(width = 2.dp.toPx() * alpha)
                    )
                    // Rapidly shrinking core
                    drawCircle(
                        color = colorRed.copy(alpha = alpha * 0.8f),
                        radius = 3.dp.toPx() * (1f - progress),
                        center = center
                    )
                }
            }
        }
    }
}
