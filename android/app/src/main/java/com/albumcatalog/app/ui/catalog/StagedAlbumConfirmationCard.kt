package com.albumcatalog.app.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.albumcatalog.app.data.model.Album
import com.albumcatalog.app.ui.components.ConditionGradeSelector
import com.albumcatalog.app.ui.theme.TextPrimary
import com.albumcatalog.app.ui.theme.TextSecondary
import com.albumcatalog.app.ui.theme.TextTertiary
import com.albumcatalog.app.ui.theme.VinylBorder
import com.albumcatalog.app.ui.theme.VinylCard
import com.albumcatalog.app.ui.theme.VinylGold
import com.albumcatalog.app.ui.theme.VinylGreen

@Composable
fun StagedAlbumConfirmationCard(
    album: Album,
    onConfirm: (Album) -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mediaCondition by remember { mutableStateOf(album.mediaCondition ?: "VG+") }
    var sleeveCondition by remember { mutableStateOf(album.sleeveCondition ?: "VG+") }
    var purchasePrice by remember { mutableStateOf(album.purchasePrice?.toString() ?: "") }
    var notes by remember { mutableStateOf(album.notes ?: "") }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(VinylCard)
            .border(2.dp, VinylGold, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Confirm Scanned Album",
                    color = VinylGold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = album.pressingStatus.uppercase(),
                    color = if (album.pressingStatus == "specific") VinylGreen else Color(0xFFB0BEC5),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            (if (album.pressingStatus == "specific") VinylGreen else Color(0xFFB0BEC5)).copy(alpha = 0.2f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Row(verticalAlignment = Alignment.Top) {
                if (!album.thumbnailUrl.isNullOrEmpty() || !album.coverImageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = album.thumbnailUrl ?: album.coverImageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2E2E36)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Album, contentDescription = null, tint = TextTertiary)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(album.title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(album.artist, color = VinylGold, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    val meta = listOfNotNull(
                        album.releaseYear?.toString(),
                        album.label,
                        album.catalogNumber,
                        album.country
                    )
                    Text(meta.joinToString(" • "), color = TextSecondary, fontSize = 12.sp)
                }
            }

            // Condition Selectors
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

            // Price Paid (Prominent & always asked when cataloging)
            OutlinedTextField(
                value = purchasePrice,
                onValueChange = { purchasePrice = it },
                label = { Text("Price Paid ($) *", fontWeight = FontWeight.SemiBold) },
                placeholder = { Text("e.g. 24.99") },
                leadingIcon = {
                    Icon(Icons.Default.AttachMoney, contentDescription = null, tint = VinylGold)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VinylGold,
                    unfocusedBorderColor = VinylBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            // Confirmation Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val finalized = album.copy(
                            mediaCondition = mediaCondition,
                            sleeveCondition = sleeveCondition,
                            purchasePrice = purchasePrice.toDoubleOrNull(),
                            notes = notes.ifEmpty { null }
                        )
                        onConfirm(finalized)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = VinylGold, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Confirm & Add", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350))
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reject & Scan", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
