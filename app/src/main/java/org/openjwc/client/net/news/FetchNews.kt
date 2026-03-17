package org.openjwc.client.net.news

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.openjwc.client.net.models.FetchNewsNetworkResult
import org.openjwc.client.net.models.FetchNewsSuccessResponse
import org.openjwc.client.net.models.NetService

private const val LABEL = "FetchNews"

suspend fun NetService.fetchNews(
    auth: String,
    deviceId: String,
    label: String,
    page: Int,
    size: Int
): FetchNewsNetworkResult = withContext(Dispatchers.IO) {
    runCatching {
        Log.d(LABEL, "Requesting devices...")
        val response = getNotices(
            "Bearer $auth",
            deviceId,
            label,
            page,
            size
        )
        if (response.isSuccessful) {
            val rawBody = response.body()?.string()
            Log.d(LABEL, "Success: $rawBody")

            if (rawBody != null) {
                val successResponse = Json.decodeFromString<FetchNewsSuccessResponse>(rawBody)
                FetchNewsNetworkResult.Success(successResponse)
            } else {
                FetchNewsNetworkResult.Failure(response.code(), "Empty response body")
            }
        } else {
            val errorMsg = response.errorBody()?.string() ?: response.message()
            Log.e(LABEL, "Failure: ${response.code()} $errorMsg")
            FetchNewsNetworkResult.Failure(response.code(), errorMsg)
        }
    }.getOrElse { e ->
        Log.e(LABEL, "Exception in devicesQuery: ${e.message}", e)
        FetchNewsNetworkResult.Error("Error: ${e.localizedMessage}")
    }
}
