package com.albumcatalog.app.ui.camera

import android.util.Size
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

@Composable
fun CameraPreviewView(
    scanMode: ScanMode,
    isScanningActive: Boolean,
    isTorchOn: Boolean,
    isBatchMode: Boolean = false,
    onBarcodeDetected: (String) -> Unit,
    onLabelDetected: (String, String?) -> Unit,
    onLabelScanStateChanged: ((LabelScanState) -> Unit)? = null,
    manualCaptureTrigger: Long = 0L,
    onManualCaptureResult: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var camera: Camera? = remember { null }
    var currentLabelAnalyzer by remember { mutableStateOf<VinylLabelAnalyzer?>(null) }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    LaunchedEffect(isTorchOn) {
        camera?.cameraControl?.enableTorch(isTorchOn)
    }

    // High resolution (1080p) analysis for vinyl barcodes and label OCR
    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setTargetResolution(Size(1920, 1080))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }

    LaunchedEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Dynamic Camera Lifecycle: Shut down physical camera sensor when scanning is paused/lookup in progress
    LaunchedEffect(cameraProvider, isScanningActive, scanMode) {
        val provider = cameraProvider ?: return@LaunchedEffect

        if (!isScanningActive || scanMode == ScanMode.MANUAL) {
            try {
                camera?.cameraControl?.enableTorch(false)
                currentLabelAnalyzer = null
                imageAnalysis.clearAnalyzer()
                provider.unbindAll()
                camera = null
                onLabelScanStateChanged?.invoke(LabelScanState(LabelScanStatus.SEARCHING, 0f))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                provider.unbindAll()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                if (scanMode == ScanMode.BARCODE) {
                    currentLabelAnalyzer = null
                    imageAnalysis.setAnalyzer(
                        cameraExecutor,
                        BarcodeAnalyzer(
                            isScanningEnabled = { isScanningActive },
                            isContinuousMode = { isBatchMode },
                            onBarcodeDetected = onBarcodeDetected
                        )
                    )
                } else {
                    val analyzer = VinylLabelAnalyzer(
                        isScanningEnabled = { isScanningActive },
                        isContinuousMode = { isBatchMode },
                        onLabelDetected = onLabelDetected,
                        onScanStateChanged = onLabelScanStateChanged
                    )
                    currentLabelAnalyzer = analyzer
                    imageAnalysis.setAnalyzer(cameraExecutor, analyzer)
                }

                val boundCamera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                camera = boundCamera
                if (isTorchOn) {
                    boundCamera.cameraControl.enableTorch(true)
                }

                // Enable tap-to-focus on preview
                previewView.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_UP) {
                        val factory = previewView.meteringPointFactory
                        val point = factory.createPoint(event.x, event.y)
                        val action = FocusMeteringAction.Builder(point).build()
                        boundCamera.cameraControl.startFocusAndMetering(action)
                    }
                    true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(manualCaptureTrigger) {
        if (manualCaptureTrigger > 0L) {
            val success = currentLabelAnalyzer?.triggerManualCapture() ?: false
            onManualCaptureResult?.invoke(success)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                camera?.cameraControl?.enableTorch(false)
                cameraProvider?.unbindAll()
                cameraExecutor.shutdown()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (scanMode != ScanMode.MANUAL) {
        AndroidView(
            factory = { previewView },
            modifier = modifier.fillMaxSize()
        )
    }
}
