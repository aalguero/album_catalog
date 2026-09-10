package com.albumcatalog.app

import android.app.Application
import android.content.Context
import android.content.SharedPreferences

class VinylCatalogApp : Application() {

    companion object {
        lateinit var instance: VinylCatalogApp
            private set
        
        private const val PREFS_NAME = "vinyl_catalog_prefs"
        private const val KEY_BASE_URL = "backend_base_url"
        // Default to emulator localhost (10.0.2.2) or local network
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8000/"
    }

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    fun getBaseUrl(): String {
        return prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun setBaseUrl(url: String) {
        val formatted = if (url.endsWith("/")) url else "$url/"
        prefs.edit().putString(KEY_BASE_URL, formatted).apply()
    }
}
