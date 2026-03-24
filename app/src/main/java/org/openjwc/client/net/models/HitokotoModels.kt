package org.openjwc.client.net.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Hitokoto(
    val text: String,
    val author: String? = null,
)

sealed class GetHitokotoResult {
    data class Success(val response: GetHitokotoSuccessResponse) : GetHitokotoResult()
    data class Failure(
        val code: Int,
        val msg: String
    ) : GetHitokotoResult()
    data class Error(
        val msg: String
    ) : GetHitokotoResult()
}

@Serializable
data class GetHitokotoSuccessResponse(
    @SerialName("msg") val message: String,
    @SerialName("data") val data: Hitokoto
)