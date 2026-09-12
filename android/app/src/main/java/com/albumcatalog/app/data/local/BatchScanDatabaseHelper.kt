package com.albumcatalog.app.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.albumcatalog.app.VinylCatalogApp
import com.albumcatalog.app.data.model.Album
import com.albumcatalog.app.data.model.MatchCandidate
import com.google.gson.Gson

class BatchScanDatabaseHelper(
    context: Context = VinylCatalogApp.instance
) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "vinyl_batch_queue.db"
        const val DATABASE_VERSION = 1

        const val TABLE_NAME = "batch_scans"
        const val COL_ID = "id"
        const val COL_SCAN_TYPE = "scan_type"
        const val COL_RAW_VALUE = "raw_value"
        const val COL_DETECTED_CATNO = "detected_catno"
        const val COL_TIMESTAMP = "timestamp"
        const val COL_STATUS = "status"
        const val COL_MATCH_JSON = "matched_candidate_json"
        const val COL_ERROR_MSG = "error_message"
        const val COL_MEDIA_COND = "media_condition"
        const val COL_SLEEVE_COND = "sleeve_condition"
        const val COL_PRICE = "purchase_price"
        const val COL_SHELF = "shelf_location"
        const val COL_NOTES = "notes"
    }

    private val gson = Gson()

    override fun onCreate(db: SQLiteDatabase) {
        val sql = """
            CREATE TABLE $TABLE_NAME (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_SCAN_TYPE TEXT NOT NULL,
                $COL_RAW_VALUE TEXT NOT NULL,
                $COL_DETECTED_CATNO TEXT,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_STATUS TEXT NOT NULL,
                $COL_MATCH_JSON TEXT,
                $COL_ERROR_MSG TEXT,
                $COL_MEDIA_COND TEXT DEFAULT 'VG+',
                $COL_SLEEVE_COND TEXT DEFAULT 'VG+',
                $COL_PRICE REAL,
                $COL_SHELF TEXT,
                $COL_NOTES TEXT
            )
        """.trimIndent()
        db.execSQL(sql)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    @Synchronized
    fun enqueueScan(scanType: String, rawValue: String, detectedCatno: String?): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_SCAN_TYPE, scanType)
            put(COL_RAW_VALUE, rawValue)
            put(COL_DETECTED_CATNO, detectedCatno)
            put(COL_TIMESTAMP, System.currentTimeMillis())
            put(COL_STATUS, "PENDING")
            put(COL_MEDIA_COND, "VG+")
            put(COL_SLEEVE_COND, "VG+")
        }
        return db.insert(TABLE_NAME, null, values)
    }

    @Synchronized
    fun updateMatch(id: Long, status: String, candidate: MatchCandidate?, errorMsg: String? = null) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_STATUS, status)
            if (candidate != null) {
                put(COL_MATCH_JSON, gson.toJson(candidate))
            } else {
                putNull(COL_MATCH_JSON)
            }
            if (errorMsg != null) {
                put(COL_ERROR_MSG, errorMsg)
            } else {
                putNull(COL_ERROR_MSG)
            }
        }
        db.update(TABLE_NAME, values, "$COL_ID = ?", arrayOf(id.toString()))
    }

    @Synchronized
    fun updateConditions(
        id: Long,
        mediaCondition: String,
        sleeveCondition: String,
        price: Double?,
        shelf: String?,
        notes: String?
    ) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_MEDIA_COND, mediaCondition)
            put(COL_SLEEVE_COND, sleeveCondition)
            if (price != null) put(COL_PRICE, price) else putNull(COL_PRICE)
            if (shelf != null) put(COL_SHELF, shelf) else putNull(COL_SHELF)
            if (notes != null) put(COL_NOTES, notes) else putNull(COL_NOTES)
        }
        db.update(TABLE_NAME, values, "$COL_ID = ?", arrayOf(id.toString()))
    }

    @Synchronized
    fun isBarcodeQueued(barcode: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT 1 FROM $TABLE_NAME WHERE $COL_RAW_VALUE = ? LIMIT 1",
            arrayOf(barcode.trim())
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    @Synchronized
    fun isCatnoQueued(catno: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT 1 FROM $TABLE_NAME WHERE $COL_DETECTED_CATNO = ? LIMIT 1",
            arrayOf(catno.trim())
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    @Synchronized
    fun attachCandidateToItem(id: Long, candidate: MatchCandidate) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_STATUS, "MATCHED")
            put(COL_MATCH_JSON, gson.toJson(candidate))
            putNull(COL_ERROR_MSG)
            if (candidate.catalogNumber != null) {
                put(COL_DETECTED_CATNO, candidate.catalogNumber)
            }
        }
        db.update(TABLE_NAME, values, "$COL_ID = ?", arrayOf(id.toString()))
    }

    @Synchronized
    fun updateScannedValues(id: Long, rawValue: String, detectedCatno: String?) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_RAW_VALUE, rawValue)
            put(COL_DETECTED_CATNO, detectedCatno)
        }
        db.update(TABLE_NAME, values, "$COL_ID = ?", arrayOf(id.toString()))
    }

    @Synchronized
    fun addManualAlbum(album: Album): Long {
        val db = writableDatabase
        val candidate = MatchCandidate(
            title = album.title,
            artist = album.artist,
            releaseYear = album.releaseYear,
            label = album.label,
            catalogNumber = album.catalogNumber,
            barcode = album.barcode,
            format = album.format,
            country = album.country,
            source = "manual",
            tracks = album.tracks
        )
        val values = ContentValues().apply {
            put(COL_SCAN_TYPE, "MANUAL")
            put(COL_RAW_VALUE, "${album.artist} - ${album.title}")
            put(COL_DETECTED_CATNO, album.catalogNumber)
            put(COL_TIMESTAMP, System.currentTimeMillis())
            put(COL_STATUS, "MATCHED")
            put(COL_MATCH_JSON, gson.toJson(candidate))
            put(COL_MEDIA_COND, album.mediaCondition)
            put(COL_SLEEVE_COND, album.sleeveCondition)
            if (album.purchasePrice != null) put(COL_PRICE, album.purchasePrice) else putNull(COL_PRICE)
            if (album.shelfLocation != null) put(COL_SHELF, album.shelfLocation) else putNull(COL_SHELF)
            if (album.notes != null) put(COL_NOTES, album.notes) else putNull(COL_NOTES)
        }
        return db.insert(TABLE_NAME, null, values)
    }

    @Synchronized
    fun getQueueCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_NAME", null)
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }

    @Synchronized
    fun getAllItems(): List<BatchScanItem> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "$COL_ID ASC"
        )
        val items = mutableListOf<BatchScanItem>()
        while (cursor.moveToNext()) {
            items.add(cursorToItem(cursor))
        }
        cursor.close()
        return items
    }

    @Synchronized
    fun getPendingItems(): List<BatchScanItem> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            "$COL_STATUS = ?",
            arrayOf("PENDING"),
            null,
            null,
            "$COL_ID ASC"
        )
        val items = mutableListOf<BatchScanItem>()
        while (cursor.moveToNext()) {
            items.add(cursorToItem(cursor))
        }
        cursor.close()
        return items
    }

    @Synchronized
    fun getItem(id: Long): BatchScanItem? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            "$COL_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )
        var item: BatchScanItem? = null
        if (cursor.moveToFirst()) {
            item = cursorToItem(cursor)
        }
        cursor.close()
        return item
    }

    @Synchronized
    fun deleteItem(id: Long): Boolean {
        val db = writableDatabase
        return db.delete(TABLE_NAME, "$COL_ID = ?", arrayOf(id.toString())) > 0
    }

    @Synchronized
    fun clearQueue() {
        val db = writableDatabase
        db.delete(TABLE_NAME, null, null)
    }

    private fun cursorToItem(cursor: Cursor): BatchScanItem {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID))
        val scanType = cursor.getString(cursor.getColumnIndexOrThrow(COL_SCAN_TYPE))
        val rawValue = cursor.getString(cursor.getColumnIndexOrThrow(COL_RAW_VALUE))
        val catno = cursor.getString(cursor.getColumnIndexOrThrow(COL_DETECTED_CATNO))
        val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP))
        val status = cursor.getString(cursor.getColumnIndexOrThrow(COL_STATUS))
        val matchJson = cursor.getString(cursor.getColumnIndexOrThrow(COL_MATCH_JSON))
        val errorMsg = cursor.getString(cursor.getColumnIndexOrThrow(COL_ERROR_MSG))
        val mediaCond = cursor.getString(cursor.getColumnIndexOrThrow(COL_MEDIA_COND)) ?: "VG+"
        val sleeveCond = cursor.getString(cursor.getColumnIndexOrThrow(COL_SLEEVE_COND)) ?: "VG+"
        val priceIdx = cursor.getColumnIndexOrThrow(COL_PRICE)
        val price = if (cursor.isNull(priceIdx)) null else cursor.getDouble(priceIdx)
        val shelf = cursor.getString(cursor.getColumnIndexOrThrow(COL_SHELF))
        val notes = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTES))

        val candidate = if (!matchJson.isNullOrEmpty()) {
            try {
                gson.fromJson(matchJson, MatchCandidate::class.java)
            } catch (_: Exception) {
                null
            }
        } else null

        return BatchScanItem(
            id = id,
            scanType = scanType,
            rawValue = rawValue,
            detectedCatno = catno,
            timestamp = timestamp,
            status = status,
            matchedCandidate = candidate,
            errorMessage = errorMsg,
            mediaCondition = mediaCond,
            sleeveCondition = sleeveCond,
            purchasePrice = price,
            shelfLocation = shelf,
            notes = notes
        )
    }
}
