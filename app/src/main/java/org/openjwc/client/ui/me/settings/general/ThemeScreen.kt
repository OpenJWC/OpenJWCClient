package org.openjwc.client.ui.me.settings.general

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.openjwc.client.ui.theme.ColorType
import org.openjwc.client.ui.theme.DarkThemeStyle
import org.openjwc.client.ui.theme.seedColors
import java.io.File

@Preview
@Composable
fun TestThemeScreen() {
    ThemeScreen(
        onSelect = { _, _ ->
        },
        onBack = {},
        colorPresets = seedColors,
        onSelectBackground = {},
        onClearBackground = {},
        currentBackgroundPath = "",
        darkThemeStyle = DarkThemeStyle.Auto,
        selectedColorType = ColorType.Dynamic,
        onAlphaChange = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(
    onBack: () -> Unit,
    onSelect: (ColorType, DarkThemeStyle) -> Unit,
    onSelectBackground: (Uri) -> Unit,
    onClearBackground: () -> Unit,
    currentBackgroundPath: String?,
    backgroundAlpha: Float = 1f,
    onAlphaChange: (Float) -> Unit,
    colorPresets: List<Color>,
    selectedColorType: ColorType = ColorType.Dynamic,
    darkThemeStyle: DarkThemeStyle = DarkThemeStyle.Auto
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onSelectBackground(it) }
    }
    var sliderValue by remember(backgroundAlpha) { mutableFloatStateOf(backgroundAlpha) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("个性化主题") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "显示模式",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )

            ThemeStyleSelector(
                selectedStyle = darkThemeStyle,
                onStyleSelected = {
                    onSelect(selectedColorType, it)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp))

            Text(
                text = "颜色主题",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )

            ListItem(
                headlineContent = { Text("动态色彩 (Material You)") },
                supportingContent = { Text("根据壁纸颜色自动提取主题色") },
                trailingContent = {
                    Switch(
                        checked = selectedColorType is ColorType.Dynamic,
                        onCheckedChange = { isChecked ->
                            onSelect(if (isChecked) ColorType.Dynamic else ColorType.Custom(colorPresets.first()), darkThemeStyle)
                        }
                    )
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            if (selectedColorType is ColorType.Custom) {
                Text(
                    text = "选择种子颜色",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )


                FlowRow(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),

                    ) {
                    colorPresets.forEach { color ->
                        ColorItem(
                            color = color,
                            isSelected = selectedColorType.color == color,
                            onClick = {
                                onSelect(ColorType.Custom(color), darkThemeStyle)
                            }
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp))

            Text(
                text = "界面装饰",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )

            ListItem(
                headlineContent = { Text("自定义背景图") },
                supportingContent = {
                    Text(if (currentBackgroundPath != null) "已设置自定义背景" else "未设置背景图")
                },
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentBackgroundPath != null) {
                            AsyncImage(
                                model = File(currentBackgroundPath),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Default.Image, contentDescription = null)
                        }
                    }
                },
                trailingContent = {
                    Row {
                        if (currentBackgroundPath != null) {
                            IconButton(onClick = onClearBackground) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "清除背景", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        IconButton(onClick = {
                            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "更换背景")
                        }
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clickable {
                        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
            )

            if (currentBackgroundPath != null) {
                ListItem(
                    headlineContent = { Text("背景不透明度") },
                    supportingContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrightnessLow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Slider(
                                value = sliderValue,
                                onValueChange = {
                                    sliderValue = it
                                },
                                onValueChangeFinished = {
                                    onAlphaChange(sliderValue)
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.BrightnessHigh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ThemeStyleSelector(
    selectedStyle: DarkThemeStyle,
    onStyleSelected: (DarkThemeStyle) -> Unit
) {
    val options = listOf(
        DarkThemeStyle.Light to "浅色",
        DarkThemeStyle.Dark to "深色",
        DarkThemeStyle.Auto to "跟随系统"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (style, label) ->
            FilterChip(
                selected = selectedStyle == style,
                onClick = { onStyleSelected(style) },
                label = { Text(label) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ColorItem(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(64.dp)
            .padding(4.dp)
            .aspectRatio(1f) // 保持正方形，方便对齐
            .then(
                if (isSelected) Modifier.border(
                    3.dp, // 增加粗度使其在页面中更明显
                    MaterialTheme.colorScheme.primary,
                    CircleShape
                ) else Modifier
            )
            .padding(4.dp) // 边框与内容间的间距
            .clip(CircleShape)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = if (isDarkColor(color)) Color.White else Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun isDarkColor(color: Color): Boolean {
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return luminance < 0.5
}