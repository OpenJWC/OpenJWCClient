package org.openjwc.client.net.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class GitHubRelease(
    val id: Long,
    @SerialName("tag_name")
    val tagName: String,
    val name: String,
    val prerelease: Boolean,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("published_at")
    val publishedAt: String,
    val body: String,
    val assets: List<ReleaseAsset>,
    @SerialName("html_url")
    val htmlUrl: String
)

@Serializable
data class ReleaseAsset(
    val id: Long,
    val name: String,
    val size: Long,
    @SerialName("download_count")
    val downloadCount: Int,
    @SerialName("content_type")
    val contentType: String,
    @SerialName("browser_download_url")
    val browserDownloadUrl: String
)