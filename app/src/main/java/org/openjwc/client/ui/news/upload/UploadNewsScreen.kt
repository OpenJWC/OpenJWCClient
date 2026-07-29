package org.openjwc.client.ui.news.upload

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Add
import androidx.compose.material.icons.twotone.CloudUpload
import androidx.compose.material.icons.twotone.DateRange
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Link
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.openjwc.client.R
import org.openjwc.client.data.datastore.AuthDataSource
import org.openjwc.client.data.datastore.CachedDataSource
import org.openjwc.client.data.datastore.SettingsDataSource
import org.openjwc.client.data.db.AppDatabase
import org.openjwc.client.data.repository.AuthRepository
import org.openjwc.client.data.repository.NewsRepository
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.net.models.UploadedNotice
import org.openjwc.client.net.models.UploadedNoticeContent
import org.openjwc.client.ui.component.settings.AppBackButton
import org.openjwc.client.ui.component.settings.SegmentedColumn
import org.openjwc.client.ui.component.settings.SettingsBaseWidget
import org.openjwc.client.ui.component.settings.SettingsSwitchWidget
import org.openjwc.client.ui.component.settings.SettingsTextFieldWidget
import org.openjwc.client.viewmodels.NavEvent
import org.openjwc.client.viewmodels.NewsViewModel
import org.openjwc.client.viewmodels.NewsViewModelFactory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UploadNewsScreen(navigator: Navigator) {
    val context = LocalContext.current
    val authDataSource = remember { AuthDataSource(context) }
    val settingsDataSource = remember { SettingsDataSource(context) }
    val settingsRepository = remember { SettingsRepository(settingsDataSource, CachedDataSource(context), authDataSource, context) }
    val authRepository = remember { AuthRepository(authDataSource, settingsDataSource) }
    val db = remember { AppDatabase.getDatabase(context) }
    val newsRepository = remember { NewsRepository(db.newsDao(), settingsDataSource, authDataSource) }
    val newsViewModel: NewsViewModel = viewModel(factory = NewsViewModelFactory(settingsRepository, newsRepository, authRepository))

    val uploadError by newsViewModel.uploadError.collectAsState()
    var isUploading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        for (event in newsViewModel.navEvent) {
            when (event) {
                is NavEvent.ToBack -> navigator.pop()
                else -> {}
            }
        }
    }

    LaunchedEffect(uploadError) {
        if (uploadError != null) isUploading = false
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val titleState = remember { TextFieldState() }
    val labelState = remember { TextFieldState() }
    val detailUrlState = remember { TextFieldState() }
    val contentState = remember { TextFieldState() }
    val dateState = remember { TextFieldState() }
    var isPage by remember { mutableStateOf(true) }
    val attachmentStates = remember { mutableStateListOf<TextFieldState>() }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialDisplayMode = DisplayMode.Picker)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val titleError = if (titleState.text.isBlank()) "Required" else ""
    val labelError = if (labelState.text.isBlank()) "Required" else ""
    val detailUrlError = if (detailUrlState.text.isBlank()) "Required" else ""
    val contentError = if (contentState.text.isBlank()) "Required" else ""
    val dateError = if (dateState.text.isBlank()) "Required" else ""
    val canSubmit = titleError.isEmpty() && labelError.isEmpty() && dateError.isEmpty() &&
            detailUrlError.isEmpty() && contentError.isEmpty()
    val scrollState = rememberScrollState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val d = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        dateState.edit { replace(0, length, d.format(formatter)) }
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) { DatePicker(state = datePickerState, showModeToggle = false) }
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.upload_news)) },
                navigationIcon = { AppBackButton(onClick = { navigator.pop() }) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(innerPadding)
        ) {
            SegmentedColumn(title = stringResource(R.string.basic_information)) {
                item { SettingsTextFieldWidget(state = titleState, title = stringResource(R.string.title), error = titleError) }
                item { SettingsTextFieldWidget(state = labelState, title = stringResource(R.string.label), error = labelError) }
                item {
                    SettingsTextFieldWidget(
                        state = dateState,
                        title = stringResource(R.string.date),
                        error = dateError,
                        leadingContent = {
                            Icon(Icons.TwoTone.DateRange, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingContent = {
                            IconButton(onClick = { showDatePicker = true }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.TwoTone.DateRange, null, Modifier.size(18.dp))
                            }
                        }
                    )
                }
                item { SettingsTextFieldWidget(state = detailUrlState, title = stringResource(R.string.detail_url), error = detailUrlError) }
                item {
                    SettingsSwitchWidget(
                        icon = Icons.TwoTone.Link,
                        title = stringResource(R.string.link_is_a_page),
                        description = stringResource(R.string.link_is_a_page_desc),
                        checked = isPage,
                        onCheckedChange = { isPage = it }
                    )
                }
            }

            SegmentedColumn(title = stringResource(R.string.main_content)) {
                item { SettingsTextFieldWidget(state = contentState, title = stringResource(R.string.markdown_text), error = contentError) }
            }

            if (attachmentStates.isNotEmpty()) {
                SegmentedColumn(title = stringResource(R.string.attachment_url_lists, attachmentStates.size)) {
                    attachmentStates.forEachIndexed { index, state ->
                        item {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Icon(Icons.TwoTone.Link, null, Modifier.size(18.dp).padding(start = 4.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    SettingsTextFieldWidget(state = state, title = "URL $index")
                                }
                                Spacer(Modifier.width(4.dp))
                                IconButton(onClick = { attachmentStates.removeAt(index) }) {
                                    Icon(Icons.TwoTone.Delete, stringResource(R.string.delete_attachment_url), Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            SegmentedColumn {
                item {
                    SettingsBaseWidget(
                        icon = Icons.TwoTone.Add,
                        title = stringResource(R.string.add_attachment_urls),
                        onClick = { attachmentStates.add(TextFieldState()) }
                    ) {}
                }
            }

            if (uploadError != null) {
                Text(
                    text = uploadError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp)
                )
            }

            Button(
                onClick = {
                    isUploading = true
                    newsViewModel.uploadNews(
                        UploadedNotice(
                            label = labelState.text.toString(),
                            title = titleState.text.toString(),
                            date = dateState.text.toString(),
                            detailUrl = detailUrlState.text.toString(),
                            isPage = isPage,
                            content = UploadedNoticeContent(
                                text = contentState.text.toString(),
                                attachmentUrls = attachmentStates.map { it.text.toString() }.filter { it.isNotBlank() }
                            )
                        )
                    )
                },
                enabled = canSubmit && !isUploading,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (isUploading) {
                    CircularWavyProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.uploading))
                } else {
                    Icon(Icons.TwoTone.CloudUpload, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.submit_upload))
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
