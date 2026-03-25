package org.openjwc.client.net.auth

import org.openjwc.client.net.models.DevicesQueryResponseData
import org.openjwc.client.net.models.NetService
import org.openjwc.client.net.models.NetworkResult
import org.openjwc.client.net.models.SuccessResponse
import org.openjwc.client.net.models.fetch

suspend fun NetService.devicesQuery(
    auth: String,
    deviceId: String
): NetworkResult<SuccessResponse<DevicesQueryResponseData>> = fetch { getDevicesQuery("Bearer $auth", deviceId) }