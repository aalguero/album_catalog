package com.albumcatalog.app.data.api

import com.albumcatalog.app.data.model.Album
import com.albumcatalog.app.data.model.DeleteResponse
import com.albumcatalog.app.data.model.DiscogsPressingOption
import com.albumcatalog.app.data.model.ScanRequest
import com.albumcatalog.app.data.model.ScanResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface VinylApiService {

    @POST("api/match/scan")
    suspend fun scan(@Body req: ScanRequest): ScanResponse

    @GET("api/match/discogs-pressings")
    suspend fun getDiscogsPressings(
        @Query("artist") artist: String,
        @Query("title") title: String,
        @Query("catalog_number") catalogNumber: String? = null,
        @Query("barcode") barcode: String? = null
    ): List<DiscogsPressingOption>

    @POST("api/catalog/albums")
    suspend fun addAlbum(@Body album: Album): Album

    @GET("api/catalog/albums")
    suspend fun listAlbums(
        @Query("q") query: String? = null,
        @Query("pressing_status") pressingStatus: String? = null,
        @Query("shelf_location") shelfLocation: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): List<Album>

    @GET("api/catalog/albums/{id}")
    suspend fun getAlbum(@Path("id") id: Int): Album

    @PUT("api/catalog/albums/{id}")
    suspend fun updateAlbum(
        @Path("id") id: Int,
        @Body album: Album
    ): Album

    @DELETE("api/catalog/albums/{id}")
    suspend fun deleteAlbum(@Path("id") id: Int): DeleteResponse

    @GET("health")
    suspend fun checkHealth(): Map<String, Any>
}
