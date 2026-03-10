package org.openjwc.client.net.chat

import android.util.Log
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import kotlinx.serialization.json.Json
import org.openjwc.client.net.models.ChatRequest
import org.openjwc.client.net.models.ChatService
import org.openjwc.client.net.models.ErrorResponse
import org.openjwc.client.net.models.NetworkResult
import org.openjwc.client.net.models.SuccessResponse
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private const val LABEL = "SendMessage"
object ChatClient {
    private const val BASE_URL = "http://101.132.106.186:8000/"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val apiService: ChatService = retrofit.create(ChatService::class.java)
}

suspend fun sendMessage(chatRequest: ChatRequest): NetworkResult {
    return try {
        val response = ChatClient.apiService.postQuery(chatRequest)
        val rawBody = response.body()?.string()

        if (response.isSuccessful && rawBody != null) {
            Log.d(LABEL, "Success: $rawBody")
            val cleanText = try {
                Json.decodeFromString<SuccessResponse>(rawBody).reply
            } catch (e: Exception) {
                Log.e(LABEL, "Parsing message failure: ${e.message}")
                rawBody
            }
            NetworkResult.Success(cleanText)
        }
        else if (response.code() == 422) {
            Log.e(LABEL, "422: $response")
            val errorBody = response.errorBody()?.string()

            if (errorBody.isNullOrBlank()) {
                return NetworkResult.Failure(422, "Unknown error")
            }

            try {
                val errorObj = Json.decodeFromString<ErrorResponse>(errorBody)
                NetworkResult.ValidationError(errorObj)
            } catch (e: Exception) {
                Log.e(LABEL, "Parsing 422 message failure: ${e.message}")
                NetworkResult.Failure(422, "Parsing 422 message failure")
            }
        } else {
            Log.e(LABEL, "Failure: ${response.code()} ${response.message()}")
            NetworkResult.Failure(response.code(), response.message())
        }
    } catch (e: Exception) {
        Log.e(LABEL, "Exception: ${e.message}")
        NetworkResult.Error("Error: ${e.localizedMessage}")
    }
}
