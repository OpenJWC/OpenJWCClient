package org.openjwc.client.net.chat

data class ChatMessage (
    val id: Long,
    val text: String,
    val isUser: Boolean
    // TODO: 加上上传附件功能
)