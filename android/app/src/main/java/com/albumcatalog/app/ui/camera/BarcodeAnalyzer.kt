package com.albumcatalog.app.ui.camera

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(
    private val isScanningEnabled: () -> Boolean,
    private val isContinuousMode: () -> Boolean = { false },
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_QR_CODE
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)
    private var lastScannedCode: String? = null
    private var lastScannedTimestamp: Long = 0L
    @Volatile
    private var isTriggered = false

    fun resetTrigger() {
        isTriggered = false
        lastScannedCode = null
        lastScannedTimestamp = 0L
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val continuous = isContinuousMode()
        // Immediately drop frames if scanning is disabled, or if single-mode has already triggered
        if (!isScanningEnabled() || (!continuous && isTriggered)) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (!isScanningEnabled() || (!continuous && isTriggered)) return@addOnSuccessListener
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { rawCode ->
                            val code = rawCode.trim()
                            val now = System.currentTimeMillis()
                            if (continuous) {
                                val isDifferent = (code != lastScannedCode)
                                // Prevent duplicate re-scans:
                                // Same barcode: 20 seconds cooldown! (Prevents picking up dupes while handling/reading record)
                                // Different barcode: 2.0 seconds debounce
                                val debounceTime = if (isDifferent) 2000L else 20_000L
                                if (now - lastScannedTimestamp > debounceTime) {
                                    lastScannedCode = code
                                    lastScannedTimestamp = now
                                    onBarcodeDetected(code)
                                    return@addOnSuccessListener
                                }
                            } else {
                                // Single-scan mode
                                if (code != lastScannedCode || now - lastScannedTimestamp > 2500L) {
                                    lastScannedCode = code
                                    lastScannedTimestamp = now
                                    isTriggered = true
                                    onBarcodeDetected(code)
                                    return@addOnSuccessListener
                                }
                            }
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
