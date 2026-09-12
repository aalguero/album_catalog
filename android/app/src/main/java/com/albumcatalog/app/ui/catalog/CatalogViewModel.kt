package com.albumcatalog.app.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albumcatalog.app.data.model.Album
import com.albumcatalog.app.data.model.DiscogsPressingOption
import com.albumcatalog.app.data.repository.VinylRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CatalogUiState {
    object Loading : CatalogUiState()
    data class Success(val albums: List<Album>) : CatalogUiState()
    data class Error(val message: String) : CatalogUiState()
}

class CatalogViewModel(
    private val repository: VinylRepository = VinylRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<CatalogUiState>(CatalogUiState.Loading)
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _pressingFilter = MutableStateFlow<String?>(null)
    val pressingFilter: StateFlow<String?> = _pressingFilter.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _duplicateState = MutableStateFlow<com.albumcatalog.app.data.model.DuplicateCheckResponse?>(null)
    val duplicateState: StateFlow<com.albumcatalog.app.data.model.DuplicateCheckResponse?> = _duplicateState.asStateFlow()

    init {
        loadAlbums()
    }

    fun checkDuplicate(album: Album) {
        viewModelScope.launch {
            val result = repository.checkDuplicate(
                barcode = album.barcode,
                catno = album.catalogNumber,
                artist = album.artist,
                title = album.title,
                discogsId = album.discogsReleaseId,
                mbId = album.musicbrainzReleaseId
            )
            result.onSuccess { resp ->
                _duplicateState.value = resp
            }.onFailure {
                _duplicateState.value = null
            }
        }
    }

    fun clearDuplicateState() {
        _duplicateState.value = null
    }

    fun loadAlbums() {
        _uiState.value = CatalogUiState.Loading
        viewModelScope.launch {
            val result = repository.getAlbums(
                query = _searchQuery.value.ifEmpty { null },
                pressingStatus = _pressingFilter.value
            )
            result.onSuccess { list ->
                _uiState.value = CatalogUiState.Success(list)
            }.onFailure { error ->
                _uiState.value = CatalogUiState.Error(error.localizedMessage ?: "Failed to load collection")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        loadAlbums()
    }

    fun setPressingFilter(filter: String?) {
        _pressingFilter.value = filter
        loadAlbums()
    }

    fun addConfirmedAlbum(album: Album, onComplete: () -> Unit) {
        viewModelScope.launch {
            val result = repository.addToCatalog(album)
            result.onSuccess { saved ->
                _statusMessage.value = "Added '${saved.title}' to your collection!"
                loadAlbums()
                onComplete()
            }.onFailure { err ->
                _statusMessage.value = "Failed to add album: ${err.localizedMessage}"
            }
        }
    }

    fun updateAlbum(id: Int, updatedAlbum: Album, onComplete: () -> Unit) {
        viewModelScope.launch {
            val result = repository.updateAlbum(id, updatedAlbum)
            result.onSuccess { saved ->
                _statusMessage.value = "Updated '${saved.title}'"
                loadAlbums()
                onComplete()
            }.onFailure { err ->
                _statusMessage.value = "Update failed: ${err.localizedMessage}"
            }
        }
    }

    fun deleteAlbum(id: Int) {
        viewModelScope.launch {
            repository.deleteAlbum(id)
            loadAlbums()
        }
    }

    suspend fun fetchDiscogsPressings(artist: String, title: String, catno: String?, barcode: String?): List<DiscogsPressingOption> {
        val res = repository.fetchDiscogsPressings(artist, title, catno, barcode)
        return res.getOrDefault(emptyList())
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
