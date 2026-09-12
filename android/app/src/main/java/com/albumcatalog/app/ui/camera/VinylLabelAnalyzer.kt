package com.albumcatalog.app.ui.camera

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

enum class LabelScanStatus {
    SEARCHING,
    CENTERING,
    STABILIZING,
    LOCKED
}

data class LabelScanState(
    val status: LabelScanStatus = LabelScanStatus.SEARCHING,
    val progress: Float = 0f,
    val candidateCatno: String? = null
)

class VinylLabelAnalyzer(
    private val isScanningEnabled: () -> Boolean,
    private val isContinuousMode: () -> Boolean = { false },
    private val onLabelDetected: (fullText: String, catno: String?) -> Unit,
    private val onScanStateChanged: ((LabelScanState) -> Unit)? = null
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var centeredStartTime: Long = 0L
    private val STABILITY_DWELL_MS = 650L
    private var lastScannedTimestamp: Long = 0L
    private var lastScannedCatno: String? = null

    // Regex matching vinyl label catalog numbers (e.g. BWZ804, DC980, BST-84154, PC 34474, ST-11531)
    private val catnoRegex = Regex(
        """\b([A-Z]{1,6}[-\s]?[0-9]{3,7}[A-Z0-9-]*|[0-9]{5,8})\b""",
        RegexOption.IGNORE_CASE
    )

    @Volatile
    private var isTriggered = false

    @Volatile
    var latestInsideText: String = ""
        private set

    @Volatile
    var latestInsideCatno: String? = null
        private set

    fun resetTrigger() {
        isTriggered = false
        centeredStartTime = 0L
        lastScannedCatno = null
        lastScannedTimestamp = 0L
    }

    fun triggerManualCapture(): Boolean {
        if (latestInsideText.isNotBlank() && !isTriggered) {
            isTriggered = true
            onScanStateChanged?.invoke(LabelScanState(
                status = LabelScanStatus.LOCKED,
                progress = 1f,
                candidateCatno = latestInsideCatno
            ))
            onLabelDetected(latestInsideText, latestInsideCatno)
            return true
        }
        return false
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val continuous = isContinuousMode()
        if (!isScanningEnabled() || (!continuous && isTriggered)) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val imgWidth = image.width.toFloat()
        val imgHeight = image.height.toFloat()

        // Circular reticle center and radius in upright camera image coordinates
        val centerX = imgWidth * 0.5f
        val centerY = imgHeight * 0.45f
        val reticleRadius = imgWidth * 0.35f
        val reticleRadiusSq = reticleRadius * reticleRadius

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                if (!isScanningEnabled() || isTriggered) return@addOnSuccessListener

                // 1. Spatial Filtering: Only consider text lines with bounding boxes strictly inside the reticle
                val insideLines = mutableListOf<Text.Line>()
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        val box = line.boundingBox ?: continue
                        val cx = box.centerX().toFloat()
                        val cy = box.centerY().toFloat()
                        val distSq = (cx - centerX) * (cx - centerX) + (cy - centerY) * (cy - centerY)
                        if (distSq <= reticleRadiusSq) {
                            val trimmed = line.text.trim()
                            if (trimmed.length >= 2) {
                                insideLines.add(line)
                            }
                        }
                    }
                }

                if (insideLines.isEmpty()) {
                    centeredStartTime = 0L
                    latestInsideText = ""
                    latestInsideCatno = null
                    if (continuous && System.currentTimeMillis() - lastScannedTimestamp > 20_000L) {
                        lastScannedCatno = null
                    }
                    onScanStateChanged?.invoke(LabelScanState(LabelScanStatus.SEARCHING, 0f))
                    return@addOnSuccessListener
                }

                // Extract catalog number candidate from inside lines only
                var catnoMatch: String? = null
                for (line in insideLines) {
                    val text = line.text.trim()
                    if (text.contains("RPM", ignoreCase = true) ||
                        text.contains("STEREO", ignoreCase = true) ||
                        text.contains("MONO", ignoreCase = true) ||
                        text.contains("SIDE", ignoreCase = true)) {
                        continue
                    }
                    val match = catnoRegex.find(text)
                    if (match != null) {
                        catnoMatch = match.value
                        break
                    }
                }

                val fullInsideText = insideLines.joinToString("\n") { it.text.trim() }.trim()
                latestInsideText = fullInsideText
                latestInsideCatno = catnoMatch

                // 2. Centering & Completeness Verification
                var sumX = 0f
                var sumY = 0f
                var minY = Float.MAX_VALUE
                var maxY = Float.MIN_VALUE
                for (line in insideLines) {
                    val box = line.boundingBox!!
                    val cx = box.centerX().toFloat()
                    val cy = box.centerY().toFloat()
                    sumX += cx
                    sumY += cy
                    if (cy < minY) minY = cy
                    if (cy > maxY) maxY = cy
                }
                val centroidX = sumX / insideLines.size
                val centroidY = sumY / insideLines.size
                val centroidDist = Math.hypot((centroidX - centerX).toDouble(), (centroidY - centerY).toDouble()).toFloat()
                val verticalSpan = if (maxY >= minY) maxY - minY else 0f

                // Label must be substantial and balanced across the spindle center
                val hasSubstance = (catnoMatch != null && insideLines.size >= 2) ||
                                   (insideLines.size >= 3 && fullInsideText.length >= 12)
                val isWellCentered = centroidDist <= (reticleRadius * 0.45f)
                val hasVerticalSpread = verticalSpan >= (reticleRadius * 0.22f)

                val isCentered = hasSubstance && isWellCentered && hasVerticalSpread

                val now = System.currentTimeMillis()
                if (!isCentered) {
                    // Text is in reticle but not yet centered or not enough lines
                    centeredStartTime = 0L
                    onScanStateChanged?.invoke(LabelScanState(
                        status = LabelScanStatus.CENTERING,
                        progress = 0f,
                        candidateCatno = catnoMatch
                    ))
                } else {
                    // 3. Stability Dwell Time
                    if (centeredStartTime == 0L) {
                        centeredStartTime = now
                    }

                    val elapsed = now - centeredStartTime
                    val progress = (elapsed.toFloat() / STABILITY_DWELL_MS).coerceIn(0f, 1f)

                    if (elapsed < STABILITY_DWELL_MS) {
                        onScanStateChanged?.invoke(LabelScanState(
                            status = LabelScanStatus.STABILIZING,
                            progress = progress,
                            candidateCatno = catnoMatch
                        ))
                    } else {
                        val nowMs = System.currentTimeMillis()
                        if (continuous) {
                            val isDifferent = (catnoMatch != null && catnoMatch != lastScannedCatno)
                            val cooldown = if (isDifferent) 2500L else 20_000L
                            if (nowMs - lastScannedTimestamp < cooldown) {
                                return@addOnSuccessListener
                            }
                            lastScannedTimestamp = nowMs
                            lastScannedCatno = catnoMatch
                            centeredStartTime = 0L
                            onScanStateChanged?.invoke(LabelScanState(
                                status = LabelScanStatus.LOCKED,
                                progress = 1f,
                                candidateCatno = catnoMatch
                            ))
                            onLabelDetected(fullInsideText, catnoMatch)
                        } else {
                            // LOCKED! Trigger match query
                            isTriggered = true
                            onScanStateChanged?.invoke(LabelScanState(
                                status = LabelScanStatus.LOCKED,
                                progress = 1f,
                                candidateCatno = catnoMatch
                            ))
                            onLabelDetected(fullInsideText, catnoMatch)
                        }
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
