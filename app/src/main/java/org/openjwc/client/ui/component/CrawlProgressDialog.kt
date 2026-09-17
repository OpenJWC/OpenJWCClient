package org.openjwc.client.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.openjwc.client.R
import org.openjwc.client.viewmodels.CrawlProgress

/**
 * 抓取数据源的进度对话框：显示当前正在抓的源、总体进度，以及每个源的结果摘要。
 * 抓取过程中不可关闭（点外部/返回无效），结束后可以手动关闭。
 */
@Composable
fun CrawlProgressDialog(
    progress: CrawlProgress,
    onDismiss: () -> Unit,
    onCancel: (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = { if (!progress.running) onDismiss() },
        title = { Text(stringResource(R.string.crawl_progress_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val current = progress.currentSourceName
                Text(
                    text = if (progress.running && current != null) {
                        stringResource(
                            R.string.crawl_progress_current,
                            current,
                            (progress.finished + 1).coerceAtMost(progress.total),
                            progress.total,
                        )
                    } else {
                        stringResource(R.string.crawl_progress_done, progress.results.size)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = {
                        if (progress.total <= 0) 0f
                        else ((progress.finished + progress.currentSourceFraction) / progress.total)
                            .coerceIn(0f, 1f)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (progress.results.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    progress.results.forEach { result ->
                        Text(
                            text = "· ${result.sourceName}：${result.summary}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // 实时日志：脚本每翻一页上报一次，抓取慢时能看清进度
                if (progress.logs.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    val logScroll = rememberScrollState()
                    LaunchedEffect(progress.logs.size) {
                        logScroll.animateScrollTo(logScroll.maxValue)
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(logScroll)
                    ) {
                        progress.logs.forEach { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                if (progress.running && onCancel != null) {
                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.cancel))
                    }
                }
                TextButton(onClick = onDismiss, enabled = !progress.running) {
                    Text(stringResource(R.string.close))
                }
            }
        },
    )
}
