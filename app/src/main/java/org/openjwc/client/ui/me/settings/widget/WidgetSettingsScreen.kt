package org.openjwc.client.ui.me.settings.widget

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Image
import androidx.compose.material.icons.twotone.Opacity
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openjwc.client.R
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.ui.component.settings.AppBackButton
import org.openjwc.client.ui.component.settings.SegmentedColumn
import org.openjwc.client.ui.component.settings.SettingsJumpPageWidget
import org.openjwc.client.widget.WidgetDataManager
import org.openjwc.client.widget.WidgetSettingsManager
import java.io.File
import java.io.FileOutputStream
import java.time.DayOfWeek
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private const val WIDGET_BG_FILE = "widget_background.jpg"

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WidgetSettingsScreen(navigator: Navigator) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.widget_settings)) },
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
        WidgetSettingsContent(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding)
        )
    }
}

@Composable
fun WidgetSettingsContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    var backgroundPath by remember {
        mutableStateOf(if (isPreview) null else WidgetSettingsManager.getBackgroundImagePath(context))
    }
    var opacity by remember {
        mutableFloatStateOf(
            if (isPreview) 128 / 255f
            else WidgetSettingsManager.getBackgroundOpacity(context) / 255f
        )
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = saveBackgroundImage(context, it)
            if (savedPath != null) {
                backgroundPath = savedPath
                WidgetSettingsManager.setBackgroundImagePath(context, savedPath)
                WidgetDataManager.refreshWidget(context)
            }
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        WidgetPreview(
            backgroundPath = backgroundPath,
            opacity = opacity,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
        )

        SegmentedColumn(title = stringResource(R.string.widget_background)) {
            item {
                SettingsJumpPageWidget(
                    icon = Icons.TwoTone.Image,
                    title = stringResource(R.string.widget_pick_image),
                    description = stringResource(R.string.widget_background_desc),
                    onClick = { imagePickerLauncher.launch("image/*") }
                )
            }
            if (backgroundPath != null) {
                item {
                    SettingsJumpPageWidget(
                        icon = Icons.TwoTone.Delete,
                        title = stringResource(R.string.widget_remove_background),
                        isError = true,
                        onClick = {
                            deleteBackgroundImage(context)
                            backgroundPath = null
                            WidgetSettingsManager.setBackgroundImagePath(context, null)
                            WidgetDataManager.refreshWidget(context)
                        }
                    )
                }
            }
        }

        SegmentedColumn(title = stringResource(R.string.widget_opacity)) {
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.widget_opacity),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${(opacity * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Slider(
                        value = opacity,
                        onValueChange = { newValue ->
                            opacity = newValue
                            val alphaInt = (newValue * 255).toInt().coerceIn(0, 255)
                            WidgetSettingsManager.setBackgroundOpacity(context, alphaInt)
                        },
                        onValueChangeFinished = {
                            WidgetDataManager.refreshWidget(context)
                        }
                    )
                    Text(
                        text = stringResource(R.string.widget_opacity_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetPreview(
    backgroundPath: String?,
    opacity: Float,
    modifier: Modifier = Modifier
) {
    val backgroundBitmap = remember(backgroundPath) {
        backgroundPath?.let { path ->
            try {
                BitmapFactory.decodeFile(path)
            } catch (_: Exception) {
                null
            }
        }
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        if (backgroundBitmap != null) {
            Image(
                bitmap = backgroundBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 1f - opacity)
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.icon05),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        R.string.widget_today_format,
                        DayOfWeek.THURSDAY.getDisplayName(JavaTextStyle.FULL, Locale.getDefault())
                    ),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.current_week, 13),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
            PreviewCourseRow(
                startTime = "08:00",
                endTime = "09:35",
                name = "大学物理",
                metadata = "${stringResource(R.string.widget_section_range, 1, 2)} | N2-304",
                color = Color(0xFF1565C0),
                countdown = stringResource(R.string.widget_countdown_minutes, 25)
            )
            PreviewCourseRow(
                startTime = "09:50",
                endTime = "11:25",
                name = "线性代数",
                metadata = "${stringResource(R.string.widget_section_range, 3, 4)} | N2-212",
                color = Color(0xFF00695C)
            )
        }
    }
}

@Composable
private fun PreviewCourseRow(
    startTime: String,
    endTime: String,
    name: String,
    metadata: String,
    color: Color,
    countdown: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(50.dp)) {
            Text(
                text = startTime,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = endTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(5.dp)
                .height(38.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = metadata,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        if (countdown != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = countdown,
                modifier = Modifier.width(66.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

private fun saveBackgroundImage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.filesDir, WIDGET_BG_FILE)
        inputStream.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (_: Exception) {
        null
    }
}

private fun deleteBackgroundImage(context: Context) {
    try {
        File(context.filesDir, WIDGET_BG_FILE).delete()
    } catch (_: Exception) {
    }
}
