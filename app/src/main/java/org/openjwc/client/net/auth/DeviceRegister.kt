package org.openjwc.client.net.auth

import org.openjwc.client.net.models.NetService
import org.openjwc.client.net.models.NetworkResult
import org.openjwc.client.net.models.SuccessResponse
import org.openjwc.client.net.models.fetch

suspend fun NetService.deviceRegister(
    auth: String,
    deviceId: String
): NetworkResult<SuccessResponse<Map<String, String>>> = fetch { postRegister("Bearer $auth", deviceId) }