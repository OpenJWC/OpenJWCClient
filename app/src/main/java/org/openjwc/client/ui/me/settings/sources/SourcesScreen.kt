package org.openjwc.client.ui.me.settings.sources

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.CloudSync
import androidx.compose.material.icons.twotone.DeleteSweep
import androidx.compose.material.icons.twotone.Folder
import androidx.compose.material.icons.twotone.FileOpen
import androidx.compose.material.icons.twotone.RssFeed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.openjwc.client.R
import org.openjwc.client.data.models.SourceEntity
import org.openjwc.client.navigation.Screen
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.ui.component.CrawlProgressDialog
import org.openjwc.client.ui.component.settings.AppBackButton
import org.openjwc.client.ui.component.settings.SegmentedColumn
import org.openjwc.client.ui.component.settings.SettingsBaseWidget
import org.openjwc.client.ui.component.settings.SettingsJumpPageWidget
import org.openjwc.client.ui.component.settings.SettingsTextFieldWidget
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.input.KeyboardType
import org.openjwc.client.viewmodels.SourcesViewModel
import org.openjwc.client.viewmodels.UiEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SourcesScreen(navigator: Navigator, viewModel: SourcesViewModel) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.sources_settings)) },
                navigationIcon = { AppBackButton(onClick = { navigator.pop() }) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        SourcesContent(
            viewModel = viewModel,
            onOpenSource = { sourceId ->
                viewModel.selectSource(sourceId)
                navigator.push(Screen.SourceDetail)
            },
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SourcesContent(
    viewModel: SourcesViewModel,
    onOpenSource: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sources by viewModel.sources.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cacheCount by viewModel.cacheCount.collectAsStateWithLifecycle()
    val counts by viewModel.counts.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showClearCacheConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        for (event in viewModel.uiEvent) {
            if (event is UiEvent.ShowToast) {
                Toast.makeText(context, event.uiText.asString(context), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val crawlProgress by viewModel.crawlProgress.collectAsStateWithLifecycle()
    if (crawlProgress.running || crawlProgress.results.isNotEmpty()) {
        CrawlProgressDialog(
            progress = crawlProgress,
            onCancel = { viewModel.cancelRunAll() },
            onDismiss = { viewModel.dismissCrawlProgress() },
        )
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importFromUri(it) }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        SegmentedColumn(title = stringResource(R.string.sources)) {
            sources.forEach { source ->
                item(key = source.id) {
                    SourceItem(
                        source = source,
                        localCount = counts[source.id] ?: 0,
                        onClick = { onOpenSource(source.id) }
                    )
                }
            }
        }

        val savedCrawlDaysGap by viewModel.crawlDaysGap.collectAsStateWithLifecycle()
        val crawlDaysGapState = remember { TextFieldState("") }
        var crawlDaysGapLoaded by remember { mutableStateOf(false) }
        if (!crawlDaysGapLoaded) {
            crawlDaysGapState.edit { replace(0, length, savedCrawlDaysGap.toString()) }
            crawlDaysGapLoaded = true
        }
        val crawlDaysGapError = run {
            val days = crawlDaysGapState.text.toString().toIntOrNull()
            when {
                crawlDaysGapState.text.isBlank() -> stringResource(R.string.required)
                days == null || days <= 0 -> stringResource(R.string.must_be_positive_integer)
                else -> ""
            }
        }
        // 只在输入合法且与已保存值不同时写入，避免无意义的重排抓取任务
        LaunchedEffect(crawlDaysGapState.text) {
            val days = crawlDaysGapState.text.toString().toIntOrNull() ?: return@LaunchedEffect
            if (days > 0 && days != savedCrawlDaysGap) viewModel.setCrawlDaysGap(days)
        }

        SegmentedColumn(title = stringResource(R.string.source_crawl_settings)) {
            item {
                SettingsTextFieldWidget(
                    state = crawlDaysGapState,
                    title = stringResource(R.string.source_crawl_days_gap),
                    error = crawlDaysGapError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        SegmentedColumn {
            item {
                SettingsBaseWidget(
                    icon = Icons.TwoTone.FileOpen,
                    title = stringResource(R.string.source_import),
                    description = stringResource(R.string.source_import_desc),
                    onClick = {
                        importLauncher.launch(
                            arrayOf("text/javascript", "application/javascript", "text/plain", "*/*")
                        )
                    }
                ) {}
            }
            item {
                SettingsBaseWidget(
                    icon = Icons.TwoTone.CloudSync,
                    title = stringResource(R.string.source_run_all),
                    enabled = !uiState.running,
                    trailingContent = if (uiState.running) {
                        { CircularWavyProgressIndicator(modifier = Modifier.padding(8.dp)) }
                    } else {
                        null
                    },
                    onClick = { viewModel.runAllSubscribed() }
                )
            }
        }

        // 「存储与缓存」原为独立设置页，现并入数据源（缓存就是抓下来的资讯语料）
        SegmentedColumn(title = stringResource(R.string.storage_and_cache)) {
            item {
                SettingsBaseWidget(
                    icon = Icons.TwoTone.Folder,
                    title = stringResource(R.string.news_cache),
                    description = stringResource(R.string.news_cache_count, cacheCount)
                ) {}
            }
            item {
                SettingsBaseWidget(
                    icon = Icons.TwoTone.DeleteSweep,
                    title = stringResource(R.string.clear_news_cache),
                    onClick = { showClearCacheConfirm = true }
                )
            }
        }
    }

    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            title = { Text(stringResource(R.string.clear_news_cache)) },
            text = { Text(stringResource(R.string.clear_news_cache_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearCache()
                    showClearCacheConfirm = false
                }) {
                    Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SourceItem(
    source: SourceEntity,
    localCount: Int,
    onClick: () -> Unit,
) {
    val localCountText = stringResource(R.string.source_local_count, localCount)
    val subscribeText = stringResource(
        if (source.subscribed) R.string.source_subscribed else R.string.source_unsubscribed
    )
    val newCountText = stringResource(R.string.source_last_new, source.lastCount)
    val description = remember(source, localCount, localCountText, subscribeText, newCountText) {
        buildString {
            append(source.id)
            append(" · ")
            append(localCountText)
            append(" · ")
            append(subscribeText)
            source.lastRunAt?.let { timestamp ->
                append(" · ")
                append(formatTime(timestamp))
                append(" · ")
                append(newCountText)
            }
            source.lastError?.let { message ->
                append(" · ")
                // 运行结果可能是多行（第一行摘要 + 逐条警告），列表里只显示摘要
                append(message.lineSequence().first())
            }
        }
    }

    SettingsJumpPageWidget(
        // 已订阅用主题色，未订阅用灰色
        icon = Icons.TwoTone.RssFeed,
        iconColor = if (source.subscribed) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        title = source.name,
        titleStyle = MaterialTheme.typography.titleMedium.copy(
            color = if (source.subscribed) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ),
        description = description,
        onClick = { onClick() }
    )
}

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
