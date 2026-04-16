package org.openjwc.client.data.repository

import kotlinx.coroutines.flow.first
import org.openjwc.client.data.datastore.AuthDataSource
import org.openjwc.client.data.datastore.SettingsDataSource
import org.openjwc.client.net.models.*
import org.openjwc.client.net.news.*

class NewsRepository(
    private val settingsDataSource: SettingsDataSource,
    private val authDataSource: AuthDataSource
) {
    /// TODO: 这个地方之后会加缓存逻辑
    private suspend fun getApiService(): NetService {
        val settings = settingsDataSource.userSettings.first()
        return NetClient.getService(
            settings.host,
            settings.port,
            settings.useHttp,
            settings.proxy
        )
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
        if (!authSession.isLoggedIn) return NetworkResult.Failure(401,"Not logged in")
        return getApiService().uploadNews(
            authSession.token ?: return NetworkResult.Failure(401, "No token"),
            authSession.uuid,
            notice
        )
    }

    suspend fun getReviewedNews(): NetworkResult<SuccessResponse<ReviewedNoticesData>> {
        val authSession = authDataSource.authSession.first()
        if (!authSession.isLoggedIn) return NetworkResult.Failure(401,"Not logged in")
        return getApiService().fetchReviewedNews(
            authSession.token ?: return NetworkResult.Failure(401, "No token"),
            authSession.uuid,
        )
    }
}