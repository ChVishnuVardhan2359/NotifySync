package com.notifysync.app.data.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequest(val email: String, val password: String)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val token: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val expiresAt: String,
)

@JsonClass(generateAdapter = true)
data class RegisterDeviceRequest(val deviceName: String, val deviceIdentifier: String)

@JsonClass(generateAdapter = true)
data class HeartbeatRequest(val deviceIdentifier: String)

@JsonClass(generateAdapter = true)
data class SyncPendingResponse(val pending: Boolean)

@JsonClass(generateAdapter = true)
data class DeviceDto(
    val id: Int,
    val deviceName: String,
    val deviceIdentifier: String,
    val lastSeen: String?,
    val createdAt: String,
    val isOnline: Boolean,
)

@JsonClass(generateAdapter = true)
data class CreateNotificationRequest(
    val deviceId: String,
    val appName: String,
    val packageName: String,
    val title: String,
    val message: String,
    val notificationTime: String,
)

@JsonClass(generateAdapter = true)
data class NotificationDto(
    val id: Int,
    val deviceId: Int,
    val deviceName: String,
    val appName: String,
    val packageName: String,
    val title: String,
    val message: String,
    val notificationTime: String,
    val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class PagedNotifications(
    val items: List<NotificationDto>,
    val page: Int,
    val pageSize: Int,
    val totalCount: Int,
    val totalPages: Int,
)

// ---- Calls ----
@JsonClass(generateAdapter = true)
data class CallItem(
    val sourceKey: String,
    val number: String,
    val name: String?,
    val callType: String,
    val callTime: String,
    val durationSeconds: Int,
)

@JsonClass(generateAdapter = true)
data class UploadCallsRequest(val deviceId: String, val items: List<CallItem>)

@JsonClass(generateAdapter = true)
data class CallDto(
    val id: Int,
    val number: String,
    val name: String?,
    val callType: String,
    val callTime: String,
    val durationSeconds: Int,
    val deviceName: String,
    val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class PagedCalls(
    val items: List<CallDto>,
    val page: Int,
    val pageSize: Int,
    val totalCount: Int,
    val totalPages: Int,
)

// ---- SMS ----
@JsonClass(generateAdapter = true)
data class SmsItem(
    val sourceKey: String,
    val address: String,
    val body: String,
    val messageType: String,
    val messageTime: String,
)

@JsonClass(generateAdapter = true)
data class UploadSmsRequest(val deviceId: String, val items: List<SmsItem>)

@JsonClass(generateAdapter = true)
data class SmsDto(
    val id: Int,
    val address: String,
    val body: String,
    val messageType: String,
    val messageTime: String,
    val deviceName: String,
    val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class PagedSms(
    val items: List<SmsDto>,
    val page: Int,
    val pageSize: Int,
    val totalCount: Int,
    val totalPages: Int,
)

@JsonClass(generateAdapter = true)
data class UploadResult(val inserted: Int, val skipped: Int)
