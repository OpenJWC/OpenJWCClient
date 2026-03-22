package org.openjwc.client.ui.me.settings.news

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openjwc.client.net.models.FetchedNotice
import org.openjwc.client.ui.news.InfoCard
import java.time.LocalDate

@Preview
@Composable
fun TestNewsDisplaySettingsScreen() {
    NewsDisplaySettingsScreen(
        initialFreshDays = 3,
        onSave = {},
        onBack = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsDisplaySettingsScreen(
    initialFreshDays: Int,
    onSave: (Int) -> Unit,
    onBack: () -> Unit,
) {
    var freshDaysString by remember { mutableStateOf(initialFreshDays.toString()) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val freshDaysInt = freshDaysString.toIntOrNull()
    val isValid = freshDaysInt != null && freshDaysInt >= 0
    val isChanged = freshDaysInt != initialFreshDays
    val canSave = isValid && isChanged

    LaunchedEffect(initialFreshDays) {
        freshDaysString = initialFreshDays.toString()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("资讯显示设置") }, // 修改了标题使其更符合功能
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { if (canSave) onSave(freshDaysInt) },
                        enabled = canSave
                    ) {
                        Text("保存")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 设置项卡片
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "高亮新鲜资讯",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "设置资讯发布后多少天内显示为“新鲜”状态。新鲜资讯在列表中会以高亮颜色显示并带有标识。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = freshDaysString,
                        onValueChange = {
                            // 过滤掉非数字输入，只允许数字
                            if (it.all { char -> char.isDigit() }) {
                                freshDaysString = it
                            }
                        },
                        label = { Text("新鲜期限 (天)") },
                        placeholder = { Text("例如: 3") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = !isValid && freshDaysString.isNotEmpty(),
                        supportingText = {
                            if (!isValid && freshDaysString.isNotEmpty()) {
                                Text("请输入有效的正整数")
                            } else {
                                Text("设置为 0 则禁用高亮")
                            }
                        },
                        trailingIcon = {
                            if (freshDaysString.isNotEmpty()) {
                                IconButton(onClick = { freshDaysString = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "清除")
                                }
                            }
                        }
                    )
                }
            }

            // 预览效果区
            Text(
                text = "预览效果",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )

            InfoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                fetchedNotice = FetchedNotice(
                    id = "preview",
                    title = "这是一条最新最热资讯标题",
                    date = LocalDate.now().toString(),
                    label = "通知",
                    detailUrl = "",
                    isPage = true,
                    contentText = "",
                    attachmentUrls = emptyList()
                ),
                onClick = {},
                isFresh = isValid && freshDaysInt != 0
            )
            InfoCard(
                Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                fetchedNotice = FetchedNotice(
                    id = "preview",
                    title = "这是一条最旧最冷资讯标题",
                    date = LocalDate.now().toString(),
                    label = "通知",
                    detailUrl = "",
                    isPage = true,
                    contentText = "",
                    attachmentUrls = emptyList()
                ),
                onClick = {},
                isFresh = false
            )
        }
    }
}