package org.openjwc.client.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openjwc.client.net.models.FetchedNotice

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChatInputBar(
    modifier: Modifier = Modifier,
    textValue: String,
    onTextChange: (String) -> Unit,
    attachments: List<FetchedNotice>,
    onSendMessage: () -> Unit,
    onAddAttachment: () -> Unit,
    onDeleteAttachment: (FetchedNotice) -> Unit,
    isSending: Boolean = false,
) {
    val isNotEmpty = textValue.isNotBlank()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column {
                if (attachments.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, start = 12.dp, end = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp), // 增加行间距
                    ) {
                        attachments.forEach { notice ->
                            AttachmentChip(
                                title = notice.title,
                                onDelete = { onDeleteAttachment(notice) }
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    IconButton(onClick = onAddAttachment) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = "Attach",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    BasicTextField(
                        value = textValue,
                        onValueChange = onTextChange,
                        maxLines = 5,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp)
                            .padding(end = 12.dp)
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.Enter && event.isCtrlPressed) {
                                    if (isNotEmpty && !isSending) {
                                        onSendMessage()
                                    }
                                    true
                                } else false
                            },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (textValue.isEmpty()) {
                                    Text(
                                        "问些什么",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.7f
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isSending) {
            ContainedLoadingIndicator(
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                indicatorColor = MaterialTheme.colorScheme.primary
            )
        } else {
            Surface(
                onClick = {
                    if (isNotEmpty) {
                        onSendMessage()
                    }
                },
                enabled = isNotEmpty,
                shape = CircleShape,
                color = if (isNotEmpty) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Send",
                        tint = if (isNotEmpty) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            0.4f
                        ),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AttachmentChip(
    title: String,
    onDelete: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete",
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onDelete() },
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Preview
@Composable
fun TestMessageStyleInputBar() {
    ChatInputBar(
        onSendMessage = {},
        onAddAttachment = {},
        attachments = listOf(
            FetchedNotice(
                id = "1",
                label = "test",
                title = "1111111111111111111111111111111",
                date = "test",
                detailUrl = "test",
                isPage = false,
                contentText = null,
                attachmentUrls = null
            ),
            FetchedNotice(
                id = "2",
                label = "test",
                title = "222222222222222222222222222222222222",
                date = "test",
                detailUrl = "test",
                isPage = false,
                contentText = null,
                attachmentUrls = null
            ),
            FetchedNotice(
                id = "3",
                label = "test",
                title = "3333333333333333333333333333333",
                date = "test",
                detailUrl = "test",
                isPage = false,
                contentText = null,
                attachmentUrls = null
            )
        ),
        onDeleteAttachment = {},
        isSending = true,
        textValue = "test",
        onTextChange = {}
    )
}