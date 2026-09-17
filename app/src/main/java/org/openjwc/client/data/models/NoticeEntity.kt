package org.openjwc.client.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.openjwc.client.net.models.FetchedNotice
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 正文提取格式版本。改动正文解析方式时 +1，旧条目会自动被重新抓取补正文。
 * 1 = Markdown（HtmlToMarkdown，对齐 JwcCrawler 的 htmd 用法）
 */
const val NOTICE_CONTENT_VERSION = 1

/**
 * 本地资讯语料（唯一来源）。
 *
 * 抓取写入、资讯流读取、收藏、以及 Agent 检索都基于这张表；
 * 与旧版不同：不再按数据源分区，也不再裁剪，作为可检索的知识库长期保留。
 */
@Entity(
    tableName = "notices",
    indices = [
        Index(value = ["publishedAt"]),
        Index(value = ["publishedDay"]),
        Index(value = ["label", "publishedAt"]),
        Index(value = ["sourceId"]),
    ]
)
data class NoticeEntity(
    @PrimaryKey val id: String,
    /** 本地数据源 id；历史遗留数据为 null。 */
    val sourceId: String?,
    val label: String,
    val title: String,
    /** 排序键（epoch 毫秒），解析失败为 0。 */
    val publishedAt: Long,
    /** `yyyy-MM-dd`，用于按日/按月查询与展示；解析失败为空串。 */
    val publishedDay: String,
    val detailUrl: String,
    val isPage: Boolean,
    val content: String?,
    /** 写入该条正文时的 [NOTICE_CONTENT_VERSION]；0 = 旧格式（纯文本），需要重抓。 */
    val contentVersion: Int = 0,
    val attachments: List<String>?,
    val fetchedAt: Long,
    /** 通知水位：已通知过为 true。 */
    val notified: Boolean = false,
    val favorite: Boolean = false,
)

fun NoticeEntity.toFetchedNotice() = FetchedNotice(
    id = id,
    label = label,
    title = title,
    date = publishedDay,
    detailUrl = detailUrl,
    isPage = isPage,
    contentText = content,
    attachmentUrls = attachments,
    sourceId = sourceId,
)

fun FetchedNotice.toNoticeEntity(
    sourceId: String?,
    now: Long,
    contentVersion: Int = NOTICE_CONTENT_VERSION,
): NoticeEntity {
    val sortKey = parseNewsDateSortKey(date)
    return NoticeEntity(
        id = id,
        sourceId = sourceId,
        label = label,
        title = title,
        publishedAt = sortKey,
        publishedDay = date.take(10),
        detailUrl = detailUrl,
        isPage = isPage,
        content = contentText,
        contentVersion = contentVersion,
        attachments = attachmentUrls,
        fetchedAt = now,
    )
}

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
