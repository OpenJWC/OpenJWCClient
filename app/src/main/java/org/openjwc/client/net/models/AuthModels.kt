package org.openjwc.client.net.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming


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
