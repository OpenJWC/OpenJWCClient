package org.openjwc.client.net.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FetchNewsResponseData(
    @SerialName("total_returned") val totalReturned: Int,
    @SerialName("total_label") val totalLabel: Int,
    @SerialName("notices") val fetchedNotices: List<FetchedNotice>
)

@Serializable
data class FetchedNotice(
    @SerialName("id") val id: String,
    val label: String,
    val title: String,
    val date: String,
    @SerialName("detail_url") val detailUrl: String,
    @SerialName("is_page") val isPage: Boolean,
    @SerialName("content_text") val contentText: String?,
    @SerialName("attachments") val attachmentUrls: List<String>?
)

@Serializable
data class FetchLabelsResponseData(
    val labels: List<String>
)
