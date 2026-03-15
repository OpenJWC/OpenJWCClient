package org.openjwc.client.net.models

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

interface AuthService {
    @Streaming
    @POST("api/client/device/unbind")
    suspend fun deviceUnbind(
        @Header("Authorization") auth: String,
        @Header("X-Device-ID") deviceId: String,
    ): Response<ResponseBody>
}