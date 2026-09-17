package org.openjwc.client.data.repository

import org.openjwc.client.data.dao.LabelCount
import org.openjwc.client.data.models.NoticeEntity
import org.openjwc.client.data.models.SourceEntity

/**
 * Agent 只依赖的只读语料能力。
 * [NewsRepository] 实现它，测试可以给出内存实现。
 */
interface NoticeCorpus {
    suspend fun searchNotices(
        query: String = "",
        label: String = "",
        sourceId: String? = null,
        fromDay: String = "",
        toDay: String = "",
        /** 只返回已收藏的条目。 */
        favoriteOnly: Boolean = false,
        relevance: Boolean = false,
        limit: Int = 20,
        offset: Int = 0,
    ): List<NoticeEntity>

    suspend fun countNotices(
        query: String = "",
        label: String = "",
        sourceId: String? = null,
        fromDay: String = "",
        toDay: String = "",
        favoriteOnly: Boolean = false,
    ): Int

    suspend fun findNotice(id: String): NoticeEntity?

    suspend fun corpusLabels(): List<LabelCount>

    suspend fun subscribedSources(): List<SourceEntity>

    suspend fun corpusCatalog(): CorpusCatalog
}
