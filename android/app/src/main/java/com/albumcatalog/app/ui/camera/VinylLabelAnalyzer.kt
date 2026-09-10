package com.albumcatalog.app.ui.camera

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class VinylLabelAnalyzer(
    private val isScanningEnabled: () -> Boolean,
    private val onLabelDetected: (fullText: String, catno: String?) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var lastRecognizedText: String? = null
    private var lastTimestamp: Long = 0L

    // Regex matching vinyl label catalog numbers (e.g. BWZ804, DC980, BST-84154, PC 34474, ST-11531)
    private val catnoRegex = Regex(
        """\b([A-Z]{1,6}[-\s]?[0-9]{3,7}[A-Z0-9-]*|[0-9]{5,8})\b""",
        RegexOption.IGNORE_CASE
    )

    @Volatile
    private var isTriggered = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (!isScanningEnabled() || isTriggered) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    if (!isScanningEnabled() || isTriggered) return@addOnSuccessListener
                    val fullText = visionText.text.trim()
                    if (fullText.length >= 3) {
                        val now = System.currentTimeMillis()
                        if (fullText != lastRecognizedText || now - lastTimestamp > 3000L) {
                            lastRecognizedText = fullText
                            lastTimestamp = now

                            // Find best candidate for catalog number
                            var catnoMatch: String? = null
                            for (line in visionText.textBlocks.flatMap { it.lines }) {
                                val text = line.text.trim()
                                // Skip common speed / stereo markings
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

                            isTriggered = true
                            onLabelDetected(fullText, catnoMatch)
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
