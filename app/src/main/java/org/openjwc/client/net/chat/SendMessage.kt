package org.openjwc.client.net.chat

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okio.ByteString.Companion.encodeUtf8
import org.openjwc.client.net.models.ChatRequest
import org.openjwc.client.net.models.ChatService
import org.openjwc.client.net.models.ErrorResponse
import org.openjwc.client.net.models.NetworkResult
import org.openjwc.client.net.models.SuccessResponse
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private const val LABEL = "SendMessage"
private const val BASE_URL = "http://101.132.106.186:8000/"

object ChatClient {
    private val loggingInterceptor = okhttp3.logging.HttpLoggingInterceptor().apply {
        level = okhttp3.logging.HttpLoggingInterceptor.Level.HEADERS
    }

    val okHttpClient = okhttp3.OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS) // 流式读取需要较长的 ReadTimeout
        .build()
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val apiService: ChatService = retrofit.create(ChatService::class.java)
}

// 先留在这边吧，万一有用呢（
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
        } else if (response.code() == 422) {
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

fun sendMessageStream(chatRequest: ChatRequest): Flow<NetworkResult> = flow {
    try {
        val response = ChatClient.apiService.postQueryStream(chatRequest)
        if (!response.isSuccessful) {
            emit(NetworkResult.Failure(response.code(), response.message()))
            return@flow
        }
        val source = response.body()?.source() ?: return@flow
        val buffer = okio.Buffer()
        var accumulatedText = ""
        val prefix = "data:"
        val separator = "\n\ndata:"

        while (true) {
            val read = source.read(buffer, 8192)
            if (read == -1L && buffer.size == 0L) break
            while (true) {
                val start = buffer.indexOf(prefix.encodeUtf8())
                if (start == -1L) break

                val next = buffer.indexOf(separator.encodeUtf8(), start + prefix.length)
                if (next == -1L) break

                // 丢弃 data: 之前的数据
                if (start > 0) buffer.skip(start)

                // 跳过 "data:"
                buffer.skip(prefix.length.toLong())

                val payloadLength = next - start - prefix.length
                var chunk = buffer.readUtf8(payloadLength)

                // 去掉前导空格（SSE规范）
                if (chunk.startsWith(" ")) {
                    chunk = chunk.substring(1)
                }
                // 消耗 "\n\n"
                buffer.skip(2)

                Log.d(LABEL, "Chunk: [${chunk.replace("\n","\\n")}]")
                if (chunk == "[DONE]") return@flow
                accumulatedText += chunk
                emit(NetworkResult.Success(accumulatedText))
            }
        }

    } catch (e: Exception) {

        emit(NetworkResult.Error("连接中断: ${e.localizedMessage}"))
    }

}.flowOn(Dispatchers.IO)