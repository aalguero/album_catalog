package com.albumcatalog.app.data.api

import com.albumcatalog.app.VinylCatalogApp
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private var currentBaseUrl: String? = null
    private var cachedService: VinylApiService? = null

    val service: VinylApiService
        get() {
            val url = VinylCatalogApp.instance.getBaseUrl()
            if (cachedService != null && currentBaseUrl == url) {
                return cachedService!!
            }
            currentBaseUrl = url
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val okHttp = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(url)
                .client(okHttp)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val instance = retrofit.create(VinylApiService::class.java)
            cachedService = instance
            return instance
        }
}
