package com.notifysync.app.data.repository

import android.content.Context
import com.notifysync.app.data.api.CallDto
import com.notifysync.app.data.api.CallItem
import com.notifysync.app.data.api.RetrofitProvider
import com.notifysync.app.data.api.SmsDto
import com.notifysync.app.data.api.SmsItem
import com.notifysync.app.data.api.UploadCallsRequest
import com.notifysync.app.data.api.UploadSmsRequest
import com.notifysync.app.data.local.DeviceDataReader
import com.notifysync.app.data.local.TokenStore

/** Reads call log / SMS from the device and syncs them to the server (with server-side dedupe). */
class DeviceDataRepository(
    private val context: Context,
    private val tokenStore: TokenStore,
) {
    private val api get() = RetrofitProvider.api(tokenStore)

    suspend fun syncCalls(): Int {
        val deviceId = tokenStore.deviceId() ?: return 0
        val items: List<CallItem> = runCatching { DeviceDataReader.readCalls(context) }.getOrDefault(emptyList())
        if (items.isEmpty()) return 0
        return runCatching {
            api.uploadCalls(UploadCallsRequest(deviceId, items)).body()?.inserted ?: 0
        }.getOrDefault(0)
    }

    suspend fun syncSms(): Int {
        val deviceId = tokenStore.deviceId() ?: return 0
        val items: List<SmsItem> = runCatching { DeviceDataReader.readSms(context) }.getOrDefault(emptyList())
        return uploadSmsItems(items)
    }

    /** Upload a specific set of SMS (used by the real-time receiver). */
    suspend fun uploadSmsItems(items: List<SmsItem>): Int {
        val deviceId = tokenStore.deviceId() ?: return 0
        if (items.isEmpty() || tokenStore.token().isNullOrBlank()) return 0
        return runCatching {
            api.uploadMessages(UploadSmsRequest(deviceId, items)).body()?.inserted ?: 0
        }.getOrDefault(0)
    }

    suspend fun recentCalls(): List<CallDto> {
        if (tokenStore.token().isNullOrBlank()) return emptyList()
        return runCatching { api.getCalls(1, 200).body()?.items ?: emptyList() }.getOrDefault(emptyList())
    }

    suspend fun recentSms(): List<SmsDto> {
        if (tokenStore.token().isNullOrBlank()) return emptyList()
        return runCatching { api.getMessages(1, 200).body()?.items ?: emptyList() }.getOrDefault(emptyList())
    }

    suspend fun callTotal(): Int? {
        if (tokenStore.token().isNullOrBlank()) return null
        return runCatching { api.getCalls(1, 1).body()?.totalCount }.getOrNull()
    }

    suspend fun smsTotal(): Int? {
        if (tokenStore.token().isNullOrBlank()) return null
        return runCatching { api.getMessages(1, 1).body()?.totalCount }.getOrNull()
    }
}
