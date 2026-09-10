package com.albumcatalog.app.ui.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.albumcatalog.app.data.model.Album
import com.albumcatalog.app.data.model.DiscogsPressingOption
import com.albumcatalog.app.data.model.MatchCandidate
import com.albumcatalog.app.ui.components.ConditionGradeSelector
import com.albumcatalog.app.ui.theme.TextPrimary
import com.albumcatalog.app.ui.theme.TextSecondary
import com.albumcatalog.app.ui.theme.VinylDarkSurface
import com.albumcatalog.app.ui.theme.VinylGold

@Composable
fun SaveAlbumDialog(
    candidate: MatchCandidate,
    discogsPressing: DiscogsPressingOption?,
    onSave: (Album) -> Unit,
    onDismiss: () -> Unit
) {
    var mediaCondition by remember { mutableStateOf("VG+") }
    var sleeveCondition by remember { mutableStateOf("VG+") }
    var shelfLocation by remember { mutableStateOf("Main Crate") }
    var purchasePrice by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().background(VinylDarkSurface, RoundedCornerShape(16.dp)),
        title = {
            Column {
                Text(
                    text = "Add to Collection",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${candidate.artist} - ${candidate.title}",
                    color = VinylGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pressing status info
                Text(
                    text = if (discogsPressing != null)
                        "Pressing: Specific (${discogsPressing.year ?: "N/A"} - ${discogsPressing.country ?: "N/A"})"
                    else
                        "Pressing: Unspecific (Preliminary Match)",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                // Condition selectors
                ConditionGradeSelector(
                    label = "Media",
                    selectedGrade = mediaCondition,
                    onGradeSelected = { mediaCondition = it }
                )

                ConditionGradeSelector(
                    label = "Sleeve / Jacket",
                    selectedGrade = sleeveCondition,
                    onGradeSelected = { sleeveCondition = it }
                )

                OutlinedTextField(
                    value = shelfLocation,
                    onValueChange = { shelfLocation = it },
                    label = { Text("Shelf / Crate Location") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = purchasePrice,
                    onValueChange = { purchasePrice = it },
                    label = { Text("Purchase Price ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (e.g. Mastered by Bob Ludwig)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val albumToSave = Album(
                        title = candidate.title,
                        artist = candidate.artist,
                        releaseYear = discogsPressing?.year ?: candidate.releaseYear,
                        label = discogsPressing?.label ?: candidate.label,
                        catalogNumber = discogsPressing?.catalogNumber ?: candidate.catalogNumber,
                        barcode = candidate.barcode,
                        country = discogsPressing?.country ?: candidate.country,
                        format = candidate.format,
                        pressingStatus = if (discogsPressing != null) "specific" else "unspecific",
                        metadataSource = if (discogsPressing != null) "discogs" else candidate.source,
                        musicbrainzReleaseId = candidate.musicbrainzReleaseId,
                        discogsReleaseId = discogsPressing?.discogsId ?: candidate.discogsReleaseId,
                        discogsMasterId = candidate.discogsMasterId,
                        coverImageUrl = candidate.coverImageUrl ?: discogsPressing?.coverImage,
                        thumbnailUrl = candidate.thumbnailUrl,
                        mediaCondition = mediaCondition,
                        sleeveCondition = sleeveCondition,
                        purchasePrice = purchasePrice.toDoubleOrNull(),
                        shelfLocation = shelfLocation.ifEmpty { null },
                        notes = notes.ifEmpty { null },
                        tracks = candidate.tracks
                    )
                    onSave(albumToSave)
                },
                colors = ButtonDefaults.buttonColors(containerColor = VinylGold, contentColor = Color.Black)
            ) {
                Text("Confirm & Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
