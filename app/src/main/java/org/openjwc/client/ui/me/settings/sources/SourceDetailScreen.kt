package org.openjwc.client.ui.me.settings.sources

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Code
import androidx.compose.material.icons.twotone.CloudSync
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.Lock
import androidx.compose.material.icons.twotone.RssFeed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.openjwc.client.R
import org.openjwc.client.data.models.SourceEntity
import org.openjwc.client.navigation.Screen
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.ui.component.settings.AppBackButton
import org.openjwc.client.ui.component.settings.SegmentedColumn
import org.openjwc.client.ui.component.settings.SettingsBaseWidget
import org.openjwc.client.ui.component.settings.SettingsJumpPageWidget
import org.openjwc.client.ui.component.settings.SettingsSwitchWidget
import org.openjwc.client.viewmodels.SourcesViewModel
import org.openjwc.client.viewmodels.UiEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 数据源属性页：订阅开关、脚本编辑、恢复内置、删除。 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SourceDetailScreen(navigator: Navigator, viewModel: SourcesViewModel) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val source by viewModel.selectedSource.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(source?.name ?: stringResource(R.string.sources)) },
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
        SourceDetailContent(
            viewModel = viewModel,
            onBack = { navigator.pop() },
            onOpenScript = { navigator.push(Screen.SourceScript) },
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding)
        )
    }
}

@Composable
fun SourceDetailContent(
    viewModel: SourcesViewModel,
    onBack: () -> Unit,
    onOpenScript: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val source by viewModel.selectedSource.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRunResult by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        for (event in viewModel.uiEvent) {
            if (event is UiEvent.ShowToast) {
                Toast.makeText(context, event.uiText.asString(context), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val entity = source
    if (entity == null) return

    val editable = !entity.isBuiltIn

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        SegmentedColumn(title = stringResource(R.string.source_properties)) {
            item {
                SettingsBaseWidget(
                    icon = Icons.TwoTone.RssFeed,
                    title = entity.name,
                    description = buildString {
                        append(entity.id)
                        append(" · v").append(entity.version)
                        append(" · ")
                        append(
                            stringResource(
                                if (entity.isBuiltIn) R.string.source_kind_builtin
                                else R.string.source_kind_sideload
                            )
                        )
                    }
                ) {}
            }
            item {
                SettingsSwitchWidget(
                    icon = Icons.TwoTone.RssFeed,
                    title = stringResource(R.string.source_subscribe),
                    description = stringResource(R.string.source_subscribe_desc),
                    checked = entity.subscribed,
                    onCheckedChange = { viewModel.setSubscribed(entity.id, it) }
                )
            }
            item {
                SettingsBaseWidget(
                    icon = Icons.TwoTone.CloudSync,
                    title = stringResource(R.string.source_run_now),
                    enabled = !uiState.running,
                    trailingContent = if (uiState.running) {
                        { CircularWavyProgressIndicator(modifier = Modifier.padding(8.dp)) }
                    } else {
                        null
                    },
                    onClick = { viewModel.run(entity) }
                )
            }
        }

        SegmentedColumn(title = stringResource(R.string.source_last_run)) {
            item {
                // 有错误/警告时可以点开看完整信息（列表里只显示摘要）
                SettingsBaseWidget(
                    icon = Icons.TwoTone.Info,
                    title = stringResource(R.string.source_last_run),
                    description = lastRunDescription(entity),
                    descriptionColor = if (entity.lastError != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        null
                    },
                    onClick = entity.lastError?.let { { showRunResult = true } }
                )
            }
        }

        SegmentedColumn(title = stringResource(R.string.source_script)) {
            if (!editable) {
                item {
                    SettingsBaseWidget(
                        icon = Icons.TwoTone.Lock,
                        title = stringResource(R.string.source_builtin_readonly),
                        description = stringResource(R.string.source_builtin_readonly_desc)
                    ) {}
                }
            }
            item {
                // 脚本内容较长，放到独立的全屏编辑器里查看/编辑，避免在设置页里嵌套滚动
                SettingsJumpPageWidget(
                    icon = Icons.TwoTone.Code,
                    title = stringResource(
                        if (editable) R.string.source_script_edit else R.string.source_script_view
                    ),
                    onClick = { onOpenScript() }
                )
            }
        }

        if (editable) {
            Button(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Text(stringResource(R.string.source_delete))
            }
        }
    }

    if (showRunResult) {
        AlertDialog(
            onDismissRequest = { showRunResult = false },
            title = { Text(stringResource(R.string.source_last_run)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    SelectionContainer {
                        Text(
                            text = buildString {
                                append(lastRunDescription(entity))
                                entity.lastError?.let {
                                    append("\n\n")
                                    append(it)
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRunResult = false }) {
                    Text(stringResource(R.string.confirm))
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.source_delete)) },
            text = { Text(stringResource(R.string.source_delete_confirm, entity.name)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete(entity.id)
                    onBack()
                }) {
                    Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private fun lastRunDescription(source: SourceEntity): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val time = source.lastRunAt?.let { formatter.format(Date(it)) } ?: "尚未运行"
    return buildString {
        append(time)
        append(" · 新增 ").append(source.lastCount).append(" 条")
        source.lastError?.let { message ->
            append(" · ")
            // 完整信息（多行）在弹窗里看，这里只放摘要
            append(message.lineSequence().first())
        }
    }
}
