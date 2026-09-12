package com.albumcatalog.app.data.model

import com.google.gson.annotations.SerializedName

data class TrackItem(
    val position: String? = null,
    val title: String,
    val duration: String? = null,
    @SerializedName("is_favorite") val isFavorite: Boolean = false
)

data class ScanRequest(
    val barcode: String? = null,
    @SerializedName("spine_text") val spineText: String? = null,
    @SerializedName("catalog_number") val catalogNumber: String? = null,
    val artist: String? = null,
    val title: String? = null
)

data class MatchCandidate(
    val title: String,
    val artist: String,
    @SerializedName("release_year") val releaseYear: Int? = null,
    val label: String? = null,
    @SerializedName("catalog_number") val catalogNumber: String? = null,
    val barcode: String? = null,
    val format: String? = "Vinyl, LP",
    val country: String? = null,
    @SerializedName("musicbrainz_release_id") val musicbrainzReleaseId: String? = null,
    @SerializedName("discogs_release_id") val discogsReleaseId: Long? = null,
    @SerializedName("discogs_master_id") val discogsMasterId: Long? = null,
    @SerializedName("cover_image_url") val coverImageUrl: String? = null,
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null,
    val tracks: List<TrackItem> = emptyList(),
    val source: String = "local_musicbrainz",
    @SerializedName("confidence_score") val confidenceScore: Int = 100,
    @SerializedName("match_reason") val matchReason: String = "Exact match",
    @SerializedName("in_collection") val inCollection: Boolean = false,
    @SerializedName("existing_copy_info") val existingCopyInfo: String? = null
)

data class ScanResponse(
    @SerializedName("query_type") val queryType: String,
    @SerializedName("source_used") val sourceUsed: String,
    val candidates: List<MatchCandidate> = emptyList()
)

data class DiscogsPressingOption(
    @SerializedName("discogs_id") val discogsId: Long,
    val title: String,
    val artist: String,
    val year: Int? = null,
    val country: String? = null,
    @SerializedName("format_description") val formatDescription: String? = null,
    val label: String? = null,
    @SerializedName("catalog_number") val catalogNumber: String? = null,
    @SerializedName("cover_image") val coverImage: String? = null,
    val notes: String? = null
)

data class Album(
    val id: Int? = null,
    val title: String,
    val artist: String,
    @SerializedName("release_year") val releaseYear: Int? = null,
    val label: String? = null,
    @SerializedName("catalog_number") val catalogNumber: String? = null,
    val barcode: String? = null,
    val format: String? = "Vinyl, LP",
    val country: String? = null,
    
    @SerializedName("pressing_status") val pressingStatus: String = "unspecific",
    @SerializedName("metadata_source") val metadataSource: String = "local_musicbrainz",
    
    @SerializedName("musicbrainz_release_id") val musicbrainzReleaseId: String? = null,
    @SerializedName("discogs_release_id") val discogsReleaseId: Long? = null,
    @SerializedName("discogs_master_id") val discogsMasterId: Long? = null,
    @SerializedName("cover_image_url") val coverImageUrl: String? = null,
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null,
    
    @SerializedName("media_condition") val mediaCondition: String? = null,
    @SerializedName("sleeve_condition") val sleeveCondition: String? = null,
    @SerializedName("purchase_price") val purchasePrice: Double? = null,
    @SerializedName("purchase_date") val purchaseDate: String? = null,
    @SerializedName("purchase_store") val purchaseStore: String? = null,
    @SerializedName("shelf_location") val shelfLocation: String? = null,
    val notes: String? = null,
    @SerializedName("favorite_tracks") val favoriteTracks: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val tracks: List<TrackItem> = emptyList(),
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class DeleteResponse(
    val status: String,
    val id: Int
)

data class DuplicateCheckRequest(
    val barcode: String? = null,
    @SerializedName("catalog_number") val catalogNumber: String? = null,
    val artist: String? = null,
    val title: String? = null,
    @SerializedName("discogs_release_id") val discogsReleaseId: Long? = null,
    @SerializedName("musicbrainz_release_id") val musicbrainzReleaseId: String? = null
)

data class DuplicateCheckResponse(
    @SerializedName("is_duplicate") val isDuplicate: Boolean = false,
    @SerializedName("match_reason") val matchReason: String? = null,
    @SerializedName("existing_albums") val existingAlbums: List<Album> = emptyList()
)
