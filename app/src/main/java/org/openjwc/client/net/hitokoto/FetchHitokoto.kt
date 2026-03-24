package org.openjwc.client.net.hitokoto

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.openjwc.client.net.models.GetHitokotoResult
import org.openjwc.client.net.models.GetHitokotoSuccessResponse
import org.openjwc.client.net.models.NetService

private const val LABEL = "FetchHitokoto"

suspend fun NetService.fetchHitokoto(
    auth: String,
    deviceId: String
): GetHitokotoResult = withContext(Dispatchers.IO) {
    runCatching {
        Log.d(LABEL, "Requesting Hitokoto...")
        val response = getHitokoto("Bearer $auth", deviceId)

        if (response.isSuccessful) {
            val rawBody = response.body()?.string()
            Log.d(LABEL, "Success: $rawBody")

            if (rawBody.isNullOrEmpty()) {
                GetHitokotoResult.Failure(response.code(), "Empty response body")
            } else {
                val successResponse = Json.decodeFromString<GetHitokotoSuccessResponse>(rawBody)
                GetHitokotoResult.Success(successResponse)
            }
        } else {
            val errorMsg = response.errorBody()?.string() ?: response.message()
            Log.e(LABEL, "Failure: ${response.code()} $errorMsg")
            GetHitokotoResult.Failure(response.code(), errorMsg)
        }
    }.getOrElse { e ->
        Log.e(LABEL, "Exception: ${e.message}", e)
        GetHitokotoResult.Error("Error: ${e.localizedMessage}")
    }
}