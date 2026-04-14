package org.openjwc.client.net.models

import kotlinx.serialization.Serializable

@Serializable
data class Hitokoto(
    val text: String = "所谓觉悟，就是在漆黑的荒野中，开辟出一条理所应当前进的光明大道。",
    val author: String? = "乔鲁诺·乔巴纳",
)