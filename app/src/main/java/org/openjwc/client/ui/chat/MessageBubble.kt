package org.openjwc.client.ui.chat

import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.launch
import org.openjwc.client.net.chat.ChatMessage

@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    // 用来弹出菜单
    var showMenu by remember { mutableStateOf(false) }
    // 用来复制
    val clipboardManager = LocalClipboard.current
    // 用来打开链接
    val uriCurrent = LocalUriHandler.current
    // 启动协程
    val scope = rememberCoroutineScope()
    // 弹 Toast
    val context = LocalContext.current
    // 区分气泡位置
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


    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier.combinedClickable(
            onClick = { /* 单击逻辑 */ },
            onLongClick = { showMenu = true }
        )) {
            Surface(
                color = containerColor,
                contentColor = contentColor,
                shape = bubbleShape,
                tonalElevation = if (isUser) 0.dp else 2.dp,
            )
            {
                if (isUser) {
                    Text(
                        text = message.text,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .widthIn(max = 280.dp), // 限制最大宽度，防止单行过长
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 24.sp
                        )
                    )
                } else {
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

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("复制") },
                    onClick = {
                        Toast.makeText(context, "复制成功", Toast.LENGTH_SHORT).show()
                        scope.launch {
                            clipboardManager.setClipEntry(
                                ClipEntry(
                                    ClipData.newPlainText(
                                        message.text,
                                        message.text
                                    )
                                )
                            )
                        }

                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("转发") },
                    onClick = { /* TODO: 转发逻辑 */ showMenu = false },
                    leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) }
                )
            }
        }
    }

    // TODO: 显示发送时间功能
    /*
    Text(
        text = "10:24 AM",
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp),
        color = MaterialTheme.colorScheme.outline
    )
    */
}