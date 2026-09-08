package org.openjwc.client.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import org.openjwc.client.data.dao.NewsDao
import org.openjwc.client.data.datastore.AuthDataSource
import org.openjwc.client.data.datastore.SettingsDataSource
import org.openjwc.client.data.models.NewsCacheEntity
import org.openjwc.client.data.models.NewsLabelCacheEntity
import org.openjwc.client.data.models.NoticeEntity
import org.openjwc.client.data.models.toFetchedNotice
import org.openjwc.client.data.models.toNewsCacheEntity
import org.openjwc.client.net.models.*
import org.openjwc.client.net.news.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

class NewsRepository(
    private val newsDao: NewsDao,
    private val settingsDataSource: SettingsDataSource,
    private val authDataSource: AuthDataSource
) {
    companion object {
        /** 每个数据源分区（host+port+label）的缓存行数上限。 */
        const val CACHE_LIMIT = 200
    }

    private suspend fun getApiService(): NetService {
        val settings = settingsDataSource.userSettings.first()
        return NetClient.getService(
            settings.host,
            settings.port,
            settings.useHttp,
            settings.proxy
        )
    }

    /** 当前数据源分区（host, port）。所有缓存/收藏读写都以调用时刻的设置为分区依据。 */
    private suspend fun getSource(): Pair<String, Int> {
        val settings = settingsDataSource.userSettings.first()
        return settings.host to settings.port
    }

    suspend fun getLabels(): NetworkResult<SuccessResponse<FetchLabelsResponseData>> {
        val authSession = authDataSource.authSession.first()
        return getApiService().fetchLabels(
            authSession.token ?: "",
            authSession.uuid
        )
    }

    suspend fun getNews(
        label: String,
        page: Int,
        size: Int
    ): NetworkResult<SuccessResponse<FetchNewsResponseData>> {
        val authSession = authDataSource.authSession.first()
        return getApiService().fetchNews(
            authSession.token ?: "",
            authSession.uuid,
            label,
            page,
            size
        )
    }

    suspend fun uploadNews(notice: UploadedNotice): NetworkResult<SuccessResponse<Map<String, String>>> {
        val authSession = authDataSource.authSession.first()
        return getApiService().uploadNews(
            authSession.token ?: "",
            authSession.uuid,
            notice
        )
    }

    suspend fun getReviewedNews(): NetworkResult<SuccessResponse<ReviewedNoticesData>> {
        val authSession = authDataSource.authSession.first()
        return getApiService().fetchReviewedNews(
            authSession.token ?: "",
            authSession.uuid,
        )
    }

    /* ================= 资讯缓存 ================= */

    suspend fun getCachedNews(label: String, limit: Int = CACHE_LIMIT): List<FetchedNotice> {
        val (host, port) = getSource()
        return newsDao.getNewsCache(host, port, label, limit).map { it.toFetchedNotice() }
    }

    suspend fun countCachedNews(label: String): Int {
        val (host, port) = getSource()
        return newsDao.countNewsCache(host, port, label)
    }

    suspend fun getCachedLabels(): List<String>? {
        val (host, port) = getSource()
        return newsDao.getLabelCache(host, port)?.labels
    }

    fun observeNewsCacheCount(): Flow<Int> = newsDao.observeNewsCacheCount()

    /**
     * 三步法写入缓存，保留已有行的 notified 状态（@Upsert 会整行覆盖）：
     * 1) 读取分区已有 id 与已通知 id 集合
     * 2) upsert（新行 notified=false）
     * 3) 回写原有 notified 状态
     * @return 本次真正新插入的资讯（Worker 依据此列表发通知）
     */
    suspend fun upsertNewsCachePreservingNotified(items: List<FetchedNotice>): List<FetchedNotice> {
        if (items.isEmpty()) return emptyList()
        val (host, port) = getSource()
        val existingIds = newsDao.getNewsCacheIds(host, port, items.first().label).toSet()
        val notifiedIds = newsDao.getNotifiedNewsIds(host, port).toSet()
        val now = System.currentTimeMillis()

        newsDao.upsertNewsCache(items.map { it.toNewsCacheEntity(host, port, now) })

        // 回写被 upsert 覆盖行的已通知状态
        val stillNotified = items.map { it.id }.filter { it in notifiedIds }
        if (stillNotified.isNotEmpty()) {
            newsDao.markNewsNotified(host, port, stillNotified)
        }

        return items.filter { it.id !in existingIds }
    }

    /** UI 刷新路径：用户正在浏览，新行立即视为已通知；随后裁剪分区容量。 */
    suspend fun refreshNewsCacheFromUi(label: String, items: List<FetchedNotice>) {
        if (items.isEmpty()) return
        val newlyInserted = upsertNewsCachePreservingNotified(items)
        if (newlyInserted.isNotEmpty()) {
            markNewsNotified(newlyInserted.map { it.id })
        }
        pruneNewsCache(label)
    }

    suspend fun pruneNewsCache(label: String, keep: Int = CACHE_LIMIT) {
        val (host, port) = getSource()
        newsDao.pruneNewsCache(host, port, label, keep)
    }

    suspend fun saveLabelsCache(labels: List<String>) {
        val (host, port) = getSource()
        newsDao.upsertLabelCache(
            NewsLabelCacheEntity(host, port, labels, System.currentTimeMillis())
        )
    }

    suspend fun clearNewsCache() {
        newsDao.clearNewsCache()
        newsDao.clearNewsLabelCache()
    }

    suspend fun markNewsNotified(noticeIds: List<String>) {
        if (noticeIds.isEmpty()) return
        val (host, port) = getSource()
        newsDao.markNewsNotified(host, port, noticeIds)
    }

    /* ================= 收藏（按数据源分区） ================= */

    @OptIn(ExperimentalCoroutinesApi::class)
    fun allFavorites(): Flow<List<NoticeEntity>> =
        settingsDataSource.userSettings
            .map { it.host to it.port }
            .distinctUntilChanged()
            .flatMapLatest { (host, port) -> newsDao.getAllFavorites(host, port) }

    suspend fun deleteFavoriteNews(noticeId: String) {
        val (host, port) = getSource()
        newsDao.deleteFavoriteById(host, port, noticeId)
    }

    suspend fun deleteAllFavorites() {
        val (host, port) = getSource()
        newsDao.deleteAllFavorites(host, port)
    }

    suspend fun insertFavoriteNews(notice: FetchedNotice) {
        val (host, port) = getSource()
        newsDao.insertFavorite(notice.toNoticeEntity(host, port))
    }

    suspend fun isFavorited(noticeId: String): Boolean {
        val (host, port) = getSource()
        return newsDao.isFavorited(host, port, noticeId)
    }

    /** 迁移遗留收藏（host='' 分区）到当前数据源，幂等。 */
    suspend fun adoptLegacyFavorites() {
        val (host, port) = getSource()
        newsDao.updateLegacyFavoritesOwner(host, port)
    }
}
