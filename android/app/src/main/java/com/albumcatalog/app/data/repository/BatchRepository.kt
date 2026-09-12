package com.albumcatalog.app.data.repository

import com.albumcatalog.app.data.local.BatchScanDatabaseHelper
import com.albumcatalog.app.data.local.BatchScanItem
import com.albumcatalog.app.data.model.Album
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BatchRepository(
    private val dbHelper: BatchScanDatabaseHelper = BatchScanDatabaseHelper(),
    private val vinylRepository: VinylRepository = VinylRepository()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _itemsFlow = MutableStateFlow<List<BatchScanItem>>(emptyList())
    val itemsFlow: StateFlow<List<BatchScanItem>> = _itemsFlow.asStateFlow()

    private val _countFlow = MutableStateFlow(0)
    val countFlow: StateFlow<Int> = _countFlow.asStateFlow()

    init {
        refresh()
        processPending()
    }

    fun refresh() {
        scope.launch {
            val items = dbHelper.getAllItems()
            _itemsFlow.value = items
            _countFlow.value = items.size
        }
    }

    fun enqueueScan(scanType: String, rawValue: String, detectedCatno: String?): Long {
        val id = dbHelper.enqueueScan(scanType, rawValue, detectedCatno)
        refresh()
        resolveMetadata(id, scanType, rawValue, detectedCatno)
        return id
    }

    private fun processPending() {
        scope.launch {
            val pending = dbHelper.getPendingItems()
            for (item in pending) {
                resolveMetadata(item.id, item.scanType, item.rawValue, item.detectedCatno)
            }
        }
    }

    private fun resolveMetadata(id: Long, scanType: String, rawValue: String, detectedCatno: String?) {
        scope.launch {
            dbHelper.updateMatch(id, "MATCHING", null)
            refresh()

            val result = if (scanType == "BARCODE") {
                vinylRepository.scanBarcode(rawValue)
            } else {
                vinylRepository.scanVinylLabel(rawValue, detectedCatno)
            }

            result.onSuccess { response ->
                if (response.candidates.isNotEmpty()) {
                    val top = response.candidates.first()
                    dbHelper.updateMatch(id, "MATCHED", top)
                } else {
                    dbHelper.updateMatch(id, "NO_MATCH", null)
                }
            }.onFailure { err ->
                dbHelper.updateMatch(id, "ERROR", null, err.localizedMessage ?: "Lookup failed")
            }

            refresh()
        }
    }

    suspend fun saveItemToCatalog(item: BatchScanItem): Result<Album> = withContext(Dispatchers.IO) {
        val album = item.toAlbum() ?: return@withContext Result.failure(Exception("No album metadata available"))
        val result = vinylRepository.addToCatalog(album)
        result.onSuccess {
            dbHelper.deleteItem(item.id)
            refresh()
        }
        result
    }

    suspend fun saveAllRemaining(): Int = withContext(Dispatchers.IO) {
        val items = dbHelper.getAllItems()
        var savedCount = 0
        for (item in items) {
            val album = item.toAlbum() ?: continue
            val res = vinylRepository.addToCatalog(album)
            if (res.isSuccess) {
                dbHelper.deleteItem(item.id)
                savedCount++
            }
        }
        refresh()
        savedCount
    }

    fun updateConditions(
        id: Long,
        mediaCondition: String,
        sleeveCondition: String,
        price: Double?,
        shelf: String? = null,
        notes: String? = null
    ) {
        scope.launch {
            dbHelper.updateConditions(id, mediaCondition, sleeveCondition, price, shelf, notes)
            refresh()
        }
    }

    fun discardItem(id: Long) {
        scope.launch {
            dbHelper.deleteItem(id)
            refresh()
        }
    }

    fun clearQueue() {
        scope.launch {
            dbHelper.clearQueue()
            refresh()
        }
    }
}
