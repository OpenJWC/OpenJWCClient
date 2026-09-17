package org.openjwc.client.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.openjwc.client.data.dao.LabelCount
import org.openjwc.client.data.dao.NoticeDao
import org.openjwc.client.data.dao.SourceDao
import org.openjwc.client.data.models.NoticeEntity
import org.openjwc.client.data.models.SourceEntity
import org.openjwc.client.data.models.toFetchedNotice
import org.openjwc.client.net.models.FetchedNotice

/** 语料覆盖范围，供 Agent 判断能查多远。 */
data class CorpusCatalog(
    val total: Int,
    val firstDay: String?,
    val lastDay: String?,
)

/**
 * 资讯仓库：本地语料（`notices`）的读写 + 数据源订阅 + 收藏，
 * 同时向 Agent 暴露检索接口（[searchNotices] / [findNotice] / [corpusCatalog]）。
 */
class NewsRepository(
    private val noticeDao: NoticeDao,
    private val sourceDao: SourceDao,
) : NoticeCorpus {

    /* ================= 资讯流 ================= */

    fun observeSubscribedSources(): Flow<List<SourceEntity>> = sourceDao.observeSubscribed()

    /** 已订阅数据源快照（Agent 的 list_sources 工具用）。 */
    override suspend fun subscribedSources(): List<SourceEntity> = sourceDao.getSubscribed()

    fun observeNoticeCount(): Flow<Int> = noticeDao.observeCount()

    /**
     * 栏目列表：以订阅源声明的 `@labels` 顺序为准，再补上语料里出现的额外栏目。
     * [sourceId] 为 null 表示全部数据源。
     */
    suspend fun getLocalLabels(sourceId: String? = null): List<String> {
        val sources = sourceDao.getSubscribed()
        val picked = if (sourceId == null) sources else sources.filter { it.id == sourceId }
        val declared = picked.flatMap { it.labels }.distinct()
        val extra = noticeDao.distinctLabelsBySource(sourceId).filterNot { it in declared }
        return declared + extra
    }

    suspend fun getLocalNews(
        label: String,
        sourceId: String? = null,
        limit: Int,
        offset: Int = 0,
    ): List<FetchedNotice> =
        noticeDao.listByLabel(label, sourceId, limit, offset).map { it.toFetchedNotice() }

    suspend fun clearNotices() {
        noticeDao.clearAll()
    }

    /* ================= 收藏 ================= */

    fun observeFavorites(): Flow<List<FetchedNotice>> =
        noticeDao.observeFavorites().map { list -> list.map { it.toFetchedNotice() } }

    suspend fun setFavorite(noticeId: String, favorite: Boolean) {
        noticeDao.setFavorite(noticeId, favorite)
    }

    suspend fun clearFavorites() {
        noticeDao.clearFavorites()
    }

    suspend fun isFavorite(noticeId: String): Boolean = noticeDao.isFavorite(noticeId)

    /* ================= Agent 检索 ================= */

    /**
     * 语料检索。空 [query] 表示只按条件列举；[relevance] 为 true 时标题命中优先。
     * 日期为 `yyyy-MM-dd` 闭区间（空串表示不限）。
     */
    override suspend fun searchNotices(
        query: String,
        label: String,
        sourceId: String?,
        fromDay: String,
        toDay: String,
        favoriteOnly: Boolean,
        relevance: Boolean,
        limit: Int,
        offset: Int,
    ): List<NoticeEntity> = noticeDao.searchNotices(
        query = query,
        label = label,
        sourceId = sourceId,
        fromDay = fromDay,
        toDay = toDay,
        favoriteOnly = if (favoriteOnly) 1 else 0,
        relevance = if (relevance) 1 else 0,
        limit = limit,
        offset = offset,
    )

    override suspend fun countNotices(
        query: String,
        label: String,
        sourceId: String?,
        fromDay: String,
        toDay: String,
        favoriteOnly: Boolean,
    ): Int = noticeDao.countNotices(
        query = query,
        label = label,
        sourceId = sourceId,
        fromDay = fromDay,
        toDay = toDay,
        favoriteOnly = if (favoriteOnly) 1 else 0,
    )

    override suspend fun findNotice(id: String): NoticeEntity? = noticeDao.findById(id)

    /** 栏目及条数（Agent 的 list_labels 工具用）。 */
    override suspend fun corpusLabels(): List<LabelCount> = noticeDao.labelCounts()

    override suspend fun corpusCatalog(): CorpusCatalog = CorpusCatalog(
        total = noticeDao.totalCount(),
        firstDay = noticeDao.minDay(),
        lastDay = noticeDao.maxDay(),
    )
}
