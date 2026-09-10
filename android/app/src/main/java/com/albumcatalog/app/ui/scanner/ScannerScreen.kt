package com.albumcatalog.app.ui.scanner

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.albumcatalog.app.data.model.MatchCandidate
import com.albumcatalog.app.ui.camera.CameraPreviewView
import com.albumcatalog.app.ui.camera.ScanMode
import com.albumcatalog.app.ui.camera.ScanOverlay
import com.albumcatalog.app.ui.components.Top3MatchCards
import com.albumcatalog.app.ui.theme.TextPrimary
import com.albumcatalog.app.ui.theme.TextSecondary
import com.albumcatalog.app.ui.theme.VinylBlack
import com.albumcatalog.app.ui.theme.VinylCard
import com.albumcatalog.app.ui.theme.VinylGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel = viewModel()
) {
    val scanMode by viewModel.scanMode.collectAsState()
    val isTorchOn by viewModel.isTorchOn.collectAsState()
    val matchState by viewModel.matchState.collectAsState()
    val discogsState by viewModel.discogsState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var searchQuery by remember { mutableStateOf("") }
    var candidateToSave by remember { mutableStateOf<MatchCandidate?>(null) }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(saveState) {
        when (saveState) {
            is SaveUiState.Success -> {
                snackbarHostState.showSnackbar("Added '${(saveState as SaveUiState.Success).album.title}' to collection!")
                viewModel.clearSaveState()
            }
            is SaveUiState.Error -> {
                snackbarHostState.showSnackbar("Error saving album: ${(saveState as SaveUiState.Error).message}")
                viewModel.clearSaveState()
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Camera Preview
        CameraPreviewView(
            scanMode = scanMode,
            isTorchOn = isTorchOn,
            onBarcodeDetected = { viewModel.onBarcodeDetected(it) },
            onSpineTextRecognized = { text, catno -> viewModel.onSpineTextRecognized(text, catno) }
        )

        // 2. Viewfinder Overlay
        ScanOverlay(
            scanMode = scanMode,
            isScanning = matchState !is MatchUiState.Matching
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
                // Mode Toggle: Barcode vs Spine
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
                        selected = scanMode == ScanMode.SPINE_TEXT,
                        onClick = { viewModel.setScanMode(ScanMode.SPINE_TEXT) },
                        label = { Text("Spine OCR", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF64B5F6),
                            selectedLabelColor = Color.Black,
                            selectedLeadingIconColor = Color.Black
                        )
                    )
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

        // 4. Loading Spinner indicator when matching
        if (matchState is MatchUiState.Matching) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = VinylGold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Matching via MusicBrainz...", color = TextPrimary, fontSize = 13.sp)
                }
            }
        }

        // 5. Match Results Modal Bottom Sheet
        if (matchState is MatchUiState.MatchesFound) {
            val candidates = (matchState as MatchUiState.MatchesFound).candidates
            ModalBottomSheet(
                onDismissRequest = { viewModel.resetMatchState() },
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
                        Text("Preliminary Matches", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        IconButton(onClick = { viewModel.resetMatchState() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Top3MatchCards(
                        candidates = candidates,
                        onSelectAndSaveUnspecific = { candidate ->
                            candidateToSave = candidate
                        },
                        onRefineWithDiscogs = { candidate ->
                            viewModel.startDiscogsRefinement(candidate)
                        }
                    )
                }
            }
        }

        // 6. Discogs Pressing Refinement Sheet
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
                        viewModel.selectDiscogsPressing(pressing)
                        candidateToSave = viewModel.stagedCandidate
                    },
                    onKeepUnspecific = {
                        viewModel.keepUnspecificPressing()
                        candidateToSave = viewModel.stagedCandidate
                    },
                    onDismiss = { viewModel.dismissDiscogsSheet() }
                )
            }
        }

        // 7. Save Album Dialog (Condition + Shelf location)
        candidateToSave?.let { candidate ->
            SaveAlbumDialog(
                candidate = candidate,
                discogsPressing = viewModel.stagedPressing,
                onSave = { album ->
                    viewModel.saveToCatalog(album)
                    candidateToSave = null
                },
                onDismiss = {
                    candidateToSave = null
                }
            )
        }

        // 8. Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }
}
