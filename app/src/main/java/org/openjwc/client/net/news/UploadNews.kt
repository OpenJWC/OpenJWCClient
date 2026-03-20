package org.openjwc.client.net.news

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.openjwc.client.net.models.UploadedNotice
import org.openjwc.client.net.models.NetService
import org.openjwc.client.net.models.PostNoticeNetworkResult
import org.openjwc.client.net.models.PostNoticeSuccessResponse

private const val LABEL = "UploadNews"

suspend fun NetService.fetchNews(
    auth: String,
    deviceId: String,
    uploadedNotice: UploadedNotice
): PostNoticeNetworkResult = withContext(Dispatchers.IO) {
    runCatching {
        Log.d(LABEL, "Requesting news...")
        val response = postNotice(
            "Bearer $auth",
            deviceId,
            uploadedNotice,
        )
        if (response.isSuccessful) {
            val rawBody = response.body()?.string()
            Log.d(LABEL, "Success: $rawBody")

            if (rawBody != null) {
                val successResponse = Json.decodeFromString<PostNoticeSuccessResponse>(rawBody)
                PostNoticeNetworkResult.Success(successResponse)
            } else {
                PostNoticeNetworkResult.Failure(response.code(), "Empty response body")
            }
        } else {
            val errorMsg = response.errorBody()?.string() ?: response.message()
            Log.e(LABEL, "Failure: ${response.code()} $errorMsg")
            PostNoticeNetworkResult.Failure(response.code(), errorMsg)
        }
    }.getOrElse { e ->
        Log.e(LABEL, "Exception in fetchNews: ${e.message}", e)
        PostNoticeNetworkResult.Error("Error: ${e.localizedMessage}")
    }
}