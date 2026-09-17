package org.openjwc.client.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.openjwc.client.R
import org.openjwc.client.agent.AgentTools
import org.openjwc.client.data.repository.ToolUiState

/**
 * Agent 工具调用卡片：用中文说明「做了什么」，并逐行列出调用参数。
 * 参数较多时默认折叠为一行，点击卡片展开全部；不展示英文工具名等实现细节。
 */
@Composable
fun ToolCard(
    tool: ToolUiState,
    modifier: Modifier = Modifier,
    /** 点击卡片打开该资讯详情（仅 read_notice 卡片可用）。参数为资讯 id。 */
    onOpenNotice: ((String) -> Unit)? = null,
) {
    var expanded by rememberSaveable(tool.id) { mutableStateOf(false) }
    val title = AgentTools.displayName(tool.name)
    val detailLines = tool.summary.lineSequence().filter { it.isNotBlank() }.toList()
    val expandable = detailLines.size > 1
    val noticeId = tool.targetId?.takeIf { tool.name == AgentTools.TOOL_READ && it.isNotBlank() }
    val navigable = noticeId != null && onOpenNotice != null

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.clickable(enabled = navigable || expandable) {
            if (navigable) onOpenNotice?.invoke(noticeId!!) else expanded = !expanded
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            when (tool.status) {
                ToolUiState.STATUS_COMPLETED -> Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )

                ToolUiState.STATUS_FAILED -> Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error,
                )

                else -> CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (navigable) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (expandable) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { expanded = !expanded },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (detailLines.isNotEmpty()) {
                    Text(
                        text = if (expanded) {
                            detailLines.joinToString("\n")
                        } else {
                            detailLines.first()
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            tool.durationMs?.let { duration ->
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.tool_duration_ms, duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
