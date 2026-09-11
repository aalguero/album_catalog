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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.albumcatalog.app.ui.theme.TextPrimary
import com.albumcatalog.app.ui.theme.TextSecondary
import com.albumcatalog.app.ui.theme.VinylBorder
import com.albumcatalog.app.ui.theme.VinylGold

@Composable
fun OcrCorrectionSheet(
    initialCatno: String,
    initialArtist: String,
    initialTitle: String,
    initialRawText: String,
    scanType: String,
    onReSearch: (catno: String, artist: String, title: String, rawText: String) -> Unit,
    onCreateManual: (catno: String, artist: String, title: String, label: String?, year: Int?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var catno by remember(initialCatno) { mutableStateOf(initialCatno) }
    var artist by remember(initialArtist) { mutableStateOf(initialArtist) }
    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    var rawText by remember(initialRawText) { mutableStateOf(initialRawText) }
    var label by remember { mutableStateOf("") }
    var yearStr by remember { mutableStateOf("") }
    var showRawText by remember { mutableStateOf(true) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Title & Dismiss
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Correct Scanned Metadata",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Fix OCR typos to re-query MusicBrainz & Discogs or save manually.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
            }
        }

        // Scan Type Pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF26262E))
                .border(1.dp, VinylBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Source: $scanType",
                color = VinylGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // 1. Catalog Number field
        OutlinedTextField(
            value = catno,
            onValueChange = { catno = it },
            label = { Text("Catalog Number") },
            placeholder = { Text("e.g. SST 003, BST-84154, PC 34474") },
            leadingIcon = {
                Icon(Icons.Default.Tag, contentDescription = null, tint = Color(0xFF81D4FA))
            },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            ),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF81D4FA),
                unfocusedBorderColor = VinylBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        // 2. Artist field
        OutlinedTextField(
            value = artist,
            onValueChange = { artist = it },
            label = { Text("Artist") },
            placeholder = { Text("e.g. Black Flag, Miles Davis") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VinylGold,
                unfocusedBorderColor = VinylBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        // 3. Album Title field
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Album Title") },
            placeholder = { Text("e.g. Jealous Again, Kind of Blue") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VinylGold,
                unfocusedBorderColor = VinylBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        // 4. Optional Label & Year row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Record Label (Optional)") },
                placeholder = { Text("e.g. SST Records, Blue Note") },
                singleLine = true,
                modifier = Modifier.weight(1.4f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VinylGold,
                    unfocusedBorderColor = VinylBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            OutlinedTextField(
                value = yearStr,
                onValueChange = { yearStr = it },
                label = { Text("Year") },
                placeholder = { Text("1980") },
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
        }

        // 5. Raw Scanned OCR Text (for reference & fine-tuning)
        if (rawText.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1B1B22))
                    .border(1.dp, VinylBorder, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scanned Label Text (OCR Reference)",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (showRawText) "Collapse" else "Expand",
                        color = VinylGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { showRawText = !showRawText }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                if (showRawText) {
                    OutlinedTextField(
                        value = rawText,
                        onValueChange = { rawText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 70.dp, max = 140.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TextSecondary
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VinylBorder,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextSecondary,
                            unfocusedTextColor = TextSecondary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 6. Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Re-search Button
            Button(
                onClick = {
                    onReSearch(catno, artist, title, rawText)
                },
                modifier = Modifier.weight(1.2f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VinylGold,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Search Again", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            // Create Manual Album Button
            OutlinedButton(
                onClick = {
                    onCreateManual(catno, artist, title, label.ifBlank { null }, yearStr.toIntOrNull())
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF81D4FA)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0288D1))
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save Manual", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
