package org.openjwc.client.net.models

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

object CheckUpdateClient {
    private val loggingInterceptor = okhttp3.logging.HttpLoggingInterceptor().apply {
        level = okhttp3.logging.HttpLoggingInterceptor.Level.HEADERS
    }
    private val serviceCache = mutableMapOf<Proxy, CheckUpdateService>()
    private val clientCache = mutableMapOf<Proxy, OkHttpClient>()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun getService(proxy: Proxy): CheckUpdateService {
        val baseUrl = "https://api.github.com/"
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

        return serviceCache.getOrPut(proxy) {
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(CheckUpdateService::class.java)
        }
    }
}

interface CheckUpdateService {
    @GET("repos/OpenJWC/OpenJWCClient/releases/latest")
    suspend fun getLatest(
        @Header("Accept") accept: String = "application/vnd.github.v3+json",
        @Header("X-GitHub-Api-Version") auth: String = "2022-11-28",
    ): Response<ResponseBody>
}