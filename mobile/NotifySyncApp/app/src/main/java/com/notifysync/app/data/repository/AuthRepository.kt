package com.notifysync.app.data.repository

import android.content.Context
import com.notifysync.app.data.api.LoginRequest
import com.notifysync.app.data.api.RegisterDeviceRequest
import com.notifysync.app.data.api.RegisterRequest
import com.notifysync.app.data.api.RetrofitProvider
import com.notifysync.app.data.local.TokenStore
import com.notifysync.app.util.DeviceInfo

class AuthRepository(
    private val context: Context,
    private val tokenStore: TokenStore,
) {
    private val api get() = RetrofitProvider.api(tokenStore)

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val res = api.login(LoginRequest(email.trim(), password))
        val body = res.body()
        require(res.isSuccessful && body != null) { errorMessage(res.code()) }
        tokenStore.saveAuth(body.token, body.email, "${body.firstName} ${body.lastName}".trim())
        registerThisDevice()
    }

    suspend fun register(email: String, password: String, first: String, last: String): Result<Unit> =
        runCatching {
            val res = api.register(RegisterRequest(email.trim(), password, first.trim(), last.trim()))
            val body = res.body()
            require(res.isSuccessful && body != null) { errorMessage(res.code()) }
            tokenStore.saveAuth(body.token, body.email, "${body.firstName} ${body.lastName}".trim())
            registerThisDevice()
        }

    private suspend fun registerThisDevice() {
        val identifier = DeviceInfo.deviceIdentifier(context)
        val name = DeviceInfo.deviceName()
        val res = api.registerDevice(RegisterDeviceRequest(name, identifier))
        res.body()?.let { tokenStore.saveDevice(it.id.toString(), it.deviceIdentifier, it.deviceName) }
    }

    suspend fun logout() = tokenStore.clear()

    private fun errorMessage(code: Int): String = when (code) {
        401 -> "Invalid email or password."
        409 -> "An account with this email already exists."
        in 500..599 -> "Server error. Please try again."
        else -> "Request failed ($code)."
    }
}
