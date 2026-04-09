package org.openjwc.client.data.repository

import org.openjwc.client.net.models.*
import org.openjwc.client.net.news.*

class NewsRepository(private val settingsRepository: SettingsRepository) {
    /// TODO: 这个地方之后会加缓存逻辑
    private suspend fun getApiService(): NetService {
        val settings = settingsRepository.getSettingsSnapshot()
        return NetClient.getService(
            settings.host,
            settings.port,
            settings.useHttp,
            settings.proxy
        )
    }

    suspend fun getLabels(): NetworkResult<SuccessResponse<FetchLabelsResponseData>> {
        val settings = settingsRepository.getSettingsSnapshot()
        return getApiService().fetchLabels(settings.authKey, settings.uuidString)
    }

    suspend fun getNews(label: String, page: Int, size: Int): NetworkResult<SuccessResponse<FetchNewsResponseData>> {
        val settings = settingsRepository.getSettingsSnapshot()
        return getApiService().fetchNews(
            settings.authKey,
            settings.uuidString,
            label,
            page,
            size
        )
    }

    suspend fun uploadNews(notice: UploadedNotice): NetworkResult<SuccessResponse<Map<String,String>>> {
        val settings = settingsRepository.getSettingsSnapshot()
        return getApiService().uploadNews(
            settings.authKey,
            settings.uuidString,
            notice
        )
    }

    suspend fun getReviewedNews(): NetworkResult<SuccessResponse<ReviewedNoticesData>> {
        val settings = settingsRepository.getSettingsSnapshot()
        return getApiService().fetchReviewedNews(
            settings.authKey,
            settings.uuidString
        )
    }
}