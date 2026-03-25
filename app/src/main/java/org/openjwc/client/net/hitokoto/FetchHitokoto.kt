package org.openjwc.client.net.hitokoto

import org.openjwc.client.net.models.Hitokoto
import org.openjwc.client.net.models.NetService
import org.openjwc.client.net.models.NetworkResult
import org.openjwc.client.net.models.SuccessResponse
import org.openjwc.client.net.models.fetch

suspend fun NetService.fetchHitokoto(
    auth: String,
    deviceId: String
): NetworkResult<SuccessResponse<Hitokoto>> = fetch { getHitokoto("Bearer $auth", deviceId) }