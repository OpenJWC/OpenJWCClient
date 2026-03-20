package org.openjwc.client.net.models

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming

object NetClient {
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

    private val serviceCache = mutableMapOf<String, NetService>()

    fun getService(host: String, port: Int): NetService {
        val baseUrl = "http://$host:$port/"
        // 如果缓存里有，直接返回；没有则创建并存入缓存
        return serviceCache.getOrPut(baseUrl) {
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(NetService::class.java)
        }
    }
}

interface NetService {
    @Streaming
    @POST("api/v1/client/chat")
    suspend fun postChatQueryStream(
        @Header("Authorization") auth: String,
        @Header("X-Device-ID") deviceId: String,
        @Body request: ChatRequestBody
    ): Response<ResponseBody>

    @POST("api/v1/client/device/unbind")
    suspend fun postDeviceUnbind(
        @Header("Authorization") auth: String,
        @Header("X-Device-ID") deviceId: String,
    ): Response<ResponseBody>

//    @Headers("Content-Type: application/json")
    @GET("api/v1/client/device")
    suspend fun getDevicesQuery(
        @Header("Authorization") auth: String,
        @Header("X-Device-ID") deviceId: String,
    ): Response<ResponseBody>

    @GET("api/v1/client/notices")
    suspend fun getNotices(
        @Header("Authorization") auth: String,
        @Header("X-Device-ID") deviceId: String,
        @Query("label") label: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<ResponseBody>

    @GET("api/v1/client/notices/labels")
    suspend fun getLabels(
        @Header("Authorization") auth: String,
        @Header("X-Device-ID") deviceId: String,
    ): Response<ResponseBody>

    @POST("api/v1/client/submissions")
    suspend fun postNotice(
        @Header("Authorization") auth: String,
        @Header("X-Device-ID") deviceId: String,
        @Body uploadedNotice: UploadedNotice
    ): Response<ResponseBody>

    @GET("api/v1/client/submissions/my")
    suspend fun getReviewedNotices(
        @Header("Authorization") auth: String,
        @Header("X-Device-ID") deviceId: String,
    ): Response<ResponseBody>
}