package org.openjwc.client.net.models

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.openjwc.client.log.Logger
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming

sealed class Proxy {
    class NoProxy : Proxy()
    data class HttpProxy(val host: String, val port: Int) : Proxy()
    data class SocksProxy(val host: String, val port: Int) : Proxy()
}

object NetClient {
    private val loggingInterceptor = okhttp3.logging.HttpLoggingInterceptor().apply {
        level = okhttp3.logging.HttpLoggingInterceptor.Level.HEADERS
    }
    private val serviceCache = mutableMapOf<Pair<String, Proxy>, NetService>()
    private val clientCache = mutableMapOf<Proxy, OkHttpClient>()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun getService(host: String, port: Int, useHttp: Boolean, proxy: Proxy): NetService {
        val prefix = if (useHttp) "http" else "https"
        val baseUrl = "$prefix://$host:$port/"
        val proxyArgument = when (proxy) {
            is Proxy.NoProxy -> java.net.Proxy.NO_PROXY
            is Proxy.HttpProxy -> java.net.Proxy(java.net.Proxy.Type.HTTP, java.net.InetSocketAddress(proxy.host, proxy.port))
            is Proxy.SocksProxy -> java.net.Proxy(java.net.Proxy.Type.SOCKS, java.net.InetSocketAddress(proxy.host, proxy.port))
        }
        val okHttpClient = clientCache.getOrPut(proxy) {
            OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .proxy(proxyArgument)
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build()
        }

        return serviceCache.getOrPut(Pair(baseUrl, proxy)) {
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

    @GET("api/v1/client/motto")
    suspend fun getHitokoto(
        @Header("Authorization") auth: String,
        @Header("X-Device-ID") deviceId: String,
    ): Response<ResponseBody>

    @POST("api/v1/client/register")
    suspend fun postRegister(
        @Header("Authorization") auth: String,
        @Header("X-Device-ID") deviceId: String,
    ): Response<ResponseBody>

    @POST("api/v2/client/login")
    suspend fun postLogin(
        @Header("Authorization") auth: String?,
        @Header("X-Device-ID") deviceId: String,
        @Header("X-Request-ID") requestId: String,
        @Header("X-Client-Version") clientVersion: String,
        @Body request: LoginRequestBody
    ): Response<ResponseBody>

    @POST("api/v2/client/auth/register")
    suspend fun postRegister(
        @Header("Authorization") auth: String?,
        @Header("X-Device-ID") deviceId: String,
        @Header("X-Request-ID") requestId: String,
        @Header("X-Client-Version") clientVersion: String,
        @Body request: RegisterRequestBody
    ): Response<ResponseBody>
}

suspend inline fun <reified T> fetch(
    label: String = "NetworkFetch",
    level: Logger.Level = Logger.Level.DEBUG,
    crossinline request: suspend () -> Response<ResponseBody>,
): NetworkResult<T> = withContext(Dispatchers.IO) {
    runCatching {
        val response = request()
        val networkJson = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }
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
        Logger.e(label, "Exception: ${e.message}", e)
        NetworkResult.Error("Error: ${e.localizedMessage}")
    }
}