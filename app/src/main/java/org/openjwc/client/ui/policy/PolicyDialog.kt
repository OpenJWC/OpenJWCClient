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
import androidx.compose.ui.res.stringResource
import dev.jeziellago.compose.markdowntext.MarkdownText
import org.openjwc.client.R

@Composable
fun PolicyDialog(
    policyText: String,
    onDismiss: () -> Unit,
    onAgree: () -> Unit,
) {
    val screenHeight = LocalWindowInfo.current.containerDpSize.height
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.user_agreement_and_privacy_policy),
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
                Text(stringResource(R.string.agree_and_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.decline_and_exit))
            }
        }
    )
}