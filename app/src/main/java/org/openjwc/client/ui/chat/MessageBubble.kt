package org.openjwc.client.ui.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jeziellago.compose.markdowntext.MarkdownText
import org.openjwc.client.net.chat.ChatMessage

@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.isUser

    val containerColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val bubbleShape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = if (isUser) 20.dp else 4.dp, // 接收方左下角尖
        bottomEnd = if (isUser) 4.dp else 20.dp     // 发送方右下角尖
    )

    val uriCurrent = LocalUriHandler.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = containerColor,
            contentColor = contentColor,
            shape = bubbleShape,
            tonalElevation = if (isUser) 0.dp else 2.dp
        ) {
            if (isUser)
            {Text(
                text = message.text,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .widthIn(max = 280.dp), // 限制最大宽度，防止单行过长
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 24.sp
                )
            )}
            else {
                MarkdownText(
                    markdown = message.text,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .widthIn(max = 280.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = contentColor,
                        lineHeight = 24.sp
                    ),
                    // 自动处理链接跳转
                    onLinkClicked = { url ->
                        uriCurrent.openUri(url)
                    }
                )
            }
        }
    }

    // TODO: 显示发送时间功能
    /*
    Text(`
        text = "10:24 AM",
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp),
        color = MaterialTheme.colorScheme.outline
    )
    */
}