package com.albumcatalog.app.ui.scanner

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.albumcatalog.app.data.model.Album
import com.albumcatalog.app.ui.camera.CameraPreviewView
import com.albumcatalog.app.ui.camera.LabelScanStatus
import com.albumcatalog.app.ui.camera.ScanMode
import com.albumcatalog.app.ui.camera.ScanOverlay
import com.albumcatalog.app.ui.components.Top3MatchCards
import com.albumcatalog.app.ui.theme.TextPrimary
import com.albumcatalog.app.ui.theme.TextSecondary
import com.albumcatalog.app.ui.theme.TextTertiary
import com.albumcatalog.app.ui.theme.VinylBlack
import com.albumcatalog.app.ui.theme.VinylGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onCandidateSelectedForConfirmation: (Album) -> Unit,
    viewModel: ScannerViewModel = viewModel()
) {
    val scanMode by viewModel.scanMode.collectAsState()
    val isScanningActive by viewModel.isScanningActive.collectAsState()
    val isTorchOn by viewModel.isTorchOn.collectAsState()
    val matchState by viewModel.matchState.collectAsState()
    val discogsState by viewModel.discogsState.collectAsState()
    val ocrCorrectionState by viewModel.ocrCorrectionState.collectAsState()
    val labelScanState by viewModel.labelScanState.collectAsState()
    val manualCaptureTrigger by viewModel.manualCaptureTrigger.collectAsState()

    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Camera Preview
        CameraPreviewView(
            scanMode = scanMode,
            isScanningActive = isScanningActive,
            isTorchOn = isTorchOn,
            onBarcodeDetected = { viewModel.onBarcodeDetected(it) },
            onLabelDetected = { text, catno -> viewModel.onLabelDetected(text, catno) },
            onLabelScanStateChanged = { viewModel.updateLabelScanState(it) },
            manualCaptureTrigger = manualCaptureTrigger,
            onManualCaptureResult = { success ->
                if (!success) {
                    Toast.makeText(
                        context,
                        "Align label inside circle before capturing",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        // 2. Viewfinder Overlay
        ScanOverlay(
            scanMode = scanMode,
            isScanningActive = isScanningActive,
            labelScanState = labelScanState
        )

        // 2.5. Vinyl Label Manual Capture Shutter Button
        if (scanMode == ScanMode.VINYL_LABEL && isScanningActive && matchState is MatchUiState.Idle) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .border(
                            width = 3.dp,
                            color = when (labelScanState.status) {
                                LabelScanStatus.STABILIZING, LabelScanStatus.LOCKED -> VinylGold
                                LabelScanStatus.CENTERING -> Color(0xFFFF9800)
                                else -> Color.White.copy(alpha = 0.8f)
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { viewModel.triggerManualCapture() },
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(
                                when (labelScanState.status) {
                                    LabelScanStatus.STABILIZING, LabelScanStatus.LOCKED -> VinylGold
                                    LabelScanStatus.CENTERING -> Color(0xFFFFB74D)
                                    else -> Color.White.copy(alpha = 0.9f)
                                }
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Capture Label Now",
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // 3. Top Controls Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 44.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mode Toggle: Barcode vs Vinyl Label
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = scanMode == ScanMode.BARCODE,
                        onClick = { viewModel.setScanMode(ScanMode.BARCODE) },
                        label = { Text("Barcode", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VinylGold,
                            selectedLabelColor = Color.Black,
                            selectedLeadingIconColor = Color.Black
                        )
                    )
                    FilterChip(
                        selected = scanMode == ScanMode.VINYL_LABEL,
                        onClick = { viewModel.setScanMode(ScanMode.VINYL_LABEL) },
                        label = { Text("Vinyl Label", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.TrackChanges, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF64B5F6),
                            selectedLabelColor = Color.Black,
                            selectedLeadingIconColor = Color.Black
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Resume button if scanning is paused
                    if (!isScanningActive && matchState is MatchUiState.Idle) {
                        IconButton(
                            onClick = { viewModel.resumeScanning() },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(VinylGold)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = Color.Black)
                        }
                    }

                    // Torch Toggle
                    IconButton(
                        onClick = { viewModel.toggleTorch() },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                    ) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Torch",
                            tint = if (isTorchOn) VinylGold else Color.White
                        )
                    }
                }
            }

            // Quick Manual Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search title, artist, or cat# manually...", fontSize = 13.sp, color = TextSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.65f)),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { viewModel.performManualSearch(searchQuery) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = VinylGold)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VinylGold,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }

        // 4. Loading indicator when matching
        if (matchState is MatchUiState.Matching) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = VinylGold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Matching release metadata...", color = TextPrimary, fontSize = 13.sp)
                }
            }
        }

        // 4.5. Camera Standby Overlay (Battery Saver Mode)
        if (!isScanningActive && matchState is MatchUiState.Idle) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { viewModel.resumeScanning() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.resumeScanning() },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(VinylGold)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Resume Camera",
                            tint = Color.Black,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    Text(
                        text = "Camera in Standby (Battery Saver)",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Tap anywhere to resume scanning",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 5. No Match Found Card with Exact Scanned Metadata
        if (matchState is MatchUiState.NoMatch) {
            val noMatch = matchState as MatchUiState.NoMatch
            var isRawTextExpanded by remember { mutableStateOf(false) }

            // Tap background outside the card to quickly turn scanner back on
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { viewModel.resumeScanning() }
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp, start = 16.dp, end = 16.dp)
                    .heightIn(max = 520.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.95f))
                    .border(1.5.dp, Color(0xFFEF5350), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "No Matching Release Found",
                            color = Color(0xFFEF5350),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { viewModel.resumeScanning() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Scanned metadata used for search:",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = {
                                viewModel.openOcrCorrection(
                                    catno = noMatch.detectedCatno,
                                    artist = noMatch.artist,
                                    title = noMatch.title,
                                    rawText = noMatch.scannedValue,
                                    scanType = noMatch.queryType
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = VinylGold, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Metadata", color = VinylGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Scanned values display card - clickable to edit
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E1E24))
                            .clickable {
                                viewModel.openOcrCorrection(
                                    catno = noMatch.detectedCatno,
                                    artist = noMatch.artist,
                                    title = noMatch.title,
                                    rawText = noMatch.scannedValue,
                                    scanType = noMatch.queryType
                                )
                            }
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Scan Type: ${noMatch.queryType}",
                                color = VinylGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap to edit ✎",
                                color = Color(0xFF81D4FA),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (!noMatch.detectedCatno.isNullOrEmpty()) {
                            Text(
                                text = "Extracted Catalog #: ${noMatch.detectedCatno}",
                                color = Color(0xFF81D4FA),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (!noMatch.artist.isNullOrEmpty()) {
                            Text(
                                text = "Artist: ${noMatch.artist}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        if (!noMatch.title.isNullOrEmpty()) {
                            Text(
                                text = "Title: ${noMatch.title}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Text(
                            text = "Raw Scanned Value:",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "\"${noMatch.scannedValue}\"",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = if (isRawTextExpanded) 20 else 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (noMatch.scannedValue.contains("\n") || noMatch.scannedValue.length > 80) {
                            Text(
                                text = if (isRawTextExpanded) "Show less ▲" else "Show full raw text (${noMatch.scannedValue.lines().size} lines) ▼",
                                color = VinylGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { isRawTextExpanded = !isRawTextExpanded }
                            )
                        }
                    }

                    Text(
                        text = "Sources checked: Local MusicBrainz (5.67M releases), Remote MusicBrainz, and Discogs API.",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.resumeScanning() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E36), contentColor = TextPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Scan Again", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                searchQuery = noMatch.detectedCatno ?: noMatch.scannedValue.lines().firstOrNull() ?: noMatch.scannedValue
                                viewModel.resumeScanning()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VinylGold),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VinylGold)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit in Search", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                viewModel.openOcrCorrection(
                                    catno = noMatch.detectedCatno,
                                    artist = noMatch.artist,
                                    title = noMatch.title,
                                    rawText = noMatch.scannedValue,
                                    scanType = noMatch.queryType
                                )
                            },
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(containerColor = VinylGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit OCR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 6. Match Results Modal Bottom Sheet
        if (matchState is MatchUiState.MatchesFound) {
            val candidates = (matchState as MatchUiState.MatchesFound).candidates
            val matchFound = matchState as MatchUiState.MatchesFound
            ModalBottomSheet(
                onDismissRequest = { viewModel.resumeScanning() },
                sheetState = bottomSheetState,
                containerColor = VinylBlack
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Suggested Matches", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        IconButton(onClick = { viewModel.resumeScanning() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Top3MatchCards(
                        candidates = candidates,
                        onSelectAndSaveUnspecific = { candidate ->
                            val album = viewModel.buildStagedAlbum(candidate)
                            viewModel.resetMatchState()
                            onCandidateSelectedForConfirmation(album)
                        },
                        onRefineWithDiscogs = { candidate ->
                            viewModel.startDiscogsRefinement(candidate)
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.openOcrCorrection(
                                catno = matchFound.detectedCatno,
                                rawText = matchFound.rawScannedValue,
                                scanType = if (scanMode == ScanMode.BARCODE) "Barcode" else "Vinyl Center Label OCR"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF81D4FA)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0288D1))
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("None of these match? Correct OCR & Re-search", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // 7. Discogs Pressing Refinement Sheet
        if (discogsState !is DiscogsUiState.Hidden) {
            val isLoading = discogsState is DiscogsUiState.Loading
            val pressings = (discogsState as? DiscogsUiState.Ready)?.pressings ?: emptyList()

            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissDiscogsSheet() },
                containerColor = VinylBlack
            ) {
                DiscogsRefineSheet(
                    isLoading = isLoading,
                    pressings = pressings,
                    onSelectPressing = { pressing ->
                        val candidate = viewModel.stagedCandidate
                        if (candidate != null) {
                            val album = viewModel.buildStagedAlbum(candidate, pressing)
                            viewModel.resetMatchState()
                            onCandidateSelectedForConfirmation(album)
                        }
                    },
                    onKeepUnspecific = {
                        val candidate = viewModel.stagedCandidate
                        if (candidate != null) {
                            val album = viewModel.buildStagedAlbum(candidate, null)
                            viewModel.resetMatchState()
                            onCandidateSelectedForConfirmation(album)
                        }
                    },
                    onDismiss = { viewModel.dismissDiscogsSheet() }
                )
            }
        }

        // 8. Dedicated OCR Error Correction Sheet
        if (ocrCorrectionState.isVisible) {
            val ocrSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissOcrCorrection() },
                sheetState = ocrSheetState,
                containerColor = VinylBlack
            ) {
                OcrCorrectionSheet(
                    initialCatno = ocrCorrectionState.catno,
                    initialArtist = ocrCorrectionState.artist,
                    initialTitle = ocrCorrectionState.title,
                    initialRawText = ocrCorrectionState.rawText,
                    scanType = ocrCorrectionState.scanType,
                    onReSearch = { catno, artist, title, rawText ->
                        viewModel.performCorrectedSearch(catno, artist, title, rawText)
                    },
                    onCreateManual = { catno, artist, title, label, year ->
                        val manualAlbum = viewModel.buildManualAlbum(catno, artist, title, label, year)
                        viewModel.resetMatchState()
                        onCandidateSelectedForConfirmation(manualAlbum)
                    },
                    onDismiss = { viewModel.dismissOcrCorrection() }
                )
            }
        }
    }
}
