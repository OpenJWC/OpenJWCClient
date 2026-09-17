package org.openjwc.client.net.models

import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FetchedNotice(
    @PrimaryKey val id: String,
    val label: String,
    val title: String,
    val date: String,
    @SerialName("detail_url") val detailUrl: String,
    @SerialName("is_page") val isPage: Boolean,
    @SerialName("content_text") val contentText: String?,
    @SerialName("attachments") val attachmentUrls: List<String>?,
    /** 本地数据源 id（脚本产出时为空，落库后回填，用于区分来源）。 */
    @SerialName("source_id") val sourceId: String? = null,
)
