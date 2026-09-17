package org.openjwc.client.ui.dailyreport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Article
import androidx.compose.material.icons.twotone.ErrorOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.openjwc.client.R
import org.openjwc.client.ui.component.MarkdownContent
import org.openjwc.client.viewmodels.DailyReportViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val chipFormatter = DateTimeFormatter.ofPattern("MM-dd")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DailyReportScreen(
    viewModel: DailyReportViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(modifier = modifier.fillMaxSize()) {
        if (state.dates.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.dates, key = { it }) { date ->
                    val isLatest = date == state.dates.firstOrNull()
                    FilterChip(
                        selected = date == state.selectedDate,
                        onClick = { viewModel.selectDate(date) },
                        label = {
                            Text(
                                text = if (isLatest) {
                                    stringResource(R.string.daily_report_latest) + " · " + chipLabel(date)
                                } else {
                                    chipLabel(date)
                                }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { viewModel.pullRefresh() },
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                state.generating || (state.loading && state.content == null) -> {
                    ScrollableCenter {
                        LoadingIndicator(modifier = Modifier.size(64.dp))
                        if (state.generating) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.daily_report_generating),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                state.content == null && state.failedDay != null -> {
                    ScrollableCenter {
                        DailyReportMessage(
                            icon = Icons.TwoTone.ErrorOutline,
                            title = stringResource(R.string.daily_report_generate_failed),
                            description = state.error,
                            actionText = stringResource(R.string.retry),
                            onAction = { viewModel.generate(state.failedDay) }
                        )
                    }
                }

                state.error != null && state.content == null -> {
                    ScrollableCenter {
                        DailyReportMessage(
                            icon = Icons.TwoTone.ErrorOutline,
                            title = stringResource(R.string.daily_report_load_failed),
                            description = state.error,
                            actionText = stringResource(R.string.retry),
                            onAction = { viewModel.refresh() }
                        )
                    }
                }

                state.content == null -> {
                    ScrollableCenter {
                        DailyReportMessage(
                            icon = Icons.TwoTone.Article,
                            title = stringResource(R.string.daily_report_none),
                            description = stringResource(R.string.daily_report_none_desc),
                            actionText = stringResource(R.string.daily_report_generate),
                            onAction = { viewModel.generate() }
                        )
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(Modifier.height(8.dp))
                        MarkdownContent(state.content.orEmpty())
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

/** 可滚动的居中容器：让空态/错误态也能触发下拉刷新。 */
@Composable
private fun ScrollableCenter(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(96.dp))
        content()
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun DailyReportMessage(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String?,
    actionText: String,
    onAction: () -> Unit,
) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!description.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = onAction) { Text(actionText) }
        }
    }
}

private fun chipLabel(date: String): String = runCatching {
    LocalDate.parse(date).format(chipFormatter.withLocale(Locale.getDefault()))
}.getOrDefault(date)
