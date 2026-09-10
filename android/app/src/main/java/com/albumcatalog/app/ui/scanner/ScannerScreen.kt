package com.albumcatalog.app.ui.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.albumcatalog.app.data.model.Album
import com.albumcatalog.app.ui.camera.CameraPreviewView
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

    var searchQuery by remember { mutableStateOf("") }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Camera Preview
        CameraPreviewView(
            scanMode = scanMode,
            isScanningActive = isScanningActive,
            isTorchOn = isTorchOn,
            onBarcodeDetected = { viewModel.onBarcodeDetected(it) },
            onLabelDetected = { text, catno -> viewModel.onLabelDetected(text, catno) }
        )

        // 2. Viewfinder Overlay
        ScanOverlay(
            scanMode = scanMode,
            isScanningActive = isScanningActive
        )

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

        // 5. No Match Found Card with Exact Scanned Metadata
        if (matchState is MatchUiState.NoMatch) {
            val noMatch = matchState as MatchUiState.NoMatch
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 30.dp, start = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.94f))
                    .border(1.5.dp, Color(0xFFEF5350), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

                    Text(
                        text = "Scanned metadata used for search:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Scanned values display card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E1E24))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Scan Type: ${noMatch.queryType}",
                            color = VinylGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Raw Scanned Value: \"${noMatch.scannedValue}\"",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        if (!noMatch.detectedCatno.isNullOrEmpty()) {
                            Text(
                                text = "Extracted Catalog #: ${noMatch.detectedCatno}",
                                color = Color(0xFF81D4FA),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = "Sources checked: Local MusicBrainz (5.67M releases), Remote MusicBrainz, and Discogs API.",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.resumeScanning() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = VinylGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Scan Again", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                searchQuery = noMatch.detectedCatno ?: noMatch.scannedValue
                                viewModel.resumeScanning()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Edit in Search", color = VinylGold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 6. Match Results Modal Bottom Sheet
        if (matchState is MatchUiState.MatchesFound) {
            val candidates = (matchState as MatchUiState.MatchesFound).candidates
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
    }
}
