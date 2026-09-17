package org.openjwc.client.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.openjwc.client.data.models.NoticeEntity

/** 栏目及其条数。 */
data class LabelCount(val label: String, val labelCount: Int)

/** 数据源及其本地语料条数。 */
data class SourceCount(val sourceId: String?, val noticeCount: Int)

/**
 * 本地资讯语料 DAO：资讯流、收藏、通知水位、以及 Agent 检索共用。
 */
@Dao
interface NoticeDao {

    /* ================= 资讯流 ================= */

    @Query(
        "SELECT * FROM notices WHERE label = :label AND (:sourceId IS NULL OR sourceId = :sourceId) " +
            "ORDER BY publishedAt DESC, id DESC LIMIT :limit OFFSET :offset"
    )
    suspend fun listByLabel(label: String, sourceId: String?, limit: Int, offset: Int): List<NoticeEntity>

    @Query("SELECT COUNT(*) FROM notices")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notices")
    suspend fun totalCount(): Int

    @Query("DELETE FROM notices")
    suspend fun clearAll()

    /* ================= 写入 ================= */

    @Upsert
    suspend fun upsertAll(items: List<NoticeEntity>)

    @Query("SELECT id FROM notices WHERE id IN (:ids) AND favorite = 1")
    suspend fun selectFavoriteIds(ids: List<String>): List<String>

    @Query("SELECT id FROM notices WHERE id IN (:ids) AND notified = 1")
    suspend fun selectNotifiedIds(ids: List<String>): List<String>

    @Query("UPDATE notices SET favorite = 1 WHERE id IN (:ids)")
    suspend fun markFavorites(ids: List<String>)

    @Query("UPDATE notices SET notified = 1 WHERE id IN (:ids)")
    suspend fun markNotified(ids: List<String>)

    /* ================= 收藏 ================= */

    @Query("SELECT * FROM notices WHERE favorite = 1 ORDER BY publishedAt DESC, id DESC")
    fun observeFavorites(): Flow<List<NoticeEntity>>

    @Query("UPDATE notices SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("UPDATE notices SET favorite = 0")
    suspend fun clearFavorites()

    @Query("SELECT EXISTS(SELECT 1 FROM notices WHERE id = :id AND favorite = 1)")
    suspend fun isFavorite(id: String): Boolean

    /* ================= 通知水位 ================= */

    @Query("SELECT id FROM notices WHERE sourceId = :sourceId")
    suspend fun idsBySource(sourceId: String): List<String>

    @Query("SELECT id FROM notices WHERE sourceId = :sourceId AND notified = 1")
    suspend fun notifiedIdsBySource(sourceId: String): List<String>

    @Query("SELECT COUNT(*) FROM notices WHERE sourceId = :sourceId")
    suspend fun countBySource(sourceId: String): Int

    /** 各数据源的本地语料条数（数据源设置页展示）。 */
    @Query(
        "SELECT sourceId, COUNT(*) AS noticeCount FROM notices " +
            "WHERE sourceId IS NOT NULL GROUP BY sourceId"
    )
    fun observeCountsBySource(): Flow<List<SourceCount>>

    /* ================= Agent 检索 ================= */

    @Query(
        """
        SELECT * FROM notices
        WHERE (:query = '' OR title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
          AND (:label = '' OR label = :label)
          AND (:sourceId IS NULL OR sourceId = :sourceId)
          AND (:fromDay = '' OR publishedDay >= :fromDay)
          AND (:toDay = '' OR publishedDay <= :toDay)
          AND (:favoriteOnly = 0 OR favorite = 1)
        ORDER BY
          CASE WHEN :relevance = 1 AND :query <> '' AND title LIKE '%' || :query || '%' THEN 0 ELSE 1 END,
          publishedAt DESC, id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun searchNotices(
        query: String,
        label: String,
        sourceId: String?,
        fromDay: String,
        toDay: String,
        favoriteOnly: Int,
        relevance: Int,
        limit: Int,
        offset: Int,
    ): List<NoticeEntity>

    @Query(
        """
        SELECT COUNT(*) FROM notices
        WHERE (:query = '' OR title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
          AND (:label = '' OR label = :label)
          AND (:sourceId IS NULL OR sourceId = :sourceId)
          AND (:fromDay = '' OR publishedDay >= :fromDay)
          AND (:toDay = '' OR publishedDay <= :toDay)
          AND (:favoriteOnly = 0 OR favorite = 1)
        """
    )
    suspend fun countNotices(
        query: String,
        label: String,
        sourceId: String?,
        fromDay: String,
        toDay: String,
        favoriteOnly: Int,
    ): Int

    /**
     * 已有正文（或本来就不需要正文）的条目 id。
     * 正文为空的条目会被脚本重新尝试，便于在校内网络下补全。
     */
    @Query(
        "SELECT id FROM notices WHERE sourceId = :sourceId " +
            "AND ((content IS NOT NULL AND content <> '' AND contentVersion >= :minVersion) OR isPage = 0)"
    )
    suspend fun idsWithContentBySource(sourceId: String, minVersion: Int): List<String>

    /** 某日发布的资讯 id（日报用）。 */
    @Query("SELECT id FROM notices WHERE publishedDay = :day ORDER BY publishedAt, id")
    suspend fun idsByDay(day: String): List<String>

    @Query("SELECT * FROM notices WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): NoticeEntity?

    @Query("SELECT DISTINCT label FROM notices WHERE label <> '' ORDER BY label")
    suspend fun distinctLabels(): List<String>

    /** 指定数据源（或全部）出现过的栏目。 */
    @Query(
        "SELECT DISTINCT label FROM notices WHERE label <> '' " +
            "AND (:sourceId IS NULL OR sourceId = :sourceId) ORDER BY label"
    )
    suspend fun distinctLabelsBySource(sourceId: String?): List<String>

    @Query("SELECT label, COUNT(*) AS labelCount FROM notices WHERE label <> '' GROUP BY label ORDER BY label")
    suspend fun labelCounts(): List<LabelCount>

    @Query("SELECT MIN(publishedDay) FROM notices WHERE publishedDay <> ''")
    suspend fun minDay(): String?

    @Query("SELECT MAX(publishedDay) FROM notices WHERE publishedDay <> ''")
    suspend fun maxDay(): String?
}
