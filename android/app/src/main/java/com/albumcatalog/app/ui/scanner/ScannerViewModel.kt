package com.albumcatalog.app.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albumcatalog.app.data.model.Album
import com.albumcatalog.app.data.model.DiscogsPressingOption
import com.albumcatalog.app.data.model.MatchCandidate
import com.albumcatalog.app.data.repository.VinylRepository
import com.albumcatalog.app.ui.camera.ScanMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MatchUiState {
    object Idle : MatchUiState()
    object Matching : MatchUiState()
    data class MatchesFound(val candidates: List<MatchCandidate>, val sourceUsed: String) : MatchUiState()
    data class NoMatch(
        val queryType: String,
        val scannedValue: String,
        val detectedCatno: String? = null,
        val details: String = "No matching release found."
    ) : MatchUiState()
    data class Error(val message: String) : MatchUiState()
}

sealed class DiscogsUiState {
    object Hidden : DiscogsUiState()
    object Loading : DiscogsUiState()
    data class Ready(val pressings: List<DiscogsPressingOption>) : DiscogsUiState()
    data class Error(val message: String) : DiscogsUiState()
}

class ScannerViewModel(
    private val repository: VinylRepository = VinylRepository()
) : ViewModel() {

    private val _scanMode = MutableStateFlow(ScanMode.BARCODE)
    val scanMode: StateFlow<ScanMode> = _scanMode.asStateFlow()

    private val _isScanningActive = MutableStateFlow(true)
    val isScanningActive: StateFlow<Boolean> = _isScanningActive.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _matchState = MutableStateFlow<MatchUiState>(MatchUiState.Idle)
    val matchState: StateFlow<MatchUiState> = _matchState.asStateFlow()

    private val _discogsState = MutableStateFlow<DiscogsUiState>(DiscogsUiState.Hidden)
    val discogsState: StateFlow<DiscogsUiState> = _discogsState.asStateFlow()

    // Staged items
    var stagedCandidate: MatchCandidate? = null
        private set
    var stagedPressing: DiscogsPressingOption? = null
        private set

    fun setScanMode(mode: ScanMode) {
        _scanMode.value = mode
        resetMatchState()
    }

    fun toggleTorch() {
        _isTorchOn.value = !_isTorchOn.value
    }

    fun resumeScanning() {
        _isScanningActive.value = true
        _matchState.value = MatchUiState.Idle
        _discogsState.value = DiscogsUiState.Hidden
        stagedCandidate = null
        stagedPressing = null
    }

    fun pauseScanning() {
        _isScanningActive.value = false
    }

    fun onBarcodeDetected(barcode: String) {
        if (!_isScanningActive.value) return
        _isScanningActive.value = false
        _matchState.value = MatchUiState.Matching

        viewModelScope.launch {
            val result = repository.scanBarcode(barcode)
            result.onSuccess { response ->
                if (response.candidates.isNotEmpty()) {
                    _matchState.value = MatchUiState.MatchesFound(
                        candidates = response.candidates,
                        sourceUsed = response.sourceUsed
                    )
                } else {
                    _matchState.value = MatchUiState.NoMatch(
                        queryType = "Barcode (UPC/EAN)",
                        scannedValue = barcode,
                        details = "Searched Local MusicBrainz, Remote MusicBrainz, and Discogs for barcode '$barcode'."
                    )
                }
            }.onFailure { error ->
                _matchState.value = MatchUiState.Error(error.localizedMessage ?: "Failed to match barcode")
            }
        }
    }

    fun onLabelDetected(fullText: String, catno: String?) {
        if (!_isScanningActive.value) return
        _isScanningActive.value = false
        _matchState.value = MatchUiState.Matching

        viewModelScope.launch {
            val result = repository.scanVinylLabel(fullText, catno)
            result.onSuccess { response ->
                if (response.candidates.isNotEmpty()) {
                    _matchState.value = MatchUiState.MatchesFound(
                        candidates = response.candidates,
                        sourceUsed = response.sourceUsed
                    )
                } else {
                    _matchState.value = MatchUiState.NoMatch(
                        queryType = "Vinyl Center Label OCR",
                        scannedValue = fullText,
                        detectedCatno = catno,
                        details = "Extracted catalog number '${catno ?: "None detected"}' from label text and searched Local MusicBrainz, Remote MusicBrainz, and Discogs."
                    )
                }
            }.onFailure { error ->
                _matchState.value = MatchUiState.Error(error.localizedMessage ?: "Failed to match label text")
            }
        }
    }

    fun performManualSearch(query: String) {
        if (query.isBlank()) return
        _isScanningActive.value = false
        _matchState.value = MatchUiState.Matching

        viewModelScope.launch {
            val result = repository.manualSearch(query)
            result.onSuccess { response ->
                if (response.candidates.isNotEmpty()) {
                    _matchState.value = MatchUiState.MatchesFound(
                        candidates = response.candidates,
                        sourceUsed = response.sourceUsed
                    )
                } else {
                    _matchState.value = MatchUiState.NoMatch(
                        queryType = "Manual Search Query",
                        scannedValue = query,
                        details = "Searched artist, title, and catalog number for '$query'."
                    )
                }
            }.onFailure { error ->
                _matchState.value = MatchUiState.Error(error.localizedMessage ?: "Search failed")
            }
        }
    }

    fun startDiscogsRefinement(candidate: MatchCandidate) {
        stagedCandidate = candidate
        _discogsState.value = DiscogsUiState.Loading

        viewModelScope.launch {
            val result = repository.fetchDiscogsPressings(
                artist = candidate.artist,
                title = candidate.title,
                catno = candidate.catalogNumber,
                barcode = candidate.barcode
            )
            result.onSuccess { pressings ->
                _discogsState.value = DiscogsUiState.Ready(pressings)
            }.onFailure { error ->
                _discogsState.value = DiscogsUiState.Error(error.localizedMessage ?: "Failed to fetch Discogs pressings")
            }
        }
    }

    fun selectDiscogsPressing(pressing: DiscogsPressingOption) {
        stagedPressing = pressing
        _discogsState.value = DiscogsUiState.Hidden
    }

    fun keepUnspecificPressing() {
        stagedPressing = null
        _discogsState.value = DiscogsUiState.Hidden
    }

    fun dismissDiscogsSheet() {
        _discogsState.value = DiscogsUiState.Hidden
    }

    fun buildStagedAlbum(candidate: MatchCandidate, pressing: DiscogsPressingOption? = stagedPressing): Album {
        return Album(
            title = candidate.title,
            artist = candidate.artist,
            releaseYear = pressing?.year ?: candidate.releaseYear,
            label = pressing?.label ?: candidate.label,
            catalogNumber = pressing?.catalogNumber ?: candidate.catalogNumber,
            barcode = candidate.barcode,
            country = pressing?.country ?: candidate.country,
            format = candidate.format,
            pressingStatus = if (pressing != null) "specific" else "unspecific",
            metadataSource = if (pressing != null) "discogs" else candidate.source,
            musicbrainzReleaseId = candidate.musicbrainzReleaseId,
            discogsReleaseId = pressing?.discogsId ?: candidate.discogsReleaseId,
            discogsMasterId = candidate.discogsMasterId,
            coverImageUrl = candidate.coverImageUrl ?: pressing?.coverImage,
            thumbnailUrl = candidate.thumbnailUrl,
            mediaCondition = "VG+",
            sleeveCondition = "VG+",
            purchasePrice = null,
            tracks = candidate.tracks
        )
    }

    fun resetMatchState() {
        _matchState.value = MatchUiState.Idle
        _discogsState.value = DiscogsUiState.Hidden
        _isScanningActive.value = true
        stagedCandidate = null
        stagedPressing = null
    }
}
