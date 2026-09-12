package com.albumcatalog.app.data.local

import com.albumcatalog.app.data.model.Album
import com.albumcatalog.app.data.model.MatchCandidate

data class BatchScanItem(
    val id: Long = 0,
    val scanType: String, // "BARCODE" or "VINYL_LABEL"
    val rawValue: String,
    val detectedCatno: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // "PENDING", "MATCHING", "MATCHED", "NO_MATCH", "SAVED"
    val matchedCandidate: MatchCandidate? = null,
    val errorMessage: String? = null,
    val mediaCondition: String = "VG+",
    val sleeveCondition: String = "VG+",
    val purchasePrice: Double? = null,
    val shelfLocation: String? = null,
    val notes: String? = null
) {
    fun toAlbum(): Album? {
        val cand = matchedCandidate
        return if (cand != null) {
            Album(
                title = cand.title,
                artist = cand.artist,
                releaseYear = cand.releaseYear,
                label = cand.label,
                catalogNumber = cand.catalogNumber ?: detectedCatno,
                barcode = cand.barcode ?: if (scanType == "BARCODE") rawValue else null,
                format = cand.format ?: "Vinyl, LP",
                country = cand.country,
                pressingStatus = "specific",
                metadataSource = cand.source,
                musicbrainzReleaseId = cand.musicbrainzReleaseId,
                discogsReleaseId = cand.discogsReleaseId,
                discogsMasterId = cand.discogsMasterId,
                coverImageUrl = cand.coverImageUrl,
                thumbnailUrl = cand.thumbnailUrl,
                mediaCondition = mediaCondition,
                sleeveCondition = sleeveCondition,
                purchasePrice = purchasePrice,
                shelfLocation = shelfLocation,
                notes = notes,
                tracks = cand.tracks
            )
        } else {
            Album(
                title = "Untitled Album",
                artist = "Unknown Artist",
                catalogNumber = detectedCatno,
                barcode = if (scanType == "BARCODE") rawValue else null,
                format = "Vinyl, LP",
                pressingStatus = "unspecific",
                metadataSource = "manual",
                mediaCondition = mediaCondition,
                sleeveCondition = sleeveCondition,
                purchasePrice = purchasePrice,
                shelfLocation = shelfLocation,
                notes = notes,
                tracks = emptyList()
            )
        }
    }
}
