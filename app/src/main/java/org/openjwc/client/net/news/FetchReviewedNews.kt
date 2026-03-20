package org.openjwc.client.net.news

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.openjwc.client.net.models.GetReviewedNoticeNetworkResult
import org.openjwc.client.net.models.GetReviewedNoticeSuccessResponse
import org.openjwc.client.net.models.NetService

private const val LABEL = "GetReviewedNews"

suspend fun NetService.fetchReviewedNews(
    auth: String,
    deviceId: String,
): GetReviewedNoticeNetworkResult = withContext(Dispatchers.IO) {
    runCatching {
        Log.d(LABEL, "Getting reviewed news...")
        val response = getReviewedNotices(
            "Bearer $auth",
            deviceId,
        )
        if (response.isSuccessful) {
            val rawBody = response.body()?.string()
            Log.d(LABEL, "Success: $rawBody")

            if (rawBody != null) {
                val successResponse = Json.decodeFromString<GetReviewedNoticeSuccessResponse>(rawBody)
                GetReviewedNoticeNetworkResult.Success(successResponse)
            } else {
                GetReviewedNoticeNetworkResult.Failure(response.code(), "Empty response body")
            }
        } else {
            val errorMsg = response.errorBody()?.string() ?: response.message()
            Log.e(LABEL, "Failure: ${response.code()} $errorMsg")
            GetReviewedNoticeNetworkResult.Failure(response.code(), errorMsg)
        }
    }.getOrElse { e ->
        Log.e(LABEL, "Exception in fetchNews: ${e.message}", e)
        GetReviewedNoticeNetworkResult.Error("Error: ${e.localizedMessage}")
    }
}