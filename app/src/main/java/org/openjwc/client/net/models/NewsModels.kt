package org.openjwc.client.net.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FetchNewsSuccessResponse(
    @SerialName("msg") val message: String,
    val data: FetchNewsResponseData
)

@Serializable
data class FetchNewsResponseData(
    @SerialName("total_returned") val totalReturned: Int,
    @SerialName("total_label") val totalLabel: Int,
    @SerialName("notices") val notices: List<Notice>
)

@Serializable
data class Notice(
    @SerialName("id") val id: String,
    val label: String,
    val title: String,
    val date: String,
    @SerialName("detail_url") val detailUrl: String,
    @SerialName("is_page") val isPage: Boolean
)

sealed class FetchNewsNetworkResult {
    data class Success(val response: FetchNewsSuccessResponse) : FetchNewsNetworkResult()
    data class Failure(
        val code: Int,
        val msg: String
    ) : FetchNewsNetworkResult()
    data class Error(
        val msg: String
    ) : FetchNewsNetworkResult()
}

@Serializable
data class FetchLabelsSuccessResponse(
    @SerialName("msg") val message: String,
    val data: FetchLabelsResponseData
)

@Serializable
data class FetchLabelsResponseData(
    val labels: List<String>
)

sealed class FetchLabelsNetworkResult {
    data class Success(val response: FetchLabelsSuccessResponse) : FetchLabelsNetworkResult()
    data class Failure(
        val code: Int,
        val msg: String
    ) : FetchLabelsNetworkResult()
    data class Error(
        val msg: String
    ) : FetchLabelsNetworkResult()
}