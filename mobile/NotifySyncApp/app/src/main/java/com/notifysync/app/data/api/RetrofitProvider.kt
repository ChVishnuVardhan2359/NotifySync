package com.notifysync.app.data.api

import com.notifysync.app.BuildConfig
import com.notifysync.app.data.local.TokenStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds the ApiService against the user-configured server URL (stored in [TokenStore]),
 * falling back to the compiled-in default. Rebuilds automatically when the URL changes.
 */
object RetrofitProvider {

    @Volatile private var service: ApiService? = null
    @Volatile private var currentBaseUrl: String? = null

    fun api(tokenStore: TokenStore): ApiService {
        val baseUrl = resolveBaseUrl(tokenStore)
        val cached = service
        if (cached != null && currentBaseUrl == baseUrl) return cached
        return synchronized(this) {
            if (service != null && currentBaseUrl == baseUrl) return service!!
            build(tokenStore, baseUrl).also {
                service = it
                currentBaseUrl = baseUrl
            }
        }
    }

    private fun resolveBaseUrl(tokenStore: TokenStore): String {
        val stored = runBlocking { tokenStore.serverUrl() }
        return if (!stored.isNullOrBlank()) stored else BuildConfig.API_BASE_URL
    }

    private fun build(tokenStore: TokenStore, baseUrl: String): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }
}
