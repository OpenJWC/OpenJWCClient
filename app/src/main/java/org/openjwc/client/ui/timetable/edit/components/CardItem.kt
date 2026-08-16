package org.openjwc.client.ui.timetable.edit.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.openjwc.client.ui.component.settings.LocalSegmentedItemShape
import org.openjwc.client.ui.theme.CardConfig

/**
 * 与 [org.openjwc.client.ui.component.settings.SettingsBaseWidget] 视觉一致的卡片容器，
 * 用于在 SegmentedColumn 中承载自定义内容（如滑块、日期行、节次行）。
 */
@Composable
fun CardItem(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(
        horizontal = 16.dp,
        vertical = 12.dp
    ),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val shape = LocalSegmentedItemShape.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceBright.copy(alpha = CardConfig.cardAlpha)
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}
