package org.openjwc.client.data.models

import androidx.room.Entity
import androidx.room.Index
import org.openjwc.client.net.models.FetchedNotice
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 资讯离线缓存表。
 * 以数据源（host + port）为分区维度，同一数据库可共存多个服务器的缓存；
 * label 建索引支持按标签查询；notified 字段即通知水位。
 */
@Entity(
    tableName = "news_cache",
    primaryKeys = ["host", "port", "noticeId"],
    indices = [Index(value = ["host", "port", "label"])]
)
data class NewsCacheEntity(
    val host: String,
    val port: Int,
    val noticeId: String,
    val label: String,
    val title: String,
    val date: String,
    val sortTime: Long,
    val detailUrl: String,
    val isPage: Boolean,
    val contentText: String?,
    val attachmentUrls: List<String>?,
    val cachedAt: Long,
    val notified: Boolean = false
)

/** 每个数据源缓存一份标签列表，供离线时回退显示。 */
@Entity(tableName = "news_cache_labels", primaryKeys = ["host", "port"])
data class NewsLabelCacheEntity(
    val host: String,
    val port: Int,
    val labels: List<String>,
    val cachedAt: Long
)

fun NewsCacheEntity.toFetchedNotice() = FetchedNotice(
    id = noticeId,
    label = label,
    title = title,
    date = date,
    detailUrl = detailUrl,
    isPage = isPage,
    contentText = contentText,
    attachmentUrls = attachmentUrls
)

fun FetchedNotice.toNewsCacheEntity(host: String, port: Int, now: Long): NewsCacheEntity =
    NewsCacheEntity(
        host = host,
        port = port,
        noticeId = id,
        label = label,
        title = title,
        date = date,
        sortTime = parseNewsDateSortKey(date),
        detailUrl = detailUrl,
        isPage = isPage,
        contentText = contentText,
        attachmentUrls = attachmentUrls,
        cachedAt = now
    )

private val NEWS_DATE_FORMATTERS = listOf(
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
    DateTimeFormatter.ISO_LOCAL_DATE_TIME,
    DateTimeFormatter.ISO_LOCAL_DATE
)

/** 解析资讯日期字符串为排序键（epoch 毫秒），无法解析时返回 0。 */
fun parseNewsDateSortKey(date: String): Long {
    if (date.isBlank()) return 0L
    for (formatter in NEWS_DATE_FORMATTERS) {
        val time = runCatching {
            LocalDateTime.parse(date, formatter)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.recoverCatching {
            LocalDate.parse(date, formatter)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrNull()
        if (time != null) return time
    }
    return 0L
}
