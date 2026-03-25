package org.openjwc.client.net.models

import kotlinx.serialization.Serializable

@Serializable
data class Hitokoto(
    val text: String,
    val author: String? = null,
)