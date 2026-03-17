package org.openjwc.client.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ChatInputBar(
    modifier: Modifier = Modifier,
    onSendMessage: (String) -> Unit,
    onAttachment: () -> Unit,
    isSending: Boolean = true,
) {
    var textState by remember { mutableStateOf("") }
    val isNotEmpty = textState.isNotBlank()

    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onAttachment) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = "Attach",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                BasicTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        if (textState.isEmpty()) {
                            Text(
                                "问些什么",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if(isSending) {
            ContainedLoadingIndicator(
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                indicatorColor = MaterialTheme.colorScheme.primary
            )
        } else
        Surface(
            onClick = {
                if (isNotEmpty) {
                    onSendMessage(textState)
                    textState = ""
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
                    tint = if (isNotEmpty) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview
@Composable
fun TestMessageStyleInputBar() {
    ChatInputBar(
        onSendMessage = {},
        onAttachment = {}
    )
}