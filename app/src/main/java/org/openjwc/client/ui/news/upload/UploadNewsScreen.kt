package org.openjwc.client.ui.news.upload

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openjwc.client.net.models.UploadedNotice
import org.openjwc.client.net.models.UploadedNoticeContent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@Preview
@Composable
fun UploadNewsPreview() {
    UploadNewsScreen(
        errorMessage = null,
        onBack = {},
        onUpload = {}
    )
}

const val MAX_TITLE = 200
const val MAX_LABEL = 20
const val MAX_URL = 1000
const val MAX_CONTENT = 10000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadNewsScreen(
    errorMessage: String?,
    onBack: () -> Unit,
    onUpload: (UploadedNotice) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var label by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf("") }
    var detailUrl by rememberSaveable { mutableStateOf("") }
    var contentText by rememberSaveable { mutableStateOf("") }
    var isPage by rememberSaveable { mutableStateOf(true) }
    var isUploading by rememberSaveable { mutableStateOf(false) }
    val attachmentUrls = rememberSaveable(saver = listSaver(
        save = { it.toList() },
        restore = { it.toMutableStateList() }
    )) { mutableStateListOf<String>() }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialDisplayMode = DisplayMode.Picker
    )

    val dateRegex = remember { Regex("""^\d{4}-\d{2}-\d{2}$""") }
    val urlRegex = remember {
        Regex("""^(https?|ftp)://[^\s/$.?#].\S*$""", RegexOption.IGNORE_CASE)
    }
    val isUrlValid: (String) -> Boolean = remember {{it.isNotBlank() && it.matches(urlRegex)} }
    val isTitleValid = title.isNotBlank() && title.length <= MAX_TITLE
    val isLabelValid = label.isNotBlank() && label.length <= MAX_LABEL
    val isDateValid = date.matches(dateRegex)
    val isDetailUrlValid = isUrlValid(detailUrl) && detailUrl.length <= MAX_URL
    val isContentValid = contentText.isNotBlank() && contentText.length <= MAX_CONTENT
    val areAttachmentsValid = attachmentUrls.all { it.isNotBlank() && isUrlValid(it) && it.length <= MAX_URL }

    val canSubmit = isTitleValid && isLabelValid && isDateValid &&
            isDetailUrlValid && isContentValid && areAttachmentsValid

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        date = selectedDate.format(formatter)
                    }
                    showDatePicker = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            isUploading = false
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text("投稿资讯") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (canSubmit) {
                        isUploading = true
                        val notice = UploadedNotice(
                            label = label,
                            title = title,
                            date = date,
                            detailUrl = detailUrl,
                            isPage = isPage,
                            content = UploadedNoticeContent(
                                text = contentText,
                                attachmentUrls = attachmentUrls.filter { it.isNotBlank() }
                            )
                        )
                        onUpload(notice)
                    }
                },
                icon = {
                    if (isUploading) {
                        CircularWavyProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                    }
                },
                text = { Text(if (isUploading) "上传中..." else "提交上传") },
                containerColor = if (canSubmit) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (canSubmit) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .windowInsetsPadding(WindowInsets.ime)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            Text(
                "基本信息",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                isError = !isTitleValid,
                supportingText = {
                    Text(
                        text = "${title.length} / $MAX_TITLE",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        color = if (title.length > MAX_TITLE) MaterialTheme.colorScheme.error else Color.Unspecified
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("标签") },
                    isError = !isLabelValid,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    supportingText = {
                        Text(
                            text = "${label.length} / $MAX_LABEL",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                            color = if (label.length > MAX_LABEL) MaterialTheme.colorScheme.error else Color.Unspecified
                        )
                    }
                )

                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = {},
                        label = { Text("日期") },
                        placeholder = { Text("yyyy-MM-dd") },
                        readOnly = true,
                        isError = !isDateValid || date.isBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable{
                                showDatePicker = true
                            }
                    )
                }
            }

            OutlinedTextField(
                value = detailUrl,
                onValueChange = { detailUrl = it },
                label = { Text("详情链接 (URL)") },
                isError = !isDetailUrlValid,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        if (!isUrlValid(detailUrl) && detailUrl.isNotBlank()) {
                            Text("URL 格式非法")
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }
                        Text(
                            text = "${detailUrl.length} / $MAX_URL",
                            color = if (detailUrl.length > MAX_URL) MaterialTheme.colorScheme.error else Color.Unspecified
                        )
                    }
                }
            )

            // isPage 开关
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = { Text("详情链接为网页") },
                    supportingContent = { Text("关闭则视为附件或外链") },
                    trailingContent = {
                        Switch(
                            checked = isPage,
                            onCheckedChange = { isPage = it }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            Text(
                "正文内容",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            OutlinedTextField(
                value = contentText,
                onValueChange = { contentText = it },
                label = { Text("Markdown 文本") },
                isError = !isContentValid,
                modifier = Modifier.fillMaxWidth().height(200.dp),
                supportingText = {
                    val color = if (contentText.length > MAX_CONTENT) MaterialTheme.colorScheme.error else Color.Unspecified
                    Text(
                        text = "${contentText.length} / $MAX_CONTENT",
                        color = color,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "附件 URL 列表 (${attachmentUrls.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(
                    onClick = { attachmentUrls.add("") },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("添加附件 URL")
                }
            }

            attachmentUrls.forEachIndexed { index, url ->
                AttachmentInputRow(
                    url = url,
                    onValueChange = { newValue -> attachmentUrls[index] = newValue },
                    onDelete = { attachmentUrls.removeAt(index) },
                    isError = !isUrlValid(url) || url.length > MAX_URL
                )
            }

            if (attachmentUrls.isEmpty()) {
                Text(
                    "暂无附件 URL (可选)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun AttachmentInputRow(
    url: String,
    onValueChange: (String) -> Unit,
    onDelete: () -> Unit,
    isError: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = url,
            onValueChange = onValueChange,
            label = { Text("URL") },
            placeholder = { Text("https://...") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            leadingIcon = {
                Icon(
                    Icons.Default.Link,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            isError = isError,
            supportingText = {
                if (url.length > 1000) Text("超过1000字符")
                else if (isError && url.isNotBlank()) Text("格式非法")
            },
        )
        IconButton(
            onClick = onDelete,
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.Delete, contentDescription = "删除附件 URL")
        }
    }
}