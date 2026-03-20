package org.openjwc.client.net.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UploadedNotice(
    val label: String,
    val title: String,
    val date: String,
    @SerialName("detail_url") val detailUrl: String,
    @SerialName("is_page") val isPage: Boolean,
    val content: UploadedNoticeContent
)

@Serializable
data class UploadedNoticeContent(
    val text: String,
    @SerialName("attachment_urls") val attachmentUrls: List<String>
)

sealed class PostNoticeNetworkResult {
    data class Success(val response: PostNoticeSuccessResponse) : PostNoticeNetworkResult()
    data class Failure(
        val code: Int,
        val msg: String
    ) : PostNoticeNetworkResult()
    data class Error(
        val msg: String
    ) : PostNoticeNetworkResult()
}

@Serializable
data class PostNoticeSuccessResponse(
    @SerialName("msg") val message: String,
    @SerialName("data") val data: List<String>?
)