package org.openjwc.client.net.auth

import org.openjwc.client.net.models.NetService
import org.openjwc.client.net.models.NetworkResult
import org.openjwc.client.net.models.fetch

suspend fun NetService.deviceUnbind(
    auth: String,
    deviceId: String
): NetworkResult<String> = fetch { postDeviceUnbind("Bearer $auth", deviceId) }