package org.openjwc.client.net.auth

import org.openjwc.client.net.models.DeviceUnbindRequestBody
import org.openjwc.client.net.models.DevicesUnbindSuccessResponse
import org.openjwc.client.net.models.NetService
import org.openjwc.client.net.models.NetworkResult
import org.openjwc.client.net.models.fetch

suspend fun NetService.deviceUnbind(
    auth: String,
    thisDeviceId: String,
    unbindDeviceId: String
): NetworkResult<DevicesUnbindSuccessResponse> = fetch {
    postDeviceUnbind(
        "Bearer $auth", thisDeviceId,
        DeviceUnbindRequestBody(unbindDeviceId)
    )
}