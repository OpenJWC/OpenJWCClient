package org.openjwc.client.ui.me.settings.general

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.openjwc.client.ui.theme.ColorType
import org.openjwc.client.ui.theme.DarkThemeStyle
import org.openjwc.client.ui.theme.SeedDefault
import org.openjwc.client.ui.theme.seedColors

@Preview(widthDp = 300, heightDp = 500)
@Composable
fun TestThemeScreen() {
    ThemeScreen(
        onSelect = {
            _, _ ->
        },
        navController = NavController(LocalContext.current),
        colorPresets = seedColors,
        initialColorType = ColorType.Custom(SeedDefault),
        initialThemeStyle = DarkThemeStyle.Auto
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(
    navController: NavController,
    onSelect: (ColorType, DarkThemeStyle) -> Unit,
    colorPresets: List<Color>,
    initialColorType: ColorType = ColorType.Dynamic,
    initialThemeStyle: DarkThemeStyle = DarkThemeStyle.Auto
) {
    // 状态管理
    var selectedColorType by remember{ mutableStateOf(initialColorType) }
    var darkThemeStyle by remember{ mutableStateOf(initialThemeStyle) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("个性化主题") },
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                /*actions = {
                    TextButton(onClick = { onConfirm(selectedColorType, darkThemeStyle) }) {
                        Text("保存")
                    }
                }*/
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
                    darkThemeStyle = it
                    onSelect(selectedColorType, darkThemeStyle)
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
                            selectedColorType = if (isChecked) ColorType.Dynamic else ColorType.Custom(colorPresets.first())
                            onSelect(selectedColorType, darkThemeStyle)
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
                            isSelected = (selectedColorType as? ColorType.Custom)?.color == color,
                            onClick = { selectedColorType = ColorType.Custom(color)
                                onSelect(selectedColorType, darkThemeStyle)
                            }
                        )
                    }
                }
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