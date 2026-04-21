package org.openjwc.client.ui.main

import org.openjwc.client.net.models.GitHubRelease
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
fun UpdateDialog(
    gitHubRelease: GitHubRelease,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit,
) {
    val screenHeight = LocalWindowInfo.current.containerDpSize.height
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.update_available),
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
                    markdown = gitHubRelease.body,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onUpdate) {
                Text(stringResource(R.string.download_update))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}