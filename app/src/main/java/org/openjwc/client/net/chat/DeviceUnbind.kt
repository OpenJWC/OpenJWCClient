package org.openjwc.client.net.chat

import android.util.Log
import kotlinx.serialization.json.Json
import org.openjwc.client.net.models.DeviceUnbindNetworkResult
import org.openjwc.client.net.models.DeviceUnbindSuccessResponse
import org.openjwc.client.net.models.NetService

private const val LABEL = "DeviceUnbind"

suspend fun NetService.deviceUnbind(
    auth: String,
    deviceId: String
): DeviceUnbindNetworkResult {
    return try {
        Log.d(LABEL, "Requesting Unbinding device...")
        val response = postDeviceUnbind("Bearer $auth", deviceId)
        val rawBody = response.body()?.string()

        if (response.isSuccessful && rawBody != null) {
            Log.d(LABEL, "Success: $rawBody")
            val successResponse = try {
                Json.decodeFromString<DeviceUnbindSuccessResponse>(rawBody)
            } catch (e: Exception) {
                Log.e(LABEL, "Parsing message failure: ${e.message}")
                return DeviceUnbindNetworkResult.Failure(response.code(),"Parsing message failure")
            }
            DeviceUnbindNetworkResult.Success(successResponse)
        } else {
            Log.e(LABEL, "Failure: ${response.code()} ${response.message()}")
            DeviceUnbindNetworkResult.Failure(response.code(), response.message())
        }
    } catch (e: Exception) {
        Log.e(LABEL, "Exception: ${e.message}")
        DeviceUnbindNetworkResult.Error("Error: ${e.localizedMessage}")
    }
}