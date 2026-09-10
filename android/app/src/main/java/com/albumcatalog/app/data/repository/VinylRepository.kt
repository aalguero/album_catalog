package com.albumcatalog.app.data.repository

import com.albumcatalog.app.data.api.ApiClient
import com.albumcatalog.app.data.model.Album
import com.albumcatalog.app.data.model.DiscogsPressingOption
import com.albumcatalog.app.data.model.ScanRequest
import com.albumcatalog.app.data.model.ScanResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VinylRepository {

    private val api get() = ApiClient.service

    suspend fun scanBarcode(barcode: String): Result<ScanResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.scan(ScanRequest(barcode = barcode.trim()))
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun scanVinylLabel(fullText: String, catno: String? = null): Result<ScanResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.scan(ScanRequest(
                spineText = fullText.trim(),
                catalogNumber = catno?.trim()
            ))
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchWithCorrections(
        catno: String? = null,
        artist: String? = null,
        title: String? = null,
        fullText: String? = null
    ): Result<ScanResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.scan(ScanRequest(
                catalogNumber = catno?.trim()?.ifEmpty { null },
                artist = artist?.trim()?.ifEmpty { null },
                title = title?.trim()?.ifEmpty { null },
                spineText = fullText?.trim()?.ifEmpty { null }
            ))
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAlbum(id: Int, album: Album): Result<Album> = withContext(Dispatchers.IO) {
        try {
            val updated = api.updateAlbum(id, album)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun manualSearch(query: String): Result<ScanResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.scan(ScanRequest(spineText = query.trim()))
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchDiscogsPressings(
        artist: String,
        title: String,
        catno: String? = null,
        barcode: String? = null
    ): Result<List<DiscogsPressingOption>> = withContext(Dispatchers.IO) {
        try {
            val list = api.getDiscogsPressings(
                artist = artist,
                title = title,
                catalogNumber = catno,
                barcode = barcode
            )
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addToCatalog(album: Album): Result<Album> = withContext(Dispatchers.IO) {
        try {
            val saved = api.addAlbum(album)
            Result.success(saved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAlbums(
        query: String? = null,
        pressingStatus: String? = null,
        shelfLocation: String? = null
    ): Result<List<Album>> = withContext(Dispatchers.IO) {
        try {
            val list = api.listAlbums(
                query = query,
                pressingStatus = pressingStatus,
                shelfLocation = shelfLocation
            )
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAlbum(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            api.deleteAlbum(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkHealth(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val res = api.checkHealth()
            Result.success(res.containsKey("status"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
