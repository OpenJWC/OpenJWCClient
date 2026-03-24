package org.openjwc.client.ui.policy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import dev.jeziellago.compose.markdowntext.MarkdownText

@Composable
fun PolicyDialog(
    policyText: String,
    onDismiss: () -> Unit,
    onAgree: () -> Unit,
) {
    // 获取屏幕高度，限制 Dialog 的最大高度，防止按钮被挤出屏幕
    val screenHeight = LocalWindowInfo.current.containerDpSize.height
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "用户协议与隐私政策",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = screenHeight * 0.6f)
                    .verticalScroll(scrollState)
            ) {
                MarkdownText(
                    markdown = policyText,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAgree) {
                Text("同意并继续")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("拒绝并退出")
            }
        }
    )
}