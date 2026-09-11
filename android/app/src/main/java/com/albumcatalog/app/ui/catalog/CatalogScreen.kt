package com.albumcatalog.app.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.albumcatalog.app.data.model.Album
import com.albumcatalog.app.ui.theme.TextPrimary
import com.albumcatalog.app.ui.theme.TextSecondary
import com.albumcatalog.app.ui.theme.TextTertiary
import com.albumcatalog.app.ui.theme.VinylBlack
import com.albumcatalog.app.ui.theme.VinylBorder
import com.albumcatalog.app.ui.theme.VinylCard
import com.albumcatalog.app.ui.theme.VinylGold
import com.albumcatalog.app.ui.theme.VinylGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    stagedAlbum: Album? = null,
    onConfirmStagedAlbum: () -> Unit = {},
    onRejectStagedAlbum: () -> Unit = {},
    viewModel: CatalogViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val pressingFilter by viewModel.pressingFilter.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedAlbumForDetails by remember { mutableStateOf<Album?>(null) }
    var albumToEdit by remember { mutableStateOf<Album?>(null) }
    var isManualEntryOpen by remember { mutableStateOf(false) }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(VinylBlack)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 44.dp, start = 16.dp, end = 16.dp)
        ) {
            Text(
                text = "Vinyl Collection",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Staged Album Confirmation Card (from scanning)
            if (stagedAlbum != null) {
                StagedAlbumConfirmationCard(
                    album = stagedAlbum,
                    onConfirm = { confirmedAlbum ->
                        viewModel.addConfirmedAlbum(confirmedAlbum, onConfirmStagedAlbum)
                    },
                    onReject = onRejectStagedAlbum,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }

            // 2. Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Filter collection by artist, title, or label...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = VinylGold) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Filter chips (All / Specific / Unspecific)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = pressingFilter == null,
                    onClick = { viewModel.setPressingFilter(null) },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VinylGold,
                        selectedLabelColor = Color.Black
                    )
                )
                FilterChip(
                    selected = pressingFilter == "specific",
                    onClick = { viewModel.setPressingFilter("specific") },
                    label = { Text("Specific Pressings") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VinylGreen,
                        selectedLabelColor = Color.Black
                    )
                )
                FilterChip(
                    selected = pressingFilter == "unspecific",
                    onClick = { viewModel.setPressingFilter("unspecific") },
                    label = { Text("Unspecific") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF9E9E9E),
                        selectedLabelColor = Color.Black
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Collection List
            when (val state = uiState) {
                is CatalogUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = VinylGold)
                    }
                }
                is CatalogUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = Color(0xFFEF5350), fontSize = 14.sp)
                    }
                }
                is CatalogUiState.Success -> {
                    if (state.albums.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No albums in collection yet.\nUse the Scan tab to catalog your first record!",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.albums) { album ->
                                AlbumCatalogItem(
                                    album = album,
                                    onClick = { selectedAlbumForDetails = album },
                                    onEdit = { albumToEdit = album },
                                    onDelete = { album.id?.let { viewModel.deleteAlbum(it) } }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Album Details Bottom Sheet
        selectedAlbumForDetails?.let { album ->
            ModalBottomSheet(
                onDismissRequest = { selectedAlbumForDetails = null },
                containerColor = VinylBlack
            ) {
                AlbumDetailContent(
                    album = album,
                    onEdit = {
                        selectedAlbumForDetails = null
                        albumToEdit = album
                    },
                    onDismiss = { selectedAlbumForDetails = null }
                )
            }
        }

        // Edit Album Dialog
        albumToEdit?.let { album ->
            EditAlbumDialog(
                album = album,
                onSave = { updated ->
                    album.id?.let { id ->
                        viewModel.updateAlbum(id, updated) {
                            albumToEdit = null
                        }
                    }
                },
                onDismiss = { albumToEdit = null },
                fetchPressings = { art, tit, cat, bar ->
                    viewModel.fetchDiscogsPressings(art, tit, cat, bar)
                }
            )
        }

        // Floating Action Button: Add Album Manually ('+' FAB)
        FloatingActionButton(
            onClick = { isManualEntryOpen = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 20.dp, end = 20.dp),
            containerColor = VinylGold,
            contentColor = Color.Black,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Album Manually", modifier = Modifier.size(28.dp))
        }

        // Manual Album Entry Modal Bottom Sheet
        if (isManualEntryOpen) {
            val manualSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { isManualEntryOpen = false },
                sheetState = manualSheetState,
                containerColor = VinylBlack
            ) {
                ManualAlbumSheet(
                    onSaveAlbum = { manualAlbum ->
                        viewModel.addConfirmedAlbum(manualAlbum) {
                            isManualEntryOpen = false
                        }
                    },
                    onDismiss = { isManualEntryOpen = false }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }
}

@Composable
fun AlbumCatalogItem(
    album: Album,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VinylCard)
            .border(1.dp, VinylBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!album.thumbnailUrl.isNullOrEmpty() || !album.coverImageUrl.isNullOrEmpty()) {
            AsyncImage(
                model = album.thumbnailUrl ?: album.coverImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2E2E36)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Album, contentDescription = null, tint = TextTertiary)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = album.artist,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Pressing Badge
                Text(
                    text = album.pressingStatus.uppercase(),
                    color = if (album.pressingStatus == "specific") VinylGreen else Color(0xFFB0BEC5),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            (if (album.pressingStatus == "specific") VinylGreen else Color(0xFFB0BEC5)).copy(alpha = 0.15f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )

                // Condition Badge
                album.mediaCondition?.let {
                    Text(
                        text = "Media: $it",
                        color = VinylGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(VinylGold.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // Price Paid Badge (replaces shelf/crate indicator)
                album.purchasePrice?.let { price ->
                    Text(
                        text = "$%.2f".format(price),
                        color = Color(0xFF81D4FA),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color(0xFF81D4FA).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Row {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = VinylGold)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextTertiary)
            }
        }
    }
}

@Composable
fun AlbumDetailContent(
    album: Album,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
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
            Column(modifier = Modifier.weight(1f)) {
                Text(album.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(album.artist, fontSize = 16.sp, color = VinylGold)
            }
            Button(
                onClick = onEdit,
                colors = ButtonDefaults.buttonColors(containerColor = VinylGold, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val details = listOfNotNull(
            album.releaseYear?.let { "Year: $it" },
            album.label?.let { "Label: $it" },
            album.catalogNumber?.let { "Cat#: $it" },
            album.country?.let { "Country: $it" },
            album.barcode?.let { "Barcode: $it" },
            album.mediaCondition?.let { "Media: $it" },
            album.sleeveCondition?.let { "Sleeve: $it" },
            album.purchasePrice?.let { "Paid: $${"%.2f".format(it)}" }
        )
        Text(details.joinToString(" • "), fontSize = 12.sp, color = TextSecondary)

        if (!album.notes.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Notes: ${album.notes}", fontSize = 12.sp, color = TextTertiary)
        }

        if (album.tracks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text("Tracklist (${album.tracks.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            LazyColumn(modifier = Modifier.height(200.dp)) {
                items(album.tracks) { track ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${track.position ?: ""} ${track.title}", color = TextSecondary, fontSize = 13.sp)
                        Text(track.duration ?: "", color = TextTertiary, fontSize = 12.sp)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}
