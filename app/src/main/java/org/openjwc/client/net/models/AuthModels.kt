package org.openjwc.client.net.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DevicesQueryResponseData(
//    @SerialName("total") val limitedDeviceCount: Int,
    @SerialName("devices") val deviceQueries: List<DeviceQuery>
)


@Serializable
data class DeviceQuery(
    @SerialName("device_uuid") val deviceUUID: String,
    @SerialName("device_name") val deviceName: String,
    @SerialName("last_login")  val lastLogin: String,
)

@Serializable
data class DevicesUnbindSuccessResponse(
    val detail: String
)

@Serializable
data class LoginSuccessResponse(
    val token: String,
    val username: String,
    val email: String
)

@Serializable
data class LoginRequestBody(
    val account: String,
    @SerialName("password_hash") val passwordHash: String,
    @SerialName("device_name") val deviceName: String
)

@Serializable
data class RegisterRequestBody(
    val username: String,
    @SerialName("password_hash") val passwordHash: String,
    val email: String,
)