package com.notifysync.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.notifysync.app.BuildConfig
import com.notifysync.app.data.api.AppInfoDto
import com.notifysync.app.data.api.RetrofitProvider
import com.notifysync.app.data.local.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/** Checks the server for a newer APK, downloads it, and launches the system installer. */
object UpdateManager {

    /** Returns server app info when a newer version is available, else null. */
    suspend fun available(tokenStore: TokenStore): AppInfoDto? {
        val info = runCatching { RetrofitProvider.api(tokenStore).getAppInfo().body() }.getOrNull()
        return if (info != null && info.available && info.versionCode > BuildConfig.VERSION_CODE) info else null
    }

    /** Downloads the latest APK to internal storage; returns the file, or null on failure. */
    suspend fun download(context: Context, tokenStore: TokenStore): File? = withContext(Dispatchers.IO) {
        val base = tokenStore.serverUrl()?.trimEnd('/') ?: return@withContext null
        val url = "$base/api/app/download"
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val builder = Request.Builder().url(url)
        tokenStore.token()?.let { builder.addHeader("Authorization", "Bearer $it") }

        val dir = File(context.filesDir, "updates").apply { mkdirs() }
        val file = File(dir, "NotifySync.apk")
        runCatching {
            client.newCall(builder.build()).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                resp.body?.byteStream()?.use { input ->
                    file.outputStream().use { out -> input.copyTo(out) }
                }
            }
        }.getOrElse { return@withContext null }
        file
    }

    /** Launches the system package installer for the downloaded APK (updates in place — no uninstall). */
    fun install(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
