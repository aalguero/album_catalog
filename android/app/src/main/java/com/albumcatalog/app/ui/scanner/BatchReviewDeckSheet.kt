package com.albumcatalog.app.ui.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.albumcatalog.app.data.local.BatchScanItem
import com.albumcatalog.app.data.model.Album
import com.albumcatalog.app.data.model.MatchCandidate
import com.albumcatalog.app.ui.catalog.ManualAlbumSheet
import com.albumcatalog.app.ui.components.ConditionGradeSelector
import com.albumcatalog.app.ui.theme.TextPrimary
import com.albumcatalog.app.ui.theme.TextSecondary
import com.albumcatalog.app.ui.theme.TextTertiary
import com.albumcatalog.app.ui.theme.VinylBlack
import com.albumcatalog.app.ui.theme.VinylBorder
import com.albumcatalog.app.ui.theme.VinylCard
import com.albumcatalog.app.ui.theme.VinylDarkSurface
import com.albumcatalog.app.ui.theme.VinylGold
import com.albumcatalog.app.ui.theme.VinylGreen
import com.albumcatalog.app.ui.theme.VinylRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchReviewDeckSheet(
    items: List<BatchScanItem>,
    onSaveItem: (BatchScanItem) -> Unit,
    onDiscardItem: (Long) -> Unit,
    onSaveAllRemaining: () -> Unit,
    onReSearchItem: (id: Long, catno: String, artist: String, title: String, rawText: String) -> Unit,
    onAttachCandidate: (id: Long, candidate: MatchCandidate) -> Unit,
    onAddManualAlbum: (Album) -> Unit,
    onConvertItemToManual: (id: Long, catno: String?, artist: String, title: String, label: String?, year: Int?) -> Unit,
    onPerformSearch: suspend (String) -> List<MatchCandidate>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSavingAll by remember { mutableStateOf(false) }

    // Dialog & Sub-sheet states
    var showAddManualChoiceDialog by remember { mutableStateOf(false) }
    var showManualSearchDialog by remember { mutableStateOf(false) }
    var manualSearchTargetItemId by remember { mutableStateOf<Long?>(null) }
    var initialSearchQuery by remember { mutableStateOf("") }
    var showFullManualEntrySheet by remember { mutableStateOf(false) }
    var showOcrCorrectionSheet by remember { mutableStateOf(false) }
    var itemForOcrCorrection by remember { mutableStateOf<BatchScanItem?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(VinylDarkSurface)
            .padding(16.dp)
    ) {
        // 1. Top Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Batch Review Deck",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = if (items.isNotEmpty()) "${items.size} album${if (items.size > 1) "s" else ""} queued for review" else "Queue empty",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // "+ Add Manual" button in header
                TextButton(
                    onClick = { showAddManualChoiceDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF81D4FA), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Manual", color = Color(0xFF81D4FA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (items.size > 1) {
                    TextButton(
                        onClick = {
                            isSavingAll = true
                            onSaveAllRemaining()
                        },
                        enabled = !isSavingAll
                    ) {
                        if (isSavingAll) {
                            CircularProgressIndicator(color = VinylGold, modifier = Modifier.size(14.dp))
                        } else {
                            Icon(Icons.Default.DoneAll, contentDescription = null, tint = VinylGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save All (${items.size})", color = VinylGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Empty State
        if (items.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = VinylGreen,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "Batch Queue Empty!",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "All queued records have been saved or cleared.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = VinylGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Return to Scanner", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showAddManualChoiceDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF81D4FA)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0288D1))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Add Record Manually", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            return
        }

        // 3. Active Review Card (top of the deck)
        val current = items.first()
        var mediaCond by remember(current.id) { mutableStateOf(current.mediaCondition) }
        var sleeveCond by remember(current.id) { mutableStateOf(current.sleeveCondition) }
        var priceInput by remember(current.id) { mutableStateOf(current.purchasePrice?.toString() ?: "") }
        var shelfInput by remember(current.id) { mutableStateOf(current.shelfLocation ?: "") }
        var isRawTextExpanded by remember(current.id) { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card Deck Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(VinylCard)
                    .border(1.dp, VinylBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Item counter & status header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Record 1 of ${items.size}",
                            color = VinylGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        val statusLabel = when (current.status) {
                            "MATCHING" -> "Matching..."
                            "MATCHED" -> if (current.scanType == "MANUAL") "MANUAL ENTRY" else "SPECIFIC PRESSING"
                            "NO_MATCH" -> "NO MATCH"
                            else -> "Pending"
                        }
                        val statusColor = when (current.status) {
                            "MATCHED" -> if (current.scanType == "MANUAL") Color(0xFF81D4FA) else VinylGreen
                            "NO_MATCH" -> VinylRed
                            else -> Color(0xFFFFB74D)
                        }

                        Text(
                            text = statusLabel,
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Album Presentation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        val cand = current.matchedCandidate
                        if (cand?.thumbnailUrl != null || cand?.coverImageUrl != null) {
                            AsyncImage(
                                model = cand.thumbnailUrl ?: cand.coverImageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, VinylBorder, RoundedCornerShape(8.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF2E2E36)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (current.status == "MATCHING") {
                                    CircularProgressIndicator(color = VinylGold, modifier = Modifier.size(28.dp))
                                } else {
                                    Icon(Icons.Default.Album, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(38.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            if (cand != null) {
                                Text(
                                    text = cand.title,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = cand.artist,
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val metaParts = listOfNotNull(
                                    cand.releaseYear?.toString(),
                                    cand.label,
                                    cand.catalogNumber ?: current.detectedCatno,
                                    cand.country
                                )
                                if (metaParts.isNotEmpty()) {
                                    Text(
                                        text = metaParts.joinToString(" • "),
                                        color = TextTertiary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (cand.inCollection) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(Color(0xFFE65100).copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                                            .border(1.dp, Color(0xFFFF9800), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "IN COLLECTION",
                                            color = Color(0xFFFFB74D),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (!cand.existingCopyInfo.isNullOrEmpty()) {
                                            Text(
                                                text = " • ${cand.existingCopyInfo}",
                                                color = Color(0xFFFFE0B2),
                                                fontSize = 9.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // Option to change or refine match
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "✎ Change / Re-Search Match",
                                    color = Color(0xFF81D4FA),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable {
                                        manualSearchTargetItemId = current.id
                                        initialSearchQuery = "${cand.artist} ${cand.title}".trim()
                                        showManualSearchDialog = true
                                    }
                                )
                            } else {
                                // No match state: clearly explain scan source & what was captured
                                Text(
                                    text = if (current.scanType == "BARCODE") "Barcode (UPC/EAN)" else "Vinyl Center Label OCR",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                if (!current.detectedCatno.isNullOrEmpty()) {
                                    Text(
                                        text = "Extracted Cat #: ${current.detectedCatno}",
                                        color = Color(0xFF81D4FA),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                if (current.status == "NO_MATCH") {
                                    Text(
                                        text = "No matching release in MusicBrainz or Discogs.",
                                        color = VinylRed,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // For Unmatched scans: display the EXACT captured OCR / Barcode text in an expandable box
                    if (current.matchedCandidate == null && current.rawValue.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1E24))
                                .border(1.dp, VinylBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (current.scanType == "BARCODE") "Scanned Barcode:" else "Scanned Label Text (Captured OCR):",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (current.rawValue.contains("\n") || current.rawValue.length > 50) {
                                    Text(
                                        text = if (isRawTextExpanded) "Show less ▲" else "Show all (${current.rawValue.lines().size} lines) ▼",
                                        color = VinylGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { isRawTextExpanded = !isRawTextExpanded }
                                    )
                                }
                            }

                            Text(
                                text = current.rawValue,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = if (isRawTextExpanded) 20 else 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Correction & Search Action Buttons for this card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // "Correct OCR" button: opens OcrCorrectionSheet
                            OutlinedButton(
                                onClick = {
                                    itemForOcrCorrection = current
                                    showOcrCorrectionSheet = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = VinylGold),
                                border = androidx.compose.foundation.BorderStroke(1.dp, VinylGold)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Correct OCR", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            // "Search Release" button: opens manual query search dialog
                            Button(
                                onClick = {
                                    manualSearchTargetItemId = current.id
                                    initialSearchQuery = current.detectedCatno ?: current.rawValue.lines().firstOrNull() ?: ""
                                    showManualSearchDialog = true
                                },
                                modifier = Modifier.weight(1.1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1), contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Search Release", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Condition Selectors
                    ConditionGradeSelector(
                        label = "Media",
                        selectedGrade = mediaCond,
                        onGradeSelected = { mediaCond = it }
                    )

                    ConditionGradeSelector(
                        label = "Sleeve",
                        selectedGrade = sleeveCond,
                        onGradeSelected = { sleeveCond = it }
                    )

                    // Price & Shelf Fields
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = priceInput,
                            onValueChange = { priceInput = it },
                            label = { Text("Price Paid ($)", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = shelfInput,
                            onValueChange = { shelfInput = it },
                            label = { Text("Shelf / Crate", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Primary Decision Actions: Discard vs Save
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Discard Button
                OutlinedButton(
                    onClick = { onDiscardItem(current.id) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VinylRed),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VinylRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Discard", fontWeight = FontWeight.SemiBold)
                }

                // Save Button
                Button(
                    onClick = {
                        val updated = current.copy(
                            mediaCondition = mediaCond,
                            sleeveCondition = sleeveCond,
                            purchasePrice = priceInput.toDoubleOrNull(),
                            shelfLocation = shelfInput.ifBlank { null }
                        )
                        onSaveItem(updated)
                    },
                    modifier = Modifier.weight(1.4f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (current.matchedCandidate != null) VinylGreen else VinylGold,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    enabled = current.status != "MATCHING"
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to Catalog", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // 5. Choice Dialog when tapping "+ Add Manual" in the header
    if (showAddManualChoiceDialog) {
        AlertDialog(
            onDismissRequest = { showAddManualChoiceDialog = false },
            containerColor = VinylCard,
            title = {
                Text("Add Record to Batch", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Choose how you want to add this vinyl record to your crate-digging batch:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    // Option 1: Search MusicBrainz & Discogs
                    Card(
                        onClick = {
                            showAddManualChoiceDialog = false
                            manualSearchTargetItemId = null
                            initialSearchQuery = ""
                            showManualSearchDialog = true
                        },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E26)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0288D1)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF81D4FA), modifier = Modifier.size(24.dp))
                            Column {
                                Text("Search Online Databases", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Search by artist, album, or catalog number", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }

                    // Option 2: Fill Manual Entry Form
                    Card(
                        onClick = {
                            showAddManualChoiceDialog = false
                            showFullManualEntrySheet = true
                        },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E26)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, VinylGold),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Album, contentDescription = null, tint = VinylGold, modifier = Modifier.size(24.dp))
                            Column {
                                Text("Full Manual Entry Form", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Enter custom title, tracklist, and private press info", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddManualChoiceDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // 6. Interactive Manual Search Dialog
    if (showManualSearchDialog) {
        BatchManualSearchDialog(
            initialQuery = initialSearchQuery,
            isAttachingToExisting = manualSearchTargetItemId != null,
            onPerformSearch = onPerformSearch,
            onSelectCandidate = { candidate ->
                val targetId = manualSearchTargetItemId
                if (targetId != null) {
                    onAttachCandidate(targetId, candidate)
                } else {
                    val album = Album(
                        title = candidate.title,
                        artist = candidate.artist,
                        releaseYear = candidate.releaseYear,
                        label = candidate.label,
                        catalogNumber = candidate.catalogNumber,
                        barcode = candidate.barcode,
                        format = candidate.format ?: "Vinyl, LP",
                        country = candidate.country,
                        pressingStatus = "specific",
                        metadataSource = candidate.source,
                        coverImageUrl = candidate.coverImageUrl,
                        thumbnailUrl = candidate.thumbnailUrl,
                        musicbrainzReleaseId = candidate.musicbrainzReleaseId,
                        discogsReleaseId = candidate.discogsReleaseId,
                        discogsMasterId = candidate.discogsMasterId,
                        mediaCondition = "VG+",
                        sleeveCondition = "VG+",
                        tracks = candidate.tracks
                    )
                    onAddManualAlbum(album)
                }
                showManualSearchDialog = false
            },
            onSwitchToFullManual = {
                showManualSearchDialog = false
                showFullManualEntrySheet = true
            },
            onDismiss = { showManualSearchDialog = false }
        )
    }

    // 7. OCR Correction Sheet
    if (showOcrCorrectionSheet && itemForOcrCorrection != null) {
        val target = itemForOcrCorrection!!
        val ocrSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showOcrCorrectionSheet = false },
            sheetState = ocrSheetState,
            containerColor = VinylBlack
        ) {
            OcrCorrectionSheet(
                initialCatno = target.detectedCatno ?: "",
                initialArtist = target.matchedCandidate?.artist ?: "",
                initialTitle = target.matchedCandidate?.title ?: "",
                initialRawText = target.rawValue,
                scanType = target.scanType,
                onReSearch = { catno, artist, title, rawText ->
                    onReSearchItem(target.id, catno, artist, title, rawText)
                    showOcrCorrectionSheet = false
                },
                onCreateManual = { catno, artist, title, label, year ->
                    onConvertItemToManual(target.id, catno, artist, title, label, year)
                    showOcrCorrectionSheet = false
                },
                onDismiss = { showOcrCorrectionSheet = false }
            )
        }
    }

    // 8. Full Manual Album Entry Form
    if (showFullManualEntrySheet) {
        val manualSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showFullManualEntrySheet = false },
            sheetState = manualSheetState,
            containerColor = VinylBlack
        ) {
            ManualAlbumSheet(
                onSaveAlbum = { album ->
                    onAddManualAlbum(album)
                    showFullManualEntrySheet = false
                },
                onDismiss = { showFullManualEntrySheet = false }
            )
        }
    }
}

/**
 * Dedicated dialog for searching releases across Local MusicBrainz, Remote MusicBrainz & Discogs
 * and either linking to an existing batch scan item or adding as a new record to the batch.
 */
@Composable
fun BatchManualSearchDialog(
    initialQuery: String,
    isAttachingToExisting: Boolean,
    onPerformSearch: suspend (String) -> List<MatchCandidate>,
    onSelectCandidate: (MatchCandidate) -> Unit,
    onSwitchToFullManual: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf(initialQuery) }
    var isSearching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<MatchCandidate>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(VinylCard)
                .border(1.5.dp, Color(0xFF0288D1), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isAttachingToExisting) "Search & Link Release" else "Search Release to Add",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Searches MusicBrainz & Discogs database",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Artist, album title, or catalog #...", fontSize = 12.sp, color = TextSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (searchQuery.isNotBlank()) {
                        isSearching = true
                        scope.launch {
                            results = onPerformSearch(searchQuery.trim())
                            isSearching = false
                            hasSearched = true
                        }
                    }
                }),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                isSearching = true
                                scope.launch {
                                    results = onPerformSearch(searchQuery.trim())
                                    isSearching = false
                                    hasSearched = true
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = VinylGold)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VinylGold,
                    unfocusedBorderColor = VinylBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            // Content Area: Searching, Results, or Empty State
            if (isSearching) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = VinylGold, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Searching databases...", color = TextSecondary, fontSize = 13.sp)
                }
            } else if (results.isNotEmpty()) {
                Text(
                    text = "Select matching release:",
                    color = VinylGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results) { candidate ->
                        Card(
                            onClick = { onSelectCandidate(candidate) },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E26)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VinylBorder),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (candidate.thumbnailUrl != null || candidate.coverImageUrl != null) {
                                    AsyncImage(
                                        model = candidate.thumbnailUrl ?: candidate.coverImageUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF2E2E36)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Album, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(28.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = candidate.title,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = candidate.artist,
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val parts = listOfNotNull(
                                        candidate.releaseYear?.toString(),
                                        candidate.label,
                                        candidate.catalogNumber
                                    )
                                    if (parts.isNotEmpty()) {
                                        Text(
                                            text = parts.joinToString(" • "),
                                            color = TextTertiary,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Button(
                                    onClick = { onSelectCandidate(candidate) },
                                    colors = ButtonDefaults.buttonColors(containerColor = VinylGreen, contentColor = Color.Black),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("Select", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            } else if (hasSearched) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "No releases found for \"$searchQuery\"",
                        color = VinylRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Try searching by artist name or broad album title.",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
            } else {
                Text(
                    text = "Enter artist, title, or catalog number above and tap search.",
                    color = TextTertiary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            // Bottom Alternative: Enter Full Manual
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onSwitchToFullManual) {
                    Text("Can't find it? Enter details manually ✎", color = Color(0xFF81D4FA), fontSize = 11.sp)
                }

                TextButton(onClick = onDismiss) {
                    Text("Close", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}
