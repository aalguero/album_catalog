package com.albumcatalog.app.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albumcatalog.app.data.model.Album
import com.albumcatalog.app.data.model.DiscogsPressingOption
import com.albumcatalog.app.data.model.MatchCandidate
import com.albumcatalog.app.data.repository.VinylRepository
import com.albumcatalog.app.ui.camera.LabelScanState
import com.albumcatalog.app.ui.camera.ScanMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.albumcatalog.app.util.FeedbackManager

sealed class MatchUiState {
    object Idle : MatchUiState()
    object Matching : MatchUiState()
    data class MatchesFound(
        val candidates: List<MatchCandidate>,
        val sourceUsed: String,
        val rawScannedValue: String? = null,
        val detectedCatno: String? = null
    ) : MatchUiState()
    data class NoMatch(
        val queryType: String,
        val scannedValue: String,
        val detectedCatno: String? = null,
        val artist: String? = null,
        val title: String? = null,
        val details: String = "No matching release found."
    ) : MatchUiState()
    data class Error(val message: String) : MatchUiState()
}

data class OcrCorrectionState(
    val isVisible: Boolean = false,
    val catno: String = "",
    val artist: String = "",
    val title: String = "",
    val rawText: String = "",
    val scanType: String = "Vinyl Center Label OCR"
)

sealed class DiscogsUiState {
    object Hidden : DiscogsUiState()
    object Loading : DiscogsUiState()
    data class Ready(val pressings: List<DiscogsPressingOption>) : DiscogsUiState()
    data class Error(val message: String) : DiscogsUiState()
}

class ScannerViewModel(
    private val repository: VinylRepository = VinylRepository(),
    private val feedbackManager: FeedbackManager = FeedbackManager()
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

    private val _ocrCorrectionState = MutableStateFlow(OcrCorrectionState())
    val ocrCorrectionState: StateFlow<OcrCorrectionState> = _ocrCorrectionState.asStateFlow()

    private val _labelScanState = MutableStateFlow(LabelScanState())
    val labelScanState: StateFlow<LabelScanState> = _labelScanState.asStateFlow()

    private val _manualCaptureTrigger = MutableStateFlow(0L)
    val manualCaptureTrigger: StateFlow<Long> = _manualCaptureTrigger.asStateFlow()

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

    fun updateLabelScanState(state: LabelScanState) {
        _labelScanState.value = state
    }

    fun triggerManualCapture() {
        _manualCaptureTrigger.value = System.currentTimeMillis()
    }

    fun resumeScanning() {
        _isScanningActive.value = true
        _matchState.value = MatchUiState.Idle
        _discogsState.value = DiscogsUiState.Hidden
        _ocrCorrectionState.value = OcrCorrectionState(isVisible = false)
        _labelScanState.value = LabelScanState()
        stagedCandidate = null
        stagedPressing = null
    }

    fun pauseScanning() {
        _isScanningActive.value = false
    }

    fun openOcrCorrection(
        catno: String? = null,
        artist: String? = null,
        title: String? = null,
        rawText: String? = null,
        scanType: String = "Vinyl Center Label OCR"
    ) {
        _isScanningActive.value = false
        _ocrCorrectionState.value = OcrCorrectionState(
            isVisible = true,
            catno = catno ?: "",
            artist = artist ?: "",
            title = title ?: "",
            rawText = rawText ?: "",
            scanType = scanType
        )
    }

    fun dismissOcrCorrection() {
        _ocrCorrectionState.value = _ocrCorrectionState.value.copy(isVisible = false)
    }

    fun performCorrectedSearch(
        catno: String,
        artist: String,
        title: String,
        rawText: String
    ) {
        _ocrCorrectionState.value = _ocrCorrectionState.value.copy(isVisible = false)
        _isScanningActive.value = false
        _matchState.value = MatchUiState.Matching

        viewModelScope.launch {
            val result = repository.searchWithCorrections(
                catno = catno,
                artist = artist,
                title = title,
                fullText = rawText
            )
            result.onSuccess { response ->
                if (response.candidates.isNotEmpty()) {
                    feedbackManager.onMatchSuccess()
                    _matchState.value = MatchUiState.MatchesFound(
                        candidates = response.candidates,
                        sourceUsed = response.sourceUsed,
                        rawScannedValue = rawText,
                        detectedCatno = catno
                    )
                } else {
                    feedbackManager.onMatchMiss()
                    val summaryList = listOfNotNull(
                        if (catno.isNotBlank()) "Cat#: $catno" else null,
                        if (artist.isNotBlank()) "Artist: $artist" else null,
                        if (title.isNotBlank()) "Title: $title" else null
                    )
                    _matchState.value = MatchUiState.NoMatch(
                        queryType = "Corrected Search",
                        scannedValue = rawText.ifBlank { summaryList.joinToString(", ") },
                        detectedCatno = catno.ifBlank { null },
                        artist = artist.ifBlank { null },
                        title = title.ifBlank { null },
                        details = "Searched ${summaryList.joinToString(", ")} across Local MusicBrainz, Remote MusicBrainz, and Discogs."
                    )
                }
            }.onFailure { error ->
                feedbackManager.onMatchMiss()
                _matchState.value = MatchUiState.Error(error.localizedMessage ?: "Search failed")
            }
        }
    }

    fun buildManualAlbum(
        catno: String,
        artist: String,
        title: String,
        label: String? = null,
        year: Int? = null
    ): Album {
        return Album(
            title = title.ifBlank { "Untitled Album" },
            artist = artist.ifBlank { "Unknown Artist" },
            catalogNumber = catno.ifBlank { null },
            label = label?.ifBlank { null },
            releaseYear = year,
            format = "Vinyl, LP",
            pressingStatus = "unspecific",
            metadataSource = "manual",
            mediaCondition = "VG+",
            sleeveCondition = "VG+",
            purchasePrice = null,
            tracks = emptyList()
        )
    }

    fun onBarcodeDetected(barcode: String) {
        if (!_isScanningActive.value) return
        _isScanningActive.value = false
        feedbackManager.onScanLocked()
        _matchState.value = MatchUiState.Matching

        viewModelScope.launch {
            val result = repository.scanBarcode(barcode)
            result.onSuccess { response ->
                if (response.candidates.isNotEmpty()) {
                    feedbackManager.onMatchSuccess()
                    _matchState.value = MatchUiState.MatchesFound(
                        candidates = response.candidates,
                        sourceUsed = response.sourceUsed,
                        rawScannedValue = barcode
                    )
                } else {
                    feedbackManager.onMatchMiss()
                    _matchState.value = MatchUiState.NoMatch(
                        queryType = "Barcode (UPC/EAN)",
                        scannedValue = barcode,
                        details = "Searched Local MusicBrainz, Remote MusicBrainz, and Discogs for barcode '$barcode'."
                    )
                }
            }.onFailure { error ->
                feedbackManager.onMatchMiss()
                _matchState.value = MatchUiState.Error(error.localizedMessage ?: "Failed to match barcode")
            }
        }
    }

    fun onLabelDetected(fullText: String, catno: String?) {
        if (!_isScanningActive.value) return
        _isScanningActive.value = false
        feedbackManager.onScanLocked()
        _matchState.value = MatchUiState.Matching

        viewModelScope.launch {
            val result = repository.scanVinylLabel(fullText, catno)
            result.onSuccess { response ->
                if (response.candidates.isNotEmpty()) {
                    feedbackManager.onMatchSuccess()
                    _matchState.value = MatchUiState.MatchesFound(
                        candidates = response.candidates,
                        sourceUsed = response.sourceUsed,
                        rawScannedValue = fullText,
                        detectedCatno = catno
                    )
                } else {
                    feedbackManager.onMatchMiss()
                    _matchState.value = MatchUiState.NoMatch(
                        queryType = "Vinyl Center Label OCR",
                        scannedValue = fullText,
                        detectedCatno = catno,
                        details = "Extracted catalog number '${catno ?: "None detected"}' from label text and searched Local MusicBrainz, Remote MusicBrainz, and Discogs."
                    )
                }
            }.onFailure { error ->
                feedbackManager.onMatchMiss()
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
                    feedbackManager.onMatchSuccess()
                    _matchState.value = MatchUiState.MatchesFound(
                        candidates = response.candidates,
                        sourceUsed = response.sourceUsed
                    )
                } else {
                    feedbackManager.onMatchMiss()
                    _matchState.value = MatchUiState.NoMatch(
                        queryType = "Manual Search Query",
                        scannedValue = query,
                        details = "Searched artist, title, and catalog number for '$query'."
                    )
                }
            }.onFailure { error ->
                feedbackManager.onMatchMiss()
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

    fun buildStagedAlbum(
        candidate: MatchCandidate,
        pressing: DiscogsPressingOption? = stagedPressing,
        isSingleMatch: Boolean = false
    ): Album {
        val isSpecific = pressing != null ||
            isSingleMatch ||
            candidate.discogsReleaseId != null ||
            candidate.source == "discogs" ||
            (candidate.catalogNumber != null && candidate.releaseYear != null && candidate.barcode != null)

        return Album(
            title = candidate.title,
            artist = candidate.artist,
            releaseYear = pressing?.year ?: candidate.releaseYear,
            label = pressing?.label ?: candidate.label,
            catalogNumber = pressing?.catalogNumber ?: candidate.catalogNumber,
            barcode = candidate.barcode,
            country = pressing?.country ?: candidate.country,
            format = candidate.format,
            pressingStatus = if (isSpecific) "specific" else "unspecific",
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
        _ocrCorrectionState.value = OcrCorrectionState(isVisible = false)
        _labelScanState.value = LabelScanState()
        _isScanningActive.value = true
        stagedCandidate = null
        stagedPressing = null
    }

    override fun onCleared() {
        super.onCleared()
        feedbackManager.release()
    }
}
