package org.openjwc.client.net.models

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

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
    fun createService(host: String, port: Int): ChatService {
        val baseUrl = "http://$host:$port/"
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ChatService::class.java)
    }
}

sealed class NetworkResult {
    data class Success(val content: String) : NetworkResult()
    data class ValidationError(val errors: ChatErrorResponse) : NetworkResult()
    data class Failure(
        val code: Int,
        val msg: String
    ) : NetworkResult()
    data class Error(
        val msg: String
    ) : NetworkResult()
}