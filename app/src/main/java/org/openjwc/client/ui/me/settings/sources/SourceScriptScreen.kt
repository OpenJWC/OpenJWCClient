package org.openjwc.client.ui.me.settings.sources

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.openjwc.client.R
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.ui.component.settings.AppBackButton
import org.openjwc.client.viewmodels.SourcesViewModel
import org.openjwc.client.viewmodels.UiEvent

/**
 * 数据源脚本的全屏查看/编辑器。
 *
 * 脚本可能有几百行，放在设置页的 `verticalScroll` 里会造成「大文本 + 嵌套滚动」卡顿；
 * 这里让 [BasicTextField] 独占整屏高度、自己滚动，去掉外层滚动容器。
 * 内置脚本只读，侧载脚本可在右上角保存。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceScriptScreen(navigator: Navigator, viewModel: SourcesViewModel) {
    val source by viewModel.selectedSource.collectAsStateWithLifecycle()
    val script by viewModel.script.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val scriptState = remember { TextFieldState("") }

    // 载入脚本内容（恢复内置 / 保存成功后 script 会变化，这里自动重新回填）
    LaunchedEffect(script) {
        val text = script ?: return@LaunchedEffect
        if (text != scriptState.text.toString()) {
            scriptState.edit { replace(0, length, text) }
        }
    }

    LaunchedEffect(Unit) {
        for (event in viewModel.uiEvent) {
            if (event is UiEvent.ShowToast) {
                Toast.makeText(context, event.uiText.asString(context), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val entity = source
    val editable = entity?.isBuiltIn == false
    val dirty = scriptState.text.toString() != script.orEmpty()
    val canSave = editable && dirty && scriptState.text.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(entity?.name ?: stringResource(R.string.source_script)) },
                navigationIcon = { AppBackButton(onClick = { navigator.pop() }) },
                actions = {
                    if (editable) {
                        TextButton(
                            onClick = { viewModel.saveScript(scriptState.text.toString()) },
                            enabled = canSave,
                        ) {
                            Text(stringResource(R.string.source_save_script))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        if (entity == null) return@Scaffold

        BasicTextField(
            state = scriptState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            readOnly = !editable,
            textStyle = MaterialTheme.typography.bodyMediumEmphasized.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace,
            ),
            lineLimits = TextFieldLineLimits.Default,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        )
    }
}
