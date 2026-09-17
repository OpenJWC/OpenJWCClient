package org.openjwc.client.ui.me.settings.motto

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Category
import androidx.compose.material.icons.twotone.Cloud
import androidx.compose.material.icons.twotone.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import org.openjwc.client.net.hitokoto.HitokotoCategory
import org.openjwc.client.ui.component.settings.AppBackButton
import org.openjwc.client.ui.component.settings.SegmentedColumn
import org.openjwc.client.ui.component.settings.SettingsDropdownWidget
import org.openjwc.client.ui.component.settings.SettingsSwitchWidget
import org.openjwc.client.ui.component.settings.SettingsTextFieldWidget
import org.openjwc.client.viewmodels.SettingsViewModel

/**
 * 每日一言设置。
 * - 本地模式：自定义文本 + 作者（对齐后端 `motto_text` / `motto_author`）；
 * - 在线模式：取自 hitokoto.cn，可选分类与最大长度，按天缓存。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MottoSettingsScreen(navigator: Navigator, settingsViewModel: SettingsViewModel) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.motto)) },
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
        MottoSettingsContent(
            settingsViewModel = settingsViewModel,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding)
        )
    }
}

@Composable
fun MottoSettingsContent(settingsViewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    val textState = remember { TextFieldState("") }
    val authorState = remember { TextFieldState("") }
    val maxLengthState = remember { TextFieldState("") }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(settings.mottoText, settings.mottoAuthor, settings.hitokotoMaxLength) {
        if (!loaded) {
            textState.edit { replace(0, length, settings.mottoText) }
            authorState.edit { replace(0, length, settings.mottoAuthor) }
            maxLengthState.edit { replace(0, length, settings.hitokotoMaxLength.toString()) }
            loaded = true
        }
    }

    val textError = if (textState.text.isBlank()) stringResource(R.string.required) else ""
    val canSaveLocal = textError.isEmpty() &&
        (textState.text.toString() != settings.mottoText || authorState.text.toString() != settings.mottoAuthor)

    val maxLengthError = run {
        val value = maxLengthState.text.toString().toIntOrNull()
        when {
            maxLengthState.text.isBlank() -> stringResource(R.string.required)
            value == null || value <= 0 -> stringResource(R.string.must_be_positive_integer)
            value > 100 -> stringResource(R.string.hitokoto_max_length_hint)
            else -> ""
        }
    }

    // 分类：不限 + 全部官方分类。
    // 分类与最大长度都属于「在线一言」的表单，统一由下方保存按钮提交（改完要能点保存）。
    val categoryOptions = listOf("" to stringResource(R.string.hitokoto_category_any)) +
        HitokotoCategory.entries.map { it.code to it.label }
    val categoryLabels = categoryOptions.map { it.second }
    var pickedCategory by remember { mutableStateOf<String?>(null) }
    val effectiveCategory = pickedCategory ?: settings.hitokotoCategory
    val selectedCategoryIndex = categoryOptions
        .indexOfFirst { it.first == effectiveCategory }
        .coerceAtLeast(0)

    val maxLengthValue = maxLengthState.text.toString().toIntOrNull()
    val categoryChanged = pickedCategory != null && pickedCategory != settings.hitokotoCategory
    val maxLengthChanged = maxLengthValue != null && maxLengthValue != settings.hitokotoMaxLength
    val canSaveOnline = maxLengthError.isEmpty() && (categoryChanged || maxLengthChanged)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        SegmentedColumn(title = stringResource(R.string.motto)) {
            item {
                SettingsSwitchWidget(
                    icon = Icons.TwoTone.Cloud,
                    title = stringResource(R.string.motto_online),
                    description = stringResource(R.string.motto_online_desc),
                    checked = settings.mottoOnline,
                    onCheckedChange = { settingsViewModel.updateMottoOnline(it) }
                )
            }
            if (settings.mottoOnline) {
                item {
                    SettingsDropdownWidget(
                        icon = Icons.TwoTone.Category,
                        title = stringResource(R.string.hitokoto_category),
                        choice = selectedCategoryIndex,
                        data = categoryLabels,
                        trailingContent = {
                            Text(
                                text = categoryLabels.getOrNull(selectedCategoryIndex).orEmpty(),
                                style = MaterialTheme.typography.bodyMediumEmphasized,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onChoiceChange = { index ->
                            categoryOptions.getOrNull(index)?.let { pickedCategory = it.first }
                        }
                    )
                }
                item {
                    SettingsTextFieldWidget(
                        state = maxLengthState,
                        title = stringResource(R.string.hitokoto_max_length),
                        error = maxLengthError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            } else {
                item {
                    SettingsTextFieldWidget(
                        state = textState,
                        title = stringResource(R.string.motto_text),
                        error = textError,
                        lineLimits = TextFieldLineLimits.MultiLine(
                            minHeightInLines = 2,
                            maxHeightInLines = 4,
                        ),
                    )
                }
                item {
                    SettingsTextFieldWidget(
                        state = authorState,
                        title = stringResource(R.string.motto_author),
                        placeholder = stringResource(R.string.motto_author_placeholder),
                    )
                }
            }
        }

        // 保存按钮必须放在 SegmentedColumn 之外：分段圆角通过 LocalSegmentedItemShape
        // 只作用于 SettingsBaseWidget，普通 Button 放进去会破坏最后一段的大圆角。
        if (settings.mottoOnline) {
            Button(
                onClick = {
                    settingsViewModel.updateHitokotoCategory(effectiveCategory)
                    maxLengthValue?.let { settingsViewModel.updateHitokotoMaxLength(it) }
                },
                enabled = canSaveOnline,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Text(stringResource(R.string.save))
            }
        } else {
            Button(
                onClick = {
                    settingsViewModel.updateMotto(
                        text = textState.text.toString().trim(),
                        author = authorState.text.toString().trim(),
                    )
                },
                enabled = canSaveLocal,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
