package org.openjwc.client.net.models

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import org.openjwc.client.log.Logger
import retrofit2.Response

@Serializable
data class SuccessResponse<T>(
    @SerialName("msg") val message: String,
    val data: T
)

sealed class NetworkResult<out T> {
    data class Success<T>(val response: T) : NetworkResult<T>()
    data class Failure(
        val code: Int,
        val msg: String
    ) : NetworkResult<Nothing>()

    data class Error(
        val msg: String
    ) : NetworkResult<Nothing>()
}

val networkJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
}

/** 统一的 Retrofit 响应解析（仅检查更新还在用）。 */
suspend inline fun <reified T> fetch(
    label: String = "NetworkFetch",
    level: Logger.Level = Logger.Level.DEBUG,
    crossinline request: suspend () -> Response<ResponseBody>,
): NetworkResult<T> = withContext(Dispatchers.IO) {
    runCatching {
        val response = request()
        if (response.isSuccessful) {
            val rawBody = response.body()?.string()
            Logger.log(label, "Success: $rawBody", level)

            if (rawBody.isNullOrEmpty()) {
                NetworkResult.Failure(response.code(), "Empty response body")
            } else {
                val successResponse = networkJson.decodeFromString<T>(rawBody)
                NetworkResult.Success(successResponse)
            }
        } else {
            val errorMsg = response.errorBody()?.string() ?: response.message()
            Logger.e(label, "Failure: ${response.code()} $errorMsg")
            NetworkResult.Failure(response.code(), errorMsg)
        }
    }.getOrElse { e ->
        if (e is kotlinx.coroutines.CancellationException) throw e
        Logger.e(label, "Exception: ${e.message}", e)
        NetworkResult.Error("Error: ${e.localizedMessage}")
    }
}
