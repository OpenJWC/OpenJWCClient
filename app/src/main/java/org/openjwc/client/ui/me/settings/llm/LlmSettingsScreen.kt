package org.openjwc.client.ui.me.settings.llm

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.Article
import androidx.compose.material.icons.twotone.Schedule
import androidx.compose.material.icons.twotone.Memory
import androidx.compose.material.icons.twotone.NetworkCheck
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.openjwc.client.R
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.net.llm.LlmPresets
import org.openjwc.client.ui.component.settings.AppBackButton
import org.openjwc.client.ui.component.settings.SegmentedColumn
import org.openjwc.client.ui.component.settings.SettingsDropdownWidget
import org.openjwc.client.ui.component.settings.SettingsBaseWidget
import org.openjwc.client.ui.component.settings.SettingsChooseWidget
import org.openjwc.client.ui.component.settings.SettingsSwitchWidget
import org.openjwc.client.ui.component.settings.SettingsTextFieldWidget
import org.openjwc.client.viewmodels.SettingsViewModel
import org.openjwc.client.viewmodels.LlmSettingsViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LlmSettingsScreen(navigator: Navigator, viewModel: LlmSettingsViewModel, settingsViewModel: SettingsViewModel) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.llm_settings)) },
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
        LlmSettingsContent(
            viewModel = viewModel,
            settingsViewModel = settingsViewModel,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LlmSettingsContent(
    viewModel: LlmSettingsViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    val baseUrlState = remember { TextFieldState("") }
    val modelState = remember { TextFieldState("") }
    val keyState = remember { TextFieldState("") }

    // 以「已加载的配置」为准回填输入框：用 providerId 作 key 时，若保存的供应商恰好是默认值，
    // 回填会漏掉（providerId 没变化），因此这里以整个 config 为 key。
    LaunchedEffect(uiState.config) {
        baseUrlState.edit { replace(0, length, uiState.config.baseUrl) }
        modelState.edit { replace(0, length, uiState.config.model) }
        keyState.edit { replace(0, length, uiState.apiKey) }
    }

    // 表单语义：只有输入与已保存值不同时才可保存；保存后停留本页
    val canSave = baseUrlState.text.toString().trim() != uiState.config.baseUrl ||
        modelState.text.toString().trim() != uiState.config.model ||
        keyState.text.toString() != uiState.apiKey

    val presets = LlmPresets.all
    val selectedIndex = presets.indexOfFirst { it.id == uiState.config.providerId }
        .coerceIn(0, presets.lastIndex)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        SegmentedColumn(title = stringResource(R.string.llm_provider)) {
            item {
                SettingsChooseWidget(
                    icon = Icons.TwoTone.Memory,
                    title = stringResource(R.string.llm_provider),
                    items = presets.map { it.name },
                    selectedIndex = selectedIndex,
                    onSelectedIndexChange = { viewModel.selectProvider(presets[it].id) }
                )
            }
            item {
                SettingsTextFieldWidget(
                    state = baseUrlState,
                    title = stringResource(R.string.llm_base_url),
                    placeholder = "https://api.openai.com/v1",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
            }
            item {
                SettingsTextFieldWidget(
                    state = modelState,
                    title = stringResource(R.string.llm_model),
                    placeholder = "gpt-4o-mini",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
            }
        }

        SegmentedColumn(title = stringResource(R.string.llm_api_key)) {
            item {
                SettingsTextFieldWidget(
                    state = keyState,
                    title = stringResource(R.string.llm_api_key),
                    placeholder = "sk-...",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            }
        }

        val reportTimeChoices = listOf("00:00", "06:00", "08:00", "12:00", "18:00", "22:00")
        val selectedReportTimeIndex = reportTimeChoices
            .indexOf(settings.dailyReportTime)
            .coerceAtLeast(0)

        // 日报由用户自己的模型生成，因此放在 AI 设置里
        SegmentedColumn(title = stringResource(R.string.daily_report)) {
            item {
                SettingsSwitchWidget(
                    icon = Icons.AutoMirrored.TwoTone.Article,
                    title = stringResource(R.string.daily_report_auto),
                    description = stringResource(R.string.daily_report_auto_desc),
                    checked = settings.dailyReportEnabled,
                    onCheckedChange = { checked ->
                        settingsViewModel.updateDailyReportEnabled(checked)
                    }
                )
            }
            item {
                SettingsDropdownWidget(
                    icon = Icons.TwoTone.Schedule,
                    title = stringResource(R.string.daily_report_time),
                    enabled = settings.dailyReportEnabled,
                    choice = selectedReportTimeIndex,
                    data = reportTimeChoices,
                    trailingContent = {
                        Text(
                            text = reportTimeChoices.getOrNull(selectedReportTimeIndex) ?: "",
                            style = MaterialTheme.typography.bodyMediumEmphasized,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                .copy(alpha = if (settings.dailyReportEnabled) 1f else 0.38f)
                        )
                    },
                    onChoiceChange = { index ->
                        reportTimeChoices.getOrNull(index)?.let {
                            settingsViewModel.updateDailyReportTime(it)
                        }
                    }
                )
            }
        }

        SegmentedColumn(title = stringResource(R.string.llm_test)) {
            item {
                // 可直接用输入框里的当前值测试，无需先保存
                SettingsBaseWidget(
                    icon = Icons.TwoTone.NetworkCheck,
                    title = if (uiState.testing) {
                        stringResource(R.string.llm_testing)
                    } else {
                        stringResource(R.string.llm_test)
                    },
                    description = uiState.testResult
                        ?: uiState.testError
                        ?: stringResource(R.string.llm_test_desc),
                    descriptionColor = when {
                        uiState.testError != null -> MaterialTheme.colorScheme.error
                        uiState.testResult != null -> MaterialTheme.colorScheme.primary
                        else -> null
                    },
                    enabled = !uiState.testing,
                    trailingContent = if (uiState.testing) {
                        { CircularWavyProgressIndicator(modifier = Modifier.padding(8.dp)) }
                    } else {
                        null
                    },
                    onClick = {
                        viewModel.testConnection(
                            baseUrl = baseUrlState.text.toString(),
                            model = modelState.text.toString(),
                            apiKey = keyState.text.toString(),
                        )
                    }
                )
            }
        }

        Button(
            onClick = {
                viewModel.updateBaseUrl(baseUrlState.text.toString())
                viewModel.updateModel(modelState.text.toString())
                viewModel.updateApiKey(keyState.text.toString())
            },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp)
        ) {
            Text(stringResource(R.string.save))
        }
    }
}
