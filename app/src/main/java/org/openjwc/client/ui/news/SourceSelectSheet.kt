package org.openjwc.client.ui.news

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openjwc.client.R
import org.openjwc.client.data.models.SourceEntity
import org.openjwc.client.ui.component.settings.SegmentedColumn
import org.openjwc.client.ui.component.settings.SettingsBaseWidget

/**
 * 数据源切换 sheet（对齐课程表的 [TableSelectSheet]）：
 * 由资讯页顶栏标题点开，选择「全部数据源」或某个订阅源。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceSelectSheet(
    sources: List<SourceEntity>,
    selectedSourceId: String?,
    onSelect: (String?) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val allLabel = stringResource(R.string.source_all_sources)
    val subscribedText = stringResource(R.string.source_subscribed)
    val unsubscribedText = stringResource(R.string.source_unsubscribed)

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.source_switch),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
            )

            SegmentedColumn {
                item(key = "__all__") {
                    val isSelected = selectedSourceId == null
                    SettingsBaseWidget(
                        leadingContent = { RadioButton(selected = isSelected, onClick = null) },
                        title = allLabel,
                        titleStyle = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        description = stringResource(R.string.source_all_sources_desc),
                        selected = isSelected,
                        onClick = {
                            onSelect(null)
                            onDismissRequest()
                        }
                    )
                }
                sources.forEach { source ->
                    item(key = source.id) {
                        val isSelected = source.id == selectedSourceId
                        SettingsBaseWidget(
                            leadingContent = { RadioButton(selected = isSelected, onClick = null) },
                            title = source.name,
                            titleStyle = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            description = buildString {
                                append(source.id)
                                append(" · ")
                                append(if (source.subscribed) subscribedText else unsubscribedText)
                            },
                            selected = isSelected,
                            onClick = {
                                onSelect(source.id)
                                onDismissRequest()
                            }
                        )
                    }
                }
            }
        }
    }
}
