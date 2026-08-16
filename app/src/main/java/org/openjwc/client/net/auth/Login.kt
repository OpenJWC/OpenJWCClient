package org.openjwc.client.net.auth

import org.openjwc.client.log.Logger
import org.openjwc.client.net.models.LoginRequestBody
import org.openjwc.client.net.models.LoginSuccessResponse
import org.openjwc.client.net.models.NetService
import org.openjwc.client.net.models.NetworkResult
import org.openjwc.client.net.models.RegisterRequestBody
import org.openjwc.client.net.models.SuccessResponse
import org.openjwc.client.net.models.fetch
import java.util.UUID

suspend fun NetService.login(
    auth: String?,
    deviceId: String,
    account: String,
    passwordHash: String,
    deviceName: String
): NetworkResult<SuccessResponse<LoginSuccessResponse>> =
    fetch(level = Logger.Level.NONE) {
        postLogin(
            auth,
            deviceId,
            UUID.randomUUID().toString(),
            "1.2.0",
            LoginRequestBody(account, passwordHash, deviceName)
        )
    }

suspend fun NetService.register(
    auth: String?,
    deviceId: String,
    username: String,
    passwordHash: String,
    email: String,
): NetworkResult<SuccessResponse<Map<String, String>>> =
    fetch(level = Logger.Level.NONE) {
        postRegister(
            auth,
            deviceId,
            UUID.randomUUID().toString(),
            "1.2.0",
            RegisterRequestBody(username, passwordHash, email)
        )
    }