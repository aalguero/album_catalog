package com.albumcatalog.app.ui.catalog

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.albumcatalog.app.data.model.Album
import com.albumcatalog.app.data.model.DiscogsPressingOption
import com.albumcatalog.app.ui.components.ConditionGradeSelector
import com.albumcatalog.app.ui.theme.TextPrimary
import com.albumcatalog.app.ui.theme.TextSecondary
import com.albumcatalog.app.ui.theme.TextTertiary
import com.albumcatalog.app.ui.theme.VinylBorder
import com.albumcatalog.app.ui.theme.VinylCard
import com.albumcatalog.app.ui.theme.VinylDarkSurface
import com.albumcatalog.app.ui.theme.VinylGold
import com.albumcatalog.app.ui.theme.VinylGreen
import kotlinx.coroutines.launch

@Composable
fun EditAlbumDialog(
    album: Album,
    onSave: (Album) -> Unit,
    onDismiss: () -> Unit,
    fetchPressings: suspend (artist: String, title: String, catno: String?, barcode: String?) -> List<DiscogsPressingOption>
) {
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf(album.title) }
    var artist by remember { mutableStateOf(album.artist) }
    var releaseYear by remember { mutableStateOf(album.releaseYear?.toString() ?: "") }
    var label by remember { mutableStateOf(album.label ?: "") }
    var catalogNumber by remember { mutableStateOf(album.catalogNumber ?: "") }
    var country by remember { mutableStateOf(album.country ?: "") }
    var pressingStatus by remember { mutableStateOf(album.pressingStatus) }
    var discogsReleaseId by remember { mutableStateOf(album.discogsReleaseId) }

    var mediaCondition by remember { mutableStateOf(album.mediaCondition ?: "VG+") }
    var sleeveCondition by remember { mutableStateOf(album.sleeveCondition ?: "VG+") }
    var shelfLocation by remember { mutableStateOf(album.shelfLocation ?: "") }
    var purchasePrice by remember { mutableStateOf(album.purchasePrice?.toString() ?: "") }
    var notes by remember { mutableStateOf(album.notes ?: "") }

    var isFetchingPressings by remember { mutableStateOf(false) }
    var availablePressings by remember { mutableStateOf<List<DiscogsPressingOption>?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().background(VinylDarkSurface, RoundedCornerShape(16.dp)),
        title = {
            Text("Edit Catalog Entry", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pressing status & Upgrade button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                (if (pressingStatus == "specific") VinylGreen else Color(0xFFB0BEC5)).copy(alpha = 0.15f)
                            )
                            .clickable {
                                pressingStatus = if (pressingStatus == "specific") "unspecific" else "specific"
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status: ${pressingStatus.uppercase()} ✎",
                            color = if (pressingStatus == "specific") VinylGreen else Color(0xFFB0BEC5),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    if (pressingStatus == "unspecific") {
                        OutlinedButton(
                            onClick = {
                                isFetchingPressings = true
                                coroutineScope.launch {
                                    val list = fetchPressings(artist, title, catalogNumber.ifEmpty { null }, album.barcode)
                                    availablePressings = list
                                    isFetchingPressings = false
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isFetchingPressings) {
                                CircularProgressIndicator(color = VinylGold, modifier = Modifier.size(16.dp))
                            } else {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.padding(start = 4.dp))
                                Text("Refine Pressing", fontSize = 11.sp, color = VinylGold)
                            }
                        }
                    }
                }

                // If pressing options are loaded, show list or empty message
                availablePressings?.let { pressings ->
                    if (pressings.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1E24))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("No alternate pressings found on Discogs.", color = TextSecondary, fontSize = 12.sp)
                            Button(
                                onClick = {
                                    pressingStatus = "specific"
                                    availablePressings = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = VinylGreen, contentColor = Color.Black),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Mark as Specific Pressing", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text("Select Specific Discogs Pressing:", fontSize = 12.sp, color = VinylGold, fontWeight = FontWeight.Bold)
                        LazyColumn(modifier = Modifier.height(180.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(pressings) { p ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(VinylCard)
                                        .border(1.dp, VinylBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            releaseYear = p.year?.toString() ?: releaseYear
                                            label = p.label ?: label
                                            catalogNumber = p.catalogNumber ?: catalogNumber
                                            country = p.country ?: country
                                            discogsReleaseId = p.discogsId
                                            pressingStatus = "specific"
                                            availablePressings = null
                                        }
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Text("${p.year ?: "N/A"} • ${p.country ?: "N/A"} • ${p.label ?: "N/A"}", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(p.formatDescription ?: "Vinyl", color = TextSecondary, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("Artist") }, modifier = Modifier.fillMaxWidth())

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = releaseYear, onValueChange = { releaseYear = it }, label = { Text("Year") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = country, onValueChange = { country = it }, label = { Text("Country") }, modifier = Modifier.weight(1f))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = catalogNumber, onValueChange = { catalogNumber = it }, label = { Text("Cat #") }, modifier = Modifier.weight(1f))
                }

                ConditionGradeSelector(label = "Media", selectedGrade = mediaCondition, onGradeSelected = { mediaCondition = it })
                ConditionGradeSelector(label = "Sleeve", selectedGrade = sleeveCondition, onGradeSelected = { sleeveCondition = it })

                OutlinedTextField(value = shelfLocation, onValueChange = { shelfLocation = it }, label = { Text("Shelf / Crate") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = purchasePrice, onValueChange = { purchasePrice = it }, label = { Text("Price ($)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = album.copy(
                        title = title.trim(),
                        artist = artist.trim(),
                        releaseYear = releaseYear.toIntOrNull(),
                        label = label.ifEmpty { null },
                        catalogNumber = catalogNumber.ifEmpty { null },
                        country = country.ifEmpty { null },
                        pressingStatus = pressingStatus,
                        discogsReleaseId = discogsReleaseId,
                        mediaCondition = mediaCondition,
                        sleeveCondition = sleeveCondition,
                        shelfLocation = shelfLocation.ifEmpty { null },
                        purchasePrice = purchasePrice.toDoubleOrNull(),
                        notes = notes.ifEmpty { null }
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = VinylGold, contentColor = Color.Black)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
