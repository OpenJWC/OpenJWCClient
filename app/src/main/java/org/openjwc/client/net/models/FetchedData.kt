package org.openjwc.client.net.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SuccessResponse<T>(
    @SerialName("msg") val message: String,
    val data: T
)

sealed class NetworkResult<out T> {
    data class Success<T>(val response: T) : NetworkResult<T>()
    data class Failure(
        val code: Int,
        val msg: String
    ) : NetworkResult<Nothing>()
    data class Error(
        val msg: String
    ) : NetworkResult<Nothing>()
}

