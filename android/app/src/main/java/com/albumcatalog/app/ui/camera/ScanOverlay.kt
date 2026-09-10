package com.albumcatalog.app.ui.camera

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ScanMode {
    BARCODE,
    SPINE_TEXT
}

@Composable
fun ScanOverlay(
    scanMode: ScanMode,
    isScanning: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Dimensions for viewfinder rect
            val rectWidth: Float
            val rectHeight: Float
            val topOffset: Float

            if (scanMode == ScanMode.BARCODE) {
                // Square-ish box for barcode
                rectWidth = width * 0.75f
                rectHeight = rectWidth * 0.65f
                topOffset = (height - rectHeight) * 0.40f
            } else {
                // Slender horizontal slit for album spine
                rectWidth = width * 0.88f
                rectHeight = 110.dp.toPx()
                topOffset = (height - rectHeight) * 0.42f
            }

            val leftOffset = (width - rectWidth) / 2f

            val holePath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(
                            offset = Offset(leftOffset, topOffset),
                            size = Size(rectWidth, rectHeight)
                        ),
                        cornerRadius = CornerRadius(16.dp.toPx())
                    )
                )
            }

            // Darken outside viewfinder
            clipPath(holePath, clipOp = ClipOp.Difference) {
                drawRect(Color.Black.copy(alpha = 0.55f))
            }

            // Target frame border
            drawRoundRect(
                color = if (scanMode == ScanMode.BARCODE) Color(0xFFFFB74D) else Color(0xFF64B5F6),
                topLeft = Offset(leftOffset, topOffset),
                size = Size(rectWidth, rectHeight),
                cornerRadius = CornerRadius(16.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )

            // Animated scanline
            if (isScanning) {
                val laserY = topOffset + (rectHeight * laserPosition)
                drawLine(
                    color = if (scanMode == ScanMode.BARCODE) Color(0xFFFF7043) else Color(0xFF42A5F5),
                    start = Offset(leftOffset + 8.dp.toPx(), laserY),
                    end = Offset(leftOffset + rectWidth - 8.dp.toPx(), laserY),
                    strokeWidth = 2.5.dp.toPx()
                )
            }
        }

        // Hint Label
        Text(
            text = if (scanMode == ScanMode.BARCODE)
                "Center vinyl barcode in frame"
            else
                "Align spine text & catalog number in frame",
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
