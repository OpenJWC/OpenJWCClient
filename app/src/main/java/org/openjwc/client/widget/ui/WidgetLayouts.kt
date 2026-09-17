package org.openjwc.client.widget.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProviders
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import org.openjwc.client.MainActivity
import org.openjwc.client.R
import org.openjwc.client.notification.NotificationNavigation
import org.openjwc.client.ui.theme.CourseColorGenerator
import org.openjwc.client.widget.WidgetCourseEntry
import org.openjwc.client.widget.remainingCourseMinutes

@Composable
fun CoursesWidgetContent(
    entries: List<WidgetCourseEntry>,
    backgroundImagePath: String?,
    opacity: Int,
    isDark: Boolean,
    colors: ColorProviders,
    headerTitle: String,
    weekLabel: String,
    emptyText: String,
    sectionLabel: (Int, Int) -> String
) {
    val size = LocalSize.current
    val compact = size.height < 166.dp
    val scrimAlpha = (255 - opacity.coerceIn(0, 255)) / 255f
    val backgroundBitmap = backgroundImagePath?.let { path ->
        try {
            BitmapFactory.decodeFile(path)
        } catch (_: Exception) {
            null
        }
    }

    WidgetTheme(colors = colors) {
        val context = LocalContext.current
        val themePrimaryArgb = GlanceTheme.colors.primary.getColor(context).toArgb()
        val scrimColor = GlanceTheme.colors.widgetBackground.getColor(context).copy(alpha = scrimAlpha)
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // 点小组件直达课程表页
            putExtra(NotificationNavigation.EXTRA_DESTINATION, NotificationNavigation.DEST_TIMETABLE)
        }
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(16.dp)
                .clickable(actionStartActivity(launchIntent))
        ) {
            if (backgroundBitmap != null) {
                Image(
                    provider = ImageProvider(backgroundBitmap),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(scrimColor)
                ) {}
            }

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(if (compact) 6.dp else 12.dp)
            ) {
                WidgetHeader(headerTitle = headerTitle, weekLabel = weekLabel)
                Spacer(modifier = GlanceModifier.height(if (compact) 4.dp else 8.dp))

                if (entries.isEmpty()) {
                    EmptyWidgetContent(emptyText)
                } else {
                    entries.forEachIndexed { index, entry ->
                        CourseRow(
                            entry = entry,
                            courseColor = CourseColorGenerator
                                .pair(entry.colorArgb, themePrimaryArgb, isDark)
                                .accent,
                            sectionText = sectionLabel(entry.startSection, entry.endSection),
                            compact = compact
                        )
                        if (index != entries.lastIndex) {
                            Spacer(modifier = GlanceModifier.height(if (compact) 4.dp else 6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetHeader(
    headerTitle: String,
    weekLabel: String
) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(22.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.mipmap.ic_launcher),
                contentDescription = null,
                modifier = GlanceModifier.size(22.dp)
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = headerTitle,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }
        if (weekLabel.isNotEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = weekLabel,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun EmptyWidgetContent(text: String) {
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun CourseRow(
    entry: WidgetCourseEntry,
    courseColor: Color,
    sectionText: String,
    compact: Boolean
) {
    val (startTime, endTime) = splitTimes(entry.timeText)
    val metadata = buildList {
        add(sectionText)
        if (entry.classroom.isNotBlank()) add(entry.classroom)
        if (entry.teacher.isNotBlank()) add(entry.teacher)
    }.joinToString(" | ")

    if (compact) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().height(28.dp)
                .background(GlanceTheme.colors.surfaceVariant).cornerRadius(10.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(GlanceModifier.width(4.dp).height(18.dp).background(courseColor)) {}
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = listOf(entry.timeText, entry.name, entry.classroom)
                    .filter { it.isNotBlank() }.joinToString("  "),
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp),
                maxLines = 1
            )
            CourseCountdown(entry)
        }
        return
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(52.dp)
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(18.dp)
            .padding(start = 10.dp, top = 6.dp, end = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = GlanceModifier.width(50.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (startTime.isNotEmpty()) {
                Text(
                    text = startTime,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Text(
                    text = endTime,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
            } else {
                Text(
                    text = sectionText,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = GlanceModifier.width(8.dp))
        Box(
            modifier = GlanceModifier
                .width(5.dp)
                .height(38.dp)
                .background(courseColor)
                .cornerRadius(3.dp)
        ) {}
        Spacer(modifier = GlanceModifier.width(10.dp))

        Column(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.name,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            Text(
                text = metadata,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
        }
        CourseCountdown(entry)
    }
}

@Composable
private fun CourseCountdown(entry: WidgetCourseEntry) {
    val endMillis = entry.countdownEndMillis ?: return
    val context = LocalContext.current
    val views = RemoteViews(context.packageName, R.layout.widget_countdown).apply {
        setTextViewText(
            R.id.course_countdown,
            context.getString(
                R.string.widget_countdown_minutes,
                remainingCourseMinutes(endMillis, System.currentTimeMillis())
            )
        )
        setTextColor(R.id.course_countdown, GlanceTheme.colors.primary.getColor(context).toArgb())
        setContentDescription(R.id.course_countdown, context.getString(R.string.widget_countdown))
    }
    Spacer(GlanceModifier.width(6.dp))
    AndroidRemoteViews(views, modifier = GlanceModifier.width(66.dp).height(24.dp))
}

private fun splitTimes(timeText: String): Pair<String, String> {
    val parts = timeText.split("-", limit = 2)
    return if (parts.size == 2 && parts.all { ':' in it }) {
        parts[0] to parts[1]
    } else {
        "" to ""
    }
}
