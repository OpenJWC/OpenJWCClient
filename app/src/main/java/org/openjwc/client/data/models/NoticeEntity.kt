package org.openjwc.client.data.models

import androidx.room.Entity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.openjwc.client.net.models.FetchedNotice

@Entity(
    tableName = "favorite_notices",
    primaryKeys = ["host", "port", "id"]
)
@Serializable
data class NoticeEntity(
    val host: String = "",
    val port: Int = 0,
    @SerialName("id")
    val id: String,
    val label: String,
    val title: String,
    val date: String,
    @SerialName("detail_url") val detailUrl: String,
    @SerialName("is_page") val isPage: Boolean,
    @SerialName("content_text") val contentText: String?,
    @SerialName("attachments") val attachmentUrls: List<String>?
)

fun NoticeEntity.toFetchedNotice() = FetchedNotice(
    id = id,
    label = label,
    title = title,
    date = date,
    detailUrl = detailUrl,
    isPage = isPage,
    contentText = contentText,
    attachmentUrls = attachmentUrls
)