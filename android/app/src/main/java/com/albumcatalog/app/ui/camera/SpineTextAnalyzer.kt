package com.albumcatalog.app.ui.camera

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class SpineTextAnalyzer(
    private val onTextRecognized: (fullText: String, detectedCatno: String?) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var lastRecognizedText: String? = null
    private var lastTimestamp: Long = 0L

    // Regex to spot typical vinyl catalog numbers on spine (e.g. PC 34474, BST-84154, ST-11531, 25 35 240)
    private val catnoRegex = Regex("""\b([A-Z]{1,5}[-\s]?[0-9]{3,7}|[0-9]{5,8})\b""", RegexOption.IGNORE_CASE)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val fullText = visionText.text.trim()
                    if (fullText.length >= 3) {
                        val now = System.currentTimeMillis()
                        if (fullText != lastRecognizedText || now - lastTimestamp > 2500L) {
                            lastRecognizedText = fullText
                            lastTimestamp = now
                            val catnoMatch = catnoRegex.find(fullText)?.value
                            onTextRecognized(fullText, catnoMatch)
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
