package org.openjwc.client.net.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReviewedNotice(
    val id: String,
    val label: String,
    val title: String,
    val date: String,
    @SerialName("detail_url") val detailUrl: String,
    @SerialName("is_page") val isPage: Boolean,
    val status: String,
    val review: String // 审核结果的原因
)
@Serializable
data class ReviewedNoticesData(
    val total: Int,
    val notices: List<ReviewedNotice>?
)
@Serializable
data class GetReviewedNoticeSuccessResponse(
    @SerialName("msg") val message: String,
    @SerialName("data") val data: ReviewedNoticesData
)
sealed class GetReviewedNoticeNetworkResult {
    data class Success(val response: GetReviewedNoticeSuccessResponse) : GetReviewedNoticeNetworkResult()
    data class Failure(
        val code: Int,
        val msg: String
    ) : GetReviewedNoticeNetworkResult()
    data class Error(
        val msg: String
    ) : GetReviewedNoticeNetworkResult()
}