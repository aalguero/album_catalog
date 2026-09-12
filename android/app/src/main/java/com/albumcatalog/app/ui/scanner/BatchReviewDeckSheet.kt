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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.albumcatalog.app.data.local.BatchScanItem
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

@Composable
fun BatchReviewDeckSheet(
    items: List<BatchScanItem>,
    onSaveItem: (BatchScanItem) -> Unit,
    onDiscardItem: (Long) -> Unit,
    onSaveAllRemaining: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSavingAll by remember { mutableStateOf(false) }

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
            Column {
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

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    .padding(vertical = 40.dp),
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
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = VinylGold, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Return to Scanner", fontWeight = FontWeight.Bold)
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
                            "MATCHED" -> "SPECIFIC PRESSING"
                            "NO_MATCH" -> "No Match"
                            else -> "Pending"
                        }
                        val statusColor = when (current.status) {
                            "MATCHED" -> VinylGreen
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
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, VinylBorder, RoundedCornerShape(8.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF2E2E36)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (current.status == "MATCHING") {
                                    CircularProgressIndicator(color = VinylGold, modifier = Modifier.size(28.dp))
                                } else {
                                    Icon(Icons.Default.Album, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(36.dp))
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
                            } else {
                                Text(
                                    text = if (current.scanType == "BARCODE") "Barcode: ${current.rawValue}" else "Vinyl Center Label",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (current.detectedCatno != null) {
                                    Text(
                                        text = "Cat #: ${current.detectedCatno}",
                                        color = Color(0xFF81D4FA),
                                        fontSize = 12.sp,
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
}
