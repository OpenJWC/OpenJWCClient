package org.openjwc.client.net.auth

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.openjwc.client.net.models.DevicesQueryNetworkResult
import org.openjwc.client.net.models.DevicesQuerySuccessResponse
import org.openjwc.client.net.models.NetService

private const val LABEL = "DevicesQuery"

suspend fun NetService.devicesQuery(
    auth: String,
    deviceId: String
): DevicesQueryNetworkResult = withContext(Dispatchers.IO) {
    try {
        Log.d(LABEL, "Requesting devices...")
        val response = getDevicesQuery("Bearer $auth", deviceId)
        val rawBody = response.body()?.string()

        if (response.isSuccessful && rawBody != null) {
            try {
                val successResponse = Json.decodeFromString<DevicesQuerySuccessResponse>(rawBody)
                DevicesQueryNetworkResult.Success(successResponse)
            } catch (e: Exception) {
                DevicesQueryNetworkResult.Failure(response.code(), "Parsing failure: ${e.message}")
            }
        } else {
            DevicesQueryNetworkResult.Failure(response.code(), response.message())
        }
    } catch (e: Exception) {
        DevicesQueryNetworkResult.Error("Error: ${e.localizedMessage}")
    }
}