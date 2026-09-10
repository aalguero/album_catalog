package com.albumcatalog.app.ui.camera

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ScanMode {
    BARCODE,
    VINYL_LABEL
}

@Composable
fun ScanOverlay(
    scanMode: ScanMode,
    isScanningActive: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height * 0.45f

            if (scanMode == ScanMode.BARCODE) {
                // Barcode box
                val rectWidth = width * 0.78f
                val rectHeight = rectWidth * 0.60f
                val left = (width - rectWidth) / 2f
                val top = centerY - (rectHeight / 2f)

                val holePath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(Offset(left, top), Size(rectWidth, rectHeight)),
                            cornerRadius = CornerRadius(16.dp.toPx())
                        )
                    )
                }

                // Darken outside
                clipPath(holePath, clipOp = ClipOp.Difference) {
                    drawRect(Color.Black.copy(alpha = 0.55f))
                }

                // Border
                drawRoundRect(
                    color = if (isScanningActive) Color(0xFFFFB74D) else Color(0xFF757575),
                    topLeft = Offset(left, top),
                    size = Size(rectWidth, rectHeight),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Laser scan line
                if (isScanningActive) {
                    val laserY = top + (rectHeight * laserPosition)
                    drawLine(
                        color = Color(0xFFFF7043),
                        start = Offset(left + 8.dp.toPx(), laserY),
                        end = Offset(left + rectWidth - 8.dp.toPx(), laserY),
                        strokeWidth = 2.5.dp.toPx()
                    )
                }
            } else {
                // Circular Vinyl Center Label
                val labelRadius = width * 0.38f
                val spindleRadius = 14.dp.toPx()

                val circlePath = Path().apply {
                    addOval(
                        Rect(
                            center = Offset(centerX, centerY),
                            radius = labelRadius
                        )
                    )
                }

                clipPath(circlePath, clipOp = ClipOp.Difference) {
                    drawRect(Color.Black.copy(alpha = 0.55f))
                }

                // Outer circle for label edge
                drawCircle(
                    color = if (isScanningActive) Color(0xFF64B5F6) else Color(0xFF757575),
                    radius = labelRadius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Center spindle hole
                drawCircle(
                    color = if (isScanningActive) Color(0xFF90CAF9) else Color(0xFF9E9E9E),
                    radius = spindleRadius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Crosshairs for center alignment
                if (isScanningActive) {
                    val crosshairLen = 22.dp.toPx()
                    drawLine(
                        color = Color(0xFF90CAF9).copy(alpha = 0.7f),
                        start = Offset(centerX - crosshairLen, centerY),
                        end = Offset(centerX + crosshairLen, centerY),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFF90CAF9).copy(alpha = 0.7f),
                        start = Offset(centerX, centerY - crosshairLen),
                        end = Offset(centerX, centerY + crosshairLen),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }
        }

        // Hint Label
        Text(
            text = if (!isScanningActive) {
                "Scanning paused"
            } else if (scanMode == ScanMode.BARCODE) {
                "Center vinyl barcode in frame"
            } else {
                "Center vinyl record label inside circle"
            },
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
