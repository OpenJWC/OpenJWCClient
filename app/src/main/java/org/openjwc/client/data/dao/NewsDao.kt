package org.openjwc.client.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.openjwc.client.data.models.NewsCacheEntity
import org.openjwc.client.data.models.NewsLabelCacheEntity
import org.openjwc.client.data.models.NoticeEntity

@Dao
interface NewsDao {
    /* ================= 收藏（按数据源分区） ================= */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(notice: NoticeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorites(notices: List<NoticeEntity>)

    @Query("DELETE FROM favorite_notices WHERE host = :host AND port = :port AND id = :noticeId")
    suspend fun deleteFavoriteById(host: String, port: Int, noticeId: String)

    @Query("DELETE FROM favorite_notices WHERE host = :host AND port = :port")
    suspend fun deleteAllFavorites(host: String, port: Int)

    @Query("SELECT * FROM favorite_notices WHERE host = :host AND port = :port ORDER BY date DESC")
    fun getAllFavorites(host: String, port: Int): Flow<List<NoticeEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_notices WHERE host = :host AND port = :port AND id = :noticeId)")
    suspend fun isFavorited(host: String, port: Int, noticeId: String): Boolean

    /** 迁移遗留数据：把 host 为空的旧收藏行归属到指定数据源（幂等）。 */
    @Query("UPDATE favorite_notices SET host = :host, port = :port WHERE host = ''")
    suspend fun updateLegacyFavoritesOwner(host: String, port: Int)

    /* ================= 资讯缓存 ================= */

    @Upsert
    suspend fun upsertNewsCache(items: List<NewsCacheEntity>)

    @Query(
        "SELECT * FROM news_cache WHERE host = :host AND port = :port AND label = :label " +
            "ORDER BY sortTime DESC LIMIT :limit"
    )
    suspend fun getNewsCache(host: String, port: Int, label: String, limit: Int): List<NewsCacheEntity>

    @Query("SELECT COUNT(*) FROM news_cache WHERE host = :host AND port = :port AND label = :label")
    suspend fun countNewsCache(host: String, port: Int, label: String): Int

    @Query("SELECT noticeId FROM news_cache WHERE host = :host AND port = :port AND label = :label")
    suspend fun getNewsCacheIds(host: String, port: Int, label: String): List<String>

    @Query("SELECT noticeId FROM news_cache WHERE host = :host AND port = :port AND notified = 1")
    suspend fun getNotifiedNewsIds(host: String, port: Int): List<String>

    @Query("UPDATE news_cache SET notified = 1 WHERE host = :host AND port = :port AND noticeId IN (:noticeIds)")
    suspend fun markNewsNotified(host: String, port: Int, noticeIds: List<String>)

    /** 裁剪：仅保留该分区 sortTime 最新的 :keep 行。 */
    @Query(
        "DELETE FROM news_cache WHERE host = :host AND port = :port AND label = :label AND noticeId NOT IN " +
            "(SELECT noticeId FROM news_cache WHERE host = :host AND port = :port AND label = :label " +
            "ORDER BY sortTime DESC LIMIT :keep)"
    )
    suspend fun pruneNewsCache(host: String, port: Int, label: String, keep: Int)

    @Query("SELECT COUNT(*) FROM news_cache")
    fun observeNewsCacheCount(): Flow<Int>

    @Query("DELETE FROM news_cache")
    suspend fun clearNewsCache()

    /* ================= 标签缓存 ================= */

    @Upsert
    suspend fun upsertLabelCache(entity: NewsLabelCacheEntity)

    @Query("SELECT * FROM news_cache_labels WHERE host = :host AND port = :port LIMIT 1")
    suspend fun getLabelCache(host: String, port: Int): NewsLabelCacheEntity?

    @Query("DELETE FROM news_cache_labels")
    suspend fun clearNewsLabelCache()
}
