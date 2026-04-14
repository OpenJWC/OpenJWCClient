package org.openjwc.client.net.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class ChatHistory(
    val role: String,
    val content: String
)

@Serializable
data class ChatRequestBody(
    @SerialName("notice_ids") val noticeId: List<String>,
    @SerialName("user_query") val userQuery: String,
    val stream: Boolean = false,
    val history: List<ChatHistory> = emptyList()
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

sealed class ChatNetworkResult {
    data class Success(val content: String) : ChatNetworkResult()
    data class ValidationError(val errors: ChatErrorResponse) : ChatNetworkResult()
    data class Failure(
        val code: Int,
        val msg: String
    ) : ChatNetworkResult()
    data class Error(
        val msg: String
    ) : ChatNetworkResult()
}