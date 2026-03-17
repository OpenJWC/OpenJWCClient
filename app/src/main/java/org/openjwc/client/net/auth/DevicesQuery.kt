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
    runCatching {
        Log.d(LABEL, "Requesting devices...")
        val response = getDevicesQuery("Bearer $auth", deviceId)

        if (response.isSuccessful) {
            // 注意：string() 读取流是耗时操作且只能调用一次
            val rawBody = response.body()?.string()
            Log.d(LABEL, "Success: $rawBody")

            if (rawBody != null) {
                val successResponse = Json.decodeFromString<DevicesQuerySuccessResponse>(rawBody)
                DevicesQueryNetworkResult.Success(successResponse)
            } else {
                DevicesQueryNetworkResult.Failure(response.code(), "Empty response body")
            }
        } else {
            val errorMsg = response.errorBody()?.string() ?: response.message()
            Log.e(LABEL, "Failure: ${response.code()} $errorMsg")
            DevicesQueryNetworkResult.Failure(response.code(), errorMsg)
        }
    }.getOrElse { e ->
        Log.e(LABEL, "Exception in devicesQuery: ${e.message}", e)
        DevicesQueryNetworkResult.Error("Error: ${e.localizedMessage}")
    }
}