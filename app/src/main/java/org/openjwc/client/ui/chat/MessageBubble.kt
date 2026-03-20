package org.openjwc.client.ui.chat

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jeziellago.compose.markdowntext.MarkdownText
import org.openjwc.client.data.models.ChatMessage
import org.openjwc.client.data.models.Role

@OptIn(ExperimentalMaterial3ExpressiveApi::class) // 启用 Expressive API
@Composable
fun MessageBubble(
    message: ChatMessage,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    maxWidth: Dp,
    onCopy: (ChatMessage) -> Unit = {},
    onShare: (ChatMessage) -> Unit = {},
    onDelete: (ChatMessage) -> Unit = {},
) {
    var showMenu by remember { mutableStateOf(false) }
    val uriCurrent = LocalUriHandler.current
    val isUser = message.role == Role.USER

    val containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

//    val screenWidth = LocalWindowInfo.current.containerSize
//    val maxBubbleWidth = screenWidth.width * 0.7f

    val bubbleShape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = if (isUser) 20.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 20.dp
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // 使用 Row 容器来排列加载器和气泡
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. 如果是用户发送中，在气泡左侧显示加载动画
            if (isUser && isLoading) {
                LoadingIndicator(
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.Bottom)
                )
            }

            Box(
                modifier = Modifier.combinedClickable(
                    onClick = { /* 单击逻辑 */ },
                    onLongClick = { showMenu = true }
                )
            ) {
                Surface(
                    color = containerColor,
                    contentColor = contentColor,
                    shape = bubbleShape,
                    tonalElevation = if (isUser) 0.dp else 2.dp,
                ) {
                    if (isUser) {
                        Text(
                            text = message.text,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .widthIn(max = maxWidth),
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp)
                        )
                    } else {
                        MarkdownText(
                            markdown = message.text,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .widthIn(max = maxWidth),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = contentColor,
                                lineHeight = 24.sp
                            ),
                            isTextSelectable = true,
                            onLinkClicked = { url -> uriCurrent.openUri(url) }
                        )
                    }
                }

                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("复制") },
                        onClick = {
                            onCopy(message)
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("转发") },
                        onClick = {
                            onShare(message)
                            showMenu = false },
                        leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "删除",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            onDelete(message)
                            showMenu = false
                        }
                    )
                }
            }

            // 2. 如果是 AI 正在生成，在气泡右侧显示加载动画
            if (!isUser && isLoading) {
                LoadingIndicator(
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.Bottom)
                )
            }
        }
    }
}


@Preview
@Composable
fun TestMessageBubbleFromUser() {
    val mockChatMessage = ChatMessage(
        messageId = 11451,
        text = "Hello World",
        role = Role.USER,
        ownerSessionId = 0,
        timestamp = System.currentTimeMillis()
    )
    MessageBubble(
        mockChatMessage,
        true,
        maxWidth = 300.dp
    )
}

@Preview
@Composable
fun TestMessageBubbleFromAI() {
    val mockChatMessage = ChatMessage(
        messageId = 19198,
        text =  "hello",
        role = Role.ASSISTANT,
        ownerSessionId = 0,
        timestamp = System.currentTimeMillis()
    )
    MessageBubble(mockChatMessage, true, maxWidth = 300.dp)
}