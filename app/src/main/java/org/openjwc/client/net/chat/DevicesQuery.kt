package org.openjwc.client.net.chat

import android.util.Log
import kotlinx.serialization.json.Json
import org.openjwc.client.net.models.DevicesQueryNetworkResult
import org.openjwc.client.net.models.DevicesQuerySuccessResponse
import org.openjwc.client.net.models.NetService

private const val LABEL = "DevicesQuery"

suspend fun NetService.devicesQuery(
    auth: String,
    deviceId: String
): DevicesQueryNetworkResult {
    return try {
        val response = getDevicesQuery(auth, deviceId)
        val rawBody = response.body()?.string()

        if (response.isSuccessful && rawBody != null) {
            Log.d(LABEL, "Success: $rawBody")
            val successResponse = try {
                Json.decodeFromString<DevicesQuerySuccessResponse>(rawBody)
            } catch (e: Exception) {
                Log.e(LABEL, "Parsing message failure: ${e.message}")
                return DevicesQueryNetworkResult.Failure(response.code(),"Parsing message failure")
            }
            DevicesQueryNetworkResult.Success(successResponse)
        } else {
            Log.e(LABEL, "Failure: ${response.code()} ${response.message()}")
            DevicesQueryNetworkResult.Failure(response.code(), response.message())
        }
    } catch (e: Exception) {
        Log.e(LABEL, "Exception: ${e.message}")
        DevicesQueryNetworkResult.Error("Error: ${e.localizedMessage}")
    }
}
