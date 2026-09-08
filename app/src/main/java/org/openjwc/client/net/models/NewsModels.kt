package org.openjwc.client.net.models

import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.openjwc.client.data.models.NoticeEntity

@Serializable
data class FetchNewsResponseData(
    @SerialName("total_returned") val totalReturned: Int,
    @SerialName("total_label") val totalLabel: Int,
    @SerialName("notices") val fetchedNotices: List<FetchedNotice>
)

@Serializable
data class FetchedNotice(
    @PrimaryKey val id: String,
    val label: String,
    val title: String,
    val date: String,
    @SerialName("detail_url") val detailUrl: String,
    @SerialName("is_page") val isPage: Boolean,
    @SerialName("content_text") val contentText: String?,
    @SerialName("attachments") val attachmentUrls: List<String>?
)

fun FetchedNotice.toNoticeEntity(host: String = "", port: Int = 0) = NoticeEntity(
    host = host,
    port = port,
    id = id,
    label = label,
    title = title,
    date = date,
    detailUrl = detailUrl,
    isPage = isPage,
    contentText = contentText,
    attachmentUrls = attachmentUrls
)

@Serializable
data class FetchLabelsResponseData(
    val labels: List<String>
)
