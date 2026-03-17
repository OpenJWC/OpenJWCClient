package org.openjwc.client.net.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class DevicesQuerySuccessResponse(
    @SerialName("msg") val message: String,
    val data: DevicesQueryResponseData
)

@Serializable
data class DevicesQueryResponseData(
    @SerialName("total") val limitedDeviceCount: Int,
    @SerialName("bound_devices") val deviceIDs: List<String>
)

sealed class DevicesQueryNetworkResult {
    data class Success(val response: DevicesQuerySuccessResponse) : DevicesQueryNetworkResult()
    data class Failure(
        val code: Int,
        val msg: String
    ) : DevicesQueryNetworkResult()
    data class Error(
        val msg: String
    ) : DevicesQueryNetworkResult()
}


@Serializable
data class DeviceUnbindSuccessResponse(
    val detail: String
)

sealed class DeviceUnbindNetworkResult {
    data class Success(val response: DeviceUnbindSuccessResponse) : DeviceUnbindNetworkResult()
    data class Failure(
        val code: Int,
        val msg: String
    ): DeviceUnbindNetworkResult()

    data class Error(
        val msg: String
    ): DeviceUnbindNetworkResult()
}