package com.notifysync.app.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.notifysync.app.util.BackgroundLog
import com.notifysync.app.worker.UploadWorker

/**
 * Event-driven sync: registers a network callback and flushes the offline queue the moment
 * internet becomes available again (e.g. after a tunnel/airplane-mode/Wi-Fi drop).
 */
object ConnectivityObserver {
    @Volatile private var callback: ConnectivityManager.NetworkCallback? = null

    fun start(context: Context) {
        if (callback != null) return
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                BackgroundLog.event("network available — triggering sync")
                UploadWorker.enqueue(context.applicationContext)
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        runCatching {
            cm.registerNetworkCallback(request, cb)
            callback = cb
        }
    }

    fun stop(context: Context) {
        val cb = callback ?: return
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        runCatching { cm.unregisterNetworkCallback(cb) }
        callback = null
    }
}
