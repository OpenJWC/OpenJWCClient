package org.openjwc.client.net.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

@Serializable
data class ChatHistory(
    val role: String,
    val content: String
)

@Serializable
data class ChatRequestBody(
    @SerialName("notice_id") val noticeId: String,
    @SerialName("user_query") val userQuery: String,
    val stream: Boolean = false,
    val history: List<ChatHistory> = emptyList()
)
@Serializable
data class ChatSuccessResponse(
    val status: String,
    val reply: String
)

@Serializable
data class ChatErrorResponse(
    val detail: List<ValidationError>
)

@Serializable
data class ValidationError(
    val loc: List<JsonElement>,
    val msg: String,
    val type: String,
    val input: String? = null,
    val ctx: JsonObject? = null
)

interface ChatService {
    @Streaming
    @POST("api/chat")
    suspend fun postQueryStream(
        @Header("Authorization") auth: String,
        @Header("X-Device-ID") deviceId: String,
        @Body request: ChatRequestBody
    ): Response<ResponseBody>
}