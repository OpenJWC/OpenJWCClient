package org.openjwc.client.net.auth

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.openjwc.client.net.models.DeviceUnbindNetworkResult
import org.openjwc.client.net.models.DeviceUnbindSuccessResponse
import org.openjwc.client.net.models.NetService

private const val LABEL = "DeviceUnbind"

suspend fun NetService.deviceUnbind(
    auth: String,
    deviceId: String
): DeviceUnbindNetworkResult = withContext(Dispatchers.IO) {
    runCatching {
        Log.d(LABEL, "Requesting Unbinding device...")
        val response = postDeviceUnbind("Bearer $auth", deviceId)

        if (response.isSuccessful) {
            val rawBody = response.body()?.string()
            Log.d(LABEL, "Success: $rawBody")

            if (rawBody.isNullOrEmpty()) {
                DeviceUnbindNetworkResult.Failure(response.code(), "Empty response body")
            } else {
                val successResponse = Json.decodeFromString<DeviceUnbindSuccessResponse>(rawBody)
                DeviceUnbindNetworkResult.Success(successResponse)
            }
        } else {
            val errorMsg = response.errorBody()?.string() ?: response.message()
            Log.e(LABEL, "Failure: ${response.code()} $errorMsg")
            DeviceUnbindNetworkResult.Failure(response.code(), errorMsg)
        }
    }.getOrElse { e ->
        Log.e(LABEL, "Exception: ${e.message}", e)
        DeviceUnbindNetworkResult.Error("Error: ${e.localizedMessage}")
    }
}