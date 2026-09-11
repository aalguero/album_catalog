package com.albumcatalog.app.ui.catalog

import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.albumcatalog.app.data.model.Album
import com.albumcatalog.app.data.model.TrackItem
import com.albumcatalog.app.ui.components.ConditionGradeSelector
import com.albumcatalog.app.ui.theme.TextPrimary
import com.albumcatalog.app.ui.theme.TextSecondary
import com.albumcatalog.app.ui.theme.VinylBorder
import com.albumcatalog.app.ui.theme.VinylCard
import com.albumcatalog.app.ui.theme.VinylGold

data class EditableTrack(
    var position: String = "",
    var title: String = "",
    var duration: String = ""
)

@Composable
fun ManualAlbumSheet(
    onSaveAlbum: (Album) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var releaseYear by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var catalogNumber by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var format by remember { mutableStateOf("Vinyl, LP") }
    var country by remember { mutableStateOf("US") }
    var purchasePrice by remember { mutableStateOf("") }
    var purchaseStore by remember { mutableStateOf("") }
    var mediaCondition by remember { mutableStateOf("VG+") }
    var sleeveCondition by remember { mutableStateOf("VG+") }
    var notes by remember { mutableStateOf("") }

    val trackList = remember { mutableStateListOf<EditableTrack>() }
    var showTracklistSection by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(VinylGold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = VinylGold, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text(
                        text = "Add Album Manually",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "For uncataloged, bootleg, or private vinyl pressings",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
            }
        }

        // Section 1: Core Album Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(VinylCard)
                .border(1.dp, VinylBorder, RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "ALBUM INFORMATION",
                color = VinylGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // Title (Required)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Album Title *") },
                placeholder = { Text("e.g. Kind of Blue") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VinylGold,
                    unfocusedBorderColor = VinylBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            // Artist (Required)
            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                label = { Text("Artist *") },
                placeholder = { Text("e.g. Miles Davis") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VinylGold,
                    unfocusedBorderColor = VinylBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            // Release Year & Record Label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = releaseYear,
                    onValueChange = { releaseYear = it },
                    label = { Text("Year") },
                    placeholder = { Text("1959") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.8f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VinylGold,
                        unfocusedBorderColor = VinylBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Record Label") },
                    placeholder = { Text("Columbia") },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VinylGold,
                        unfocusedBorderColor = VinylBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }

            // Catalog Number & Barcode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = catalogNumber,
                    onValueChange = { catalogNumber = it },
                    label = { Text("Catalog #") },
                    placeholder = { Text("CL 1355") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VinylGold,
                        unfocusedBorderColor = VinylBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Barcode (UPC)") },
                    placeholder = { Text("Optional") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VinylGold,
                        unfocusedBorderColor = VinylBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }

            // Format & Country
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = format,
                    onValueChange = { format = it },
                    label = { Text("Format") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VinylGold,
                        unfocusedBorderColor = VinylBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("Country") },
                    placeholder = { Text("US") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VinylGold,
                        unfocusedBorderColor = VinylBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        }

        // Section 2: Condition & Acquisition Details
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(VinylCard)
                .border(1.dp, VinylBorder, RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "CONDITION & PURCHASE",
                color = VinylGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // Price Paid & Store
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = purchasePrice,
                    onValueChange = { purchasePrice = it },
                    label = { Text("Price Paid") },
                    placeholder = { Text("25.00") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = VinylGold) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VinylGold,
                        unfocusedBorderColor = VinylBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                OutlinedTextField(
                    value = purchaseStore,
                    onValueChange = { purchaseStore = it },
                    label = { Text("Purchase Store") },
                    placeholder = { Text("Amoeba, Record Fair") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VinylGold,
                        unfocusedBorderColor = VinylBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }

            // Goldmine Condition Selectors
            ConditionGradeSelector(
                label = "Media",
                selectedGrade = mediaCondition,
                onGradeSelected = { mediaCondition = it }
            )

            ConditionGradeSelector(
                label = "Sleeve",
                selectedGrade = sleeveCondition,
                onGradeSelected = { sleeveCondition = it }
            )

            // Collector Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Collector Notes (Optional)") },
                placeholder = { Text("e.g. 1st pressing etched runout, shrink intact, includes poster") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VinylGold,
                    unfocusedBorderColor = VinylBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }

        // Section 3: Optional Tracklist Builder
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(VinylCard)
                .border(1.dp, VinylBorder, RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.QueueMusic, contentDescription = null, tint = VinylGold, modifier = Modifier.size(16.dp))
                    Text(
                        text = "TRACKLIST (${trackList.size} tracks)",
                        color = VinylGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = if (showTracklistSection) "Hide ▲" else "Expand ▼",
                    color = Color(0xFF81D4FA),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { showTracklistSection = !showTracklistSection }
                )
            }

            if (showTracklistSection) {
                trackList.forEachIndexed { index, track ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = track.position,
                            onValueChange = { trackList[index] = track.copy(position = it) },
                            label = { Text("Pos") },
                            placeholder = { Text("A1") },
                            singleLine = true,
                            modifier = Modifier.width(60.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        )
                        OutlinedTextField(
                            value = track.title,
                            onValueChange = { trackList[index] = track.copy(title = it) },
                            label = { Text("Title") },
                            placeholder = { Text("Track title") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                        )
                        OutlinedTextField(
                            value = track.duration,
                            onValueChange = { trackList[index] = track.copy(duration = it) },
                            label = { Text("Dur") },
                            placeholder = { Text("4:15") },
                            singleLine = true,
                            modifier = Modifier.width(72.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        )
                        IconButton(
                            onClick = { trackList.removeAt(index) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Track", tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Button(
                    onClick = {
                        val nextPos = when (trackList.size) {
                            in 0..4 -> "A${trackList.size + 1}"
                            in 5..9 -> "B${trackList.size - 4}"
                            else -> "${trackList.size + 1}"
                        }
                        trackList.add(EditableTrack(position = nextPos, title = "", duration = ""))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C36), contentColor = TextPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Add Track", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Action Buttons: Save & Cancel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel", color = TextSecondary, fontSize = 13.sp)
            }

            Button(
                onClick = {
                    if (title.isBlank() || artist.isBlank()) {
                        Toast.makeText(context, "Please enter both Album Title and Artist", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val priceVal = purchasePrice.toDoubleOrNull()
                    val yearVal = releaseYear.toIntOrNull()

                    val tracks = trackList
                        .filter { it.title.isNotBlank() }
                        .map {
                            TrackItem(
                                position = it.position.ifBlank { null },
                                title = it.title.trim(),
                                duration = it.duration.ifBlank { null }
                            )
                        }

                    val manualAlbum = Album(
                        title = title.trim(),
                        artist = artist.trim(),
                        releaseYear = yearVal,
                        label = label.ifBlank { null },
                        catalogNumber = catalogNumber.ifBlank { null },
                        barcode = barcode.ifBlank { null },
                        format = format.ifBlank { "Vinyl, LP" },
                        country = country.ifBlank { null },
                        pressingStatus = "unspecific",
                        metadataSource = "manual",
                        mediaCondition = mediaCondition,
                        sleeveCondition = sleeveCondition,
                        purchasePrice = priceVal,
                        purchaseStore = purchaseStore.ifBlank { null },
                        notes = notes.ifBlank { null },
                        tracks = tracks
                    )

                    onSaveAlbum(manualAlbum)
                },
                modifier = Modifier.weight(1.6f),
                colors = ButtonDefaults.buttonColors(containerColor = VinylGold, contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Album, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save to Collection", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
