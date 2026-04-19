package org.openjwc.client.ui.me.settings.log

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openjwc.client.log.Logger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    logs: List<Logger.LogEntry>,
    onBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text("日志") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            items(
                items = logs,
                key = { it.id }
            ) { log ->
                LogItem(log)
            }
        }
    }
}

@Preview
@Composable
fun TestLogItem() {
    LogItem(
        log = Logger.LogEntry(
            id = 0,
            timestamp = System.currentTimeMillis(),
            level = Logger.Level.DEBUG,
            tag = "TestTag",
            message = "This is a test log message."
        )
    )
}

@Composable
fun LogItem(
    log: Logger.LogEntry,
    modifier: Modifier = Modifier
) {
    val levelColor = getLevelColor(log.level)
    val timeString = remember(log.timestamp) {
        java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
            .format(log.timestamp)
    }

    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.outlinedCardColors(
            containerColor = levelColor.copy(alpha = 0.05f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = levelColor.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(end = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 8.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = log.tag,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = " | ${log.level.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = levelColor.copy(alpha = 0.8f)
                        )
                    }

                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                SelectionContainer {
                    Text(
                        text = log.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace,
                        softWrap = true
                    )
                }
            }
        }
    }
}


@Composable
fun getLevelColor(level: Logger.Level): Color {
    return when (level) {
        Logger.Level.NONE -> MaterialTheme.colorScheme.primaryFixedDim
        Logger.Level.ERROR -> MaterialTheme.colorScheme.error
        Logger.Level.WARNING -> MaterialTheme.colorScheme.tertiary
        Logger.Level.INFO -> MaterialTheme.colorScheme.primary
        Logger.Level.DEBUG -> MaterialTheme.colorScheme.outline
        Logger.Level.VERBOSE -> MaterialTheme.colorScheme.primaryFixedDim
    }
}