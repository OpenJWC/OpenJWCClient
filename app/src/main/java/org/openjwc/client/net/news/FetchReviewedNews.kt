package org.openjwc.client.net.news

import org.openjwc.client.net.models.NetService
import org.openjwc.client.net.models.NetworkResult
import org.openjwc.client.net.models.ReviewedNoticesData
import org.openjwc.client.net.models.SuccessResponse
import org.openjwc.client.net.models.fetch

suspend fun NetService.fetchReviewedNews(
    auth: String,
    deviceId: String,
): NetworkResult<SuccessResponse<ReviewedNoticesData>> = fetch { getReviewedNotices("Bearer $auth", deviceId) }