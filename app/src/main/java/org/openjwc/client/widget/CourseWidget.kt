package org.openjwc.client.widget

import android.content.Context
import android.content.res.Configuration
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import org.openjwc.client.R
import org.openjwc.client.widget.ui.CoursesWidgetContent
import org.openjwc.client.widget.ui.widgetColorProviders
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

class CourseWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = WidgetModels.computeWidgetDisplayState(context)
        state.nextRefreshAtMillis?.let { WidgetUpdateScheduler.scheduleRefresh(context, it) }
        val backgroundPath = WidgetSettingsManager.getBackgroundImagePath(context)
        val opacity = WidgetSettingsManager.getBackgroundOpacity(context)
        val dayName = DayOfWeek.of(state.dayOfWeek.coerceIn(1, 7))
            .getDisplayName(TextStyle.FULL, Locale.getDefault())
        val isDark = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val colors = widgetColorProviders(context, isDark)

        provideContent {
            CoursesWidgetContent(
                entries = state.entries,
                backgroundImagePath = backgroundPath,
                opacity = opacity,
                isDark = isDark,
                colors = colors,
                headerTitle = context.getString(
                    if (state.isTomorrow) R.string.widget_tomorrow_format else R.string.widget_today_format,
                    dayName
                ),
                weekLabel = state.weekNumber?.let { context.getString(R.string.current_week, it) }.orEmpty(),
                emptyText = context.getString(
                    if (state.isDayComplete) R.string.widget_courses_completed else R.string.widget_no_courses_today
                ),
                sectionLabel = { start, end ->
                    context.getString(R.string.widget_section_range, start, end)
                }
            )
        }
    }
}

class CourseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CourseWidget()
}
