package org.openjwc.client.net.news

import org.openjwc.client.log.Logger
import org.openjwc.client.net.models.FetchLabelsResponseData
import org.openjwc.client.net.models.FetchNewsResponseData
import org.openjwc.client.net.models.NetService
import org.openjwc.client.net.models.NetworkResult
import org.openjwc.client.net.models.SuccessResponse
import org.openjwc.client.net.models.fetch


suspend fun NetService.fetchNews(
    auth: String,
    deviceId: String,
    label: String,
    page: Int,
    size: Int
): NetworkResult<SuccessResponse<FetchNewsResponseData>> = fetch(level = Logger.Level.DEBUG) { getNotices("Bearer $auth", deviceId, label, page, size) }

suspend fun NetService.fetchLabels(
    auth: String,
    deviceId: String
): NetworkResult<SuccessResponse<FetchLabelsResponseData>> = fetch { getLabels("Bearer $auth", deviceId) }