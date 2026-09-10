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
    data class NoMatch(val reason: String) : MatchUiState()
    data class Error(val message: String) : MatchUiState()
}

sealed class DiscogsUiState {
    object Hidden : DiscogsUiState()
    object Loading : DiscogsUiState()
    data class Ready(val pressings: List<DiscogsPressingOption>) : DiscogsUiState()
    data class Error(val message: String) : DiscogsUiState()
}

sealed class SaveUiState {
    object Idle : SaveUiState()
    object Saving : SaveUiState()
    data class Success(val album: Album) : SaveUiState()
    data class Error(val message: String) : SaveUiState()
}

class ScannerViewModel(
    private val repository: VinylRepository = VinylRepository()
) : ViewModel() {

    private val _scanMode = MutableStateFlow(ScanMode.BARCODE)
    val scanMode: StateFlow<ScanMode> = _scanMode.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _matchState = MutableStateFlow<MatchUiState>(MatchUiState.Idle)
    val matchState: StateFlow<MatchUiState> = _matchState.asStateFlow()

    private val _discogsState = MutableStateFlow<DiscogsUiState>(DiscogsUiState.Hidden)
    val discogsState: StateFlow<DiscogsUiState> = _discogsState.asStateFlow()

    private val _saveState = MutableStateFlow<SaveUiState>(SaveUiState.Idle)
    val saveState: StateFlow<SaveUiState> = _saveState.asStateFlow()

    // Staging for album saving
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

    fun onBarcodeDetected(barcode: String) {
        if (_matchState.value is MatchUiState.Matching) return
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
                    _matchState.value = MatchUiState.NoMatch("No release found for barcode: $barcode")
                }
            }.onFailure { error ->
                _matchState.value = MatchUiState.Error(error.localizedMessage ?: "Failed to match barcode")
            }
        }
    }

    fun onSpineTextRecognized(fullText: String, catno: String?) {
        if (_matchState.value is MatchUiState.Matching) return
        _matchState.value = MatchUiState.Matching

        viewModelScope.launch {
            val result = repository.scanSpineText(catno ?: fullText)
            result.onSuccess { response ->
                if (response.candidates.isNotEmpty()) {
                    _matchState.value = MatchUiState.MatchesFound(
                        candidates = response.candidates,
                        sourceUsed = response.sourceUsed
                    )
                } else {
                    _matchState.value = MatchUiState.NoMatch("No release found for spine text")
                }
            }.onFailure { error ->
                _matchState.value = MatchUiState.Error(error.localizedMessage ?: "Failed to match spine text")
            }
        }
    }

    fun performManualSearch(query: String) {
        if (query.isBlank()) return
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
                    _matchState.value = MatchUiState.NoMatch("No matches found for: $query")
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

    fun saveToCatalog(album: Album) {
        _saveState.value = SaveUiState.Saving
        viewModelScope.launch {
            val result = repository.addToCatalog(album)
            result.onSuccess { saved ->
                _saveState.value = SaveUiState.Success(saved)
                resetMatchState()
            }.onFailure { error ->
                _saveState.value = SaveUiState.Error(error.localizedMessage ?: "Failed to save album")
            }
        }
    }

    fun resetMatchState() {
        _matchState.value = MatchUiState.Idle
        _discogsState.value = DiscogsUiState.Hidden
        stagedCandidate = null
        stagedPressing = null
    }

    fun clearSaveState() {
        _saveState.value = SaveUiState.Idle
    }
}
