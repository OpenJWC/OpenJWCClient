package org.openjwc.client.net.news

import org.openjwc.client.net.models.UploadedNotice
import org.openjwc.client.net.models.NetService
import org.openjwc.client.net.models.NetworkResult
import org.openjwc.client.net.models.SuccessResponse
import org.openjwc.client.net.models.fetch

suspend fun NetService.uploadNews(
    auth: String,
    deviceId: String,
    uploadedNotice: UploadedNotice
): NetworkResult<SuccessResponse<Map<String,String>>> = fetch { postNotice("Bearer $auth", deviceId, uploadedNotice) }