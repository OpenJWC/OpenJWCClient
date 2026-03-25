package org.openjwc.client.net.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
@Serializable
data class DevicesQueryResponseData(
    @SerialName("total") val limitedDeviceCount: Int,
    @SerialName("bound_devices") val deviceIDs: List<String>
)

@Serializable
data class DevicesUnbindSuccessResponse(
    val detail: String
)