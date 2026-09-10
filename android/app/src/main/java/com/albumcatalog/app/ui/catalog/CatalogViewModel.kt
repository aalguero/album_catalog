package com.albumcatalog.app.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albumcatalog.app.data.model.Album
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

    init {
        loadAlbums()
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

    fun deleteAlbum(id: Int) {
        viewModelScope.launch {
            repository.deleteAlbum(id)
            loadAlbums()
        }
    }
}
