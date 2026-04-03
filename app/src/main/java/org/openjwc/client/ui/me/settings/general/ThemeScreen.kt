package org.openjwc.client.ui.me.settings.general

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ThemeStyleSelectorCard(darkThemeStyle, onSelect, selectedColorType)

//            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp))

            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    ListItem(
                        headlineContent = {
                            Text(
                                "颜色主题",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        supportingContent = { Text("基于 Material You 的色彩方案") }
                    )

                    ListItem(
                        headlineContent = { Text("动态色彩") },
                        supportingContent = { Text("根据壁纸颜色自动提取 (仅支持 Android 12 及以上) ") },
                        trailingContent = {
                            Switch(
                                checked = selectedColorType is ColorType.Dynamic,
                                onCheckedChange = { isChecked ->
                                    onSelect(
                                        if (isChecked) ColorType.Dynamic else ColorType.Custom(
                                            colorPresets.first()
                                        ), darkThemeStyle
                                    )
                                }
                            )
                        }
                    )

                    AnimatedVisibility(
                        visible = selectedColorType is ColorType.Custom,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column {
                            Text(
                                text = "选择种子颜色",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            FlowRow(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                colorPresets.forEach { color ->
                                    ColorItem(
                                        color = color,
                                        isSelected = (selectedColorType as? ColorType.Custom)?.color == color,
                                        onClick = {
                                            onSelect(
                                                ColorType.Custom(color),
                                                darkThemeStyle
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        "界面装饰",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
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
                                        Icon(
                                            Icons.Default.DeleteSweep,
                                            contentDescription = "清除背景",
                                            tint = MaterialTheme.colorScheme.error
                                        )
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
    }
}

/*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeStyleSelector(
    selectedStyle: DarkThemeStyle,
    onStyleSelected: (DarkThemeStyle) -> Unit
) {
    val options = listOf(
        Triple(DarkThemeStyle.Light, "浅色", Icons.Outlined.LightMode),
        Triple(DarkThemeStyle.Dark, "深色", Icons.Outlined.DarkMode),
        Triple(DarkThemeStyle.Auto, "跟随系统", Icons.Outlined.Contrast)
    )
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        options.forEachIndexed { index, (style, label, icon) ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size
                ),
                onClick = { onStyleSelected(style) },
                selected = selectedStyle == style,
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            )
        }
    }
}
*/

@Composable
fun ThemeStyleSelector(
    selectedStyle: DarkThemeStyle,
    onStyleSelected: (DarkThemeStyle) -> Unit
) {
    // 容器使用 Surface Color，提升层级感
    Surface(
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(24.dp), // MD3 标志性的大圆角
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "外观模式",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                val options = listOf(
                    DarkThemeStyle.Light to "浅色" to Icons.Default.LightMode,
                    DarkThemeStyle.Dark to "深色" to Icons.Default.DarkMode,
                    DarkThemeStyle.Auto to "系统" to Icons.Default.AutoMode
                )

                options.forEachIndexed { index, (pair, icon) ->
                    val (style, label) = pair
                    val selected = selectedStyle == style

                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size
                        ),
                        onClick = { onStyleSelected(style) },
                        selected = selected,
                        label = { Text(label) },
                        icon = {
                            // 使用动画切换图标
                            Crossfade(targetState = selected) { isSelected ->
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.Check else icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        // 自定义颜色以符合 MD3e 的高对比度需求
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            inactiveContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
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
            .aspectRatio(1f)
            .then(
                if (isSelected) Modifier.border(
                    3.dp,
                    MaterialTheme.colorScheme.primary,
                    CircleShape
                ) else Modifier
            )
            .padding(4.dp)
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

@Composable
fun ThemeStyleSelectorCard(
    selectedStyle: DarkThemeStyle,
    onSelect: (ColorType, DarkThemeStyle) -> Unit,
    selectedColorType: ColorType
) {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "显示模式",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(
                    Triple(DarkThemeStyle.Light, "浅色", Icons.Default.LightMode),
                    Triple(DarkThemeStyle.Dark, "深色", Icons.Default.DarkMode),
                    Triple(DarkThemeStyle.Auto, "系统", Icons.Default.AutoMode)
                )
                options.forEachIndexed { index, (style, label, icon) ->
                    val isSelected = selectedStyle == style
                    SegmentedButton(
                        selected = isSelected,
                        onClick = { onSelect(selectedColorType, style) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size
                        ),
                        icon = {
                            Crossfade(targetState = isSelected) { target ->
                                Icon(
                                    imageVector = if (target) Icons.Default.Check else icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        label = { Text(label) }
                    )
                }
            }
        }
    }
}

private fun isDarkColor(color: Color): Boolean {
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return luminance < 0.5
}