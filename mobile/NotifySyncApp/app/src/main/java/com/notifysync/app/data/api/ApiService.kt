package com.notifysync.app.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST("api/device/register")
    suspend fun registerDevice(@Body body: RegisterDeviceRequest): Response<DeviceDto>

    @POST("api/device/heartbeat")
    suspend fun heartbeat(@Body body: HeartbeatRequest): Response<Unit>

    @GET("api/device/sync-pending")
    suspend fun syncPending(@Query("identifier") identifier: String): Response<SyncPendingResponse>

    @GET("api/notifications")
    suspend fun getNotifications(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): Response<PagedNotifications>

    @POST("api/calls")
    suspend fun uploadCalls(@Body body: UploadCallsRequest): Response<UploadResult>

    @GET("api/calls")
    suspend fun getCalls(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): Response<PagedCalls>

    @POST("api/messages")
    suspend fun uploadMessages(@Body body: UploadSmsRequest): Response<UploadResult>

    @GET("api/messages")
    suspend fun getMessages(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): Response<PagedSms>

    @POST("api/notifications")
    suspend fun createNotification(@Body body: CreateNotificationRequest): Response<NotificationDto>
}
