package org.openjwc.client.widget

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import org.openjwc.client.data.course.CourseSnapshot
import org.openjwc.client.data.course.CourseSnapshotLoader
import org.openjwc.client.data.course.CourseTimeUtils
import org.openjwc.client.data.models.Course
import java.time.Instant
import java.time.ZoneId
import kotlin.math.max

data class WidgetCourseEntry(
    val name: String,
    val classroom: String,
    val teacher: String,
    val colorArgb: Int,
    val startSection: Int,
    val endSection: Int,
    val timeText: String,
    val countdownEndMillis: Long? = null
)

data class WidgetDisplayState(
    val entries: List<WidgetCourseEntry> = emptyList(),
    val dayOfWeek: Int = 1,
    val weekNumber: Int? = null,
    val isTomorrow: Boolean = false,
    val isDayComplete: Boolean = false,
    val nextRefreshAtMillis: Long? = null
)

object WidgetModels {
    suspend fun computeWidgetDisplayState(
        context: Context,
        nowMillis: Long = System.currentTimeMillis()
    ): WidgetDisplayState {
        val snapshot = CourseSnapshotLoader.loadCurrent(context)
            ?: return emptyState(nowMillis)
        return buildWidgetDisplayState(snapshot, nowMillis)
    }

    private fun emptyState(nowMillis: Long): WidgetDisplayState {
        val zone = ZoneId.systemDefault()
        return WidgetDisplayState(
            dayOfWeek = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate().dayOfWeek.value,
            nextRefreshAtMillis = nextMidnightRefresh(nowMillis, zone)
        )
    }
}

internal fun buildWidgetDisplayState(
    snapshot: CourseSnapshot,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault()
): WidgetDisplayState {
    val todayDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    val todayWeek = CourseTimeUtils.weekForDate(snapshot.startDate, snapshot.totalWeeks, todayDate)
    val todayDay = todayDate.dayOfWeek
    val todayCourses = snapshot.coursesOn(todayDay, todayWeek)

    val latestEndMinute = todayCourses.mapNotNull { course ->
        CourseTimeUtils.periodEndMinute(snapshot.periods, snapshot.endPeriodOf(course))
    }.maxOrNull()
    val forecastMinute = max(FORECAST_START_MINUTE, latestEndMinute ?: 0)
    val currentMinute = minuteOfDay(nowMillis, zone)
    val shouldForecastTomorrow = currentMinute >= forecastMinute

    if (shouldForecastTomorrow) {
        val tomorrowDate = todayDate.plusDays(1)
        val tomorrowWeek = CourseTimeUtils.weekForDate(snapshot.startDate, snapshot.totalWeeks, tomorrowDate)
        val tomorrowDay = tomorrowDate.dayOfWeek
        val tomorrowEntries = courseEntries(
            snapshot = snapshot,
            courses = snapshot.coursesOn(tomorrowDay, tomorrowWeek)
        ).take(MAX_COURSES)

        return WidgetDisplayState(
            entries = tomorrowEntries,
            dayOfWeek = tomorrowDay.value,
            weekNumber = tomorrowWeek,
            isTomorrow = tomorrowEntries.isNotEmpty(),
            isDayComplete = tomorrowEntries.isEmpty(),
            nextRefreshAtMillis = nextMidnightRefresh(nowMillis, zone)
        )
    }

    val latestStartedMinute = todayCourses.mapNotNull { course ->
        CourseTimeUtils.periodStartMinute(snapshot.periods, course.startPeriod)
    }.filter { it <= currentMinute }.maxOrNull()

    val remainingCourses = todayCourses.filter { course ->
        val endMinute = CourseTimeUtils.periodEndMinute(snapshot.periods, snapshot.endPeriodOf(course))
        // 刚结束的课程在课间保留倒计时 0，直到下一节课开始才被替换。
        endMinute == null || endMinute > currentMinute ||
            CourseTimeUtils.periodStartMinute(snapshot.periods, course.startPeriod) == latestStartedMinute
    }

    val nextBoundaryMinute = remainingCourses.flatMap { course ->
        listOfNotNull(
            CourseTimeUtils.periodStartMinute(snapshot.periods, course.startPeriod),
            CourseTimeUtils.periodEndMinute(snapshot.periods, snapshot.endPeriodOf(course))
        )
    }.filter { it > currentMinute }.minOrNull()

    val entries = courseEntries(snapshot, remainingCourses, nowMillis, zone).take(MAX_COURSES)
    val nextBoundaryMillis = atMinuteOfLocalDay(nowMillis, nextBoundaryMinute ?: forecastMinute, zone)
    val nextRefreshMillis = if (entries.any { (it.countdownEndMillis ?: 0) > nowMillis }) {
        minOf(nextBoundaryMillis, atMinuteOfLocalDay(nowMillis, currentMinute + 1, zone))
    } else {
        nextBoundaryMillis
    }

    return WidgetDisplayState(
        entries = entries,
        dayOfWeek = todayDay.value,
        weekNumber = todayWeek,
        isDayComplete = todayCourses.isNotEmpty() && remainingCourses.isEmpty(),
        nextRefreshAtMillis = nextRefreshMillis
    )
}

private fun courseEntries(
    snapshot: CourseSnapshot,
    courses: List<Course>,
    nowMillis: Long? = null,
    zone: ZoneId = ZoneId.systemDefault()
): List<WidgetCourseEntry> = courses.map { course ->
    val endPeriod = snapshot.endPeriodOf(course)
    val startTime = CourseTimeUtils.periodStart(snapshot.periods, course.startPeriod)
    val endTime = CourseTimeUtils.periodEnd(snapshot.periods, endPeriod)
    val timeText = if (startTime != null && endTime != null) {
        "${CourseTimeUtils.formatTime(startTime)}-${CourseTimeUtils.formatTime(endTime)}"
    } else {
        "第${course.startPeriod}-${endPeriod}节"
    }
    val countdownEndMillis = if (nowMillis != null && startTime != null && endTime != null) {
        val startMinute = CourseTimeUtils.minuteOfDay(startTime)
        val endMinute = CourseTimeUtils.minuteOfDay(endTime)
        val currentMinute = minuteOfDay(nowMillis, zone)
        if (endMinute > startMinute && currentMinute in startMinute until endMinute) {
            atMinuteOfLocalDay(nowMillis, endMinute, zone)
        } else {
            null
        }
    } else {
        null
    }
    WidgetCourseEntry(
        name = course.name,
        classroom = course.location,
        teacher = course.teacher,
        colorArgb = course.color.toArgb(),
        startSection = course.startPeriod,
        endSection = endPeriod,
        timeText = timeText,
        countdownEndMillis = countdownEndMillis
    )
}

private fun minuteOfDay(timeMillis: Long, zone: ZoneId): Int {
    val time = Instant.ofEpochMilli(timeMillis).atZone(zone).toLocalTime()
    return time.hour * 60 + time.minute
}

private fun atMinuteOfLocalDay(timeMillis: Long, minuteOfDay: Int, zone: ZoneId): Long {
    val date = Instant.ofEpochMilli(timeMillis).atZone(zone).toLocalDate()
    return date.atStartOfDay(zone)
        .plusMinutes(minuteOfDay.toLong())
        .toInstant()
        .toEpochMilli()
}

private fun nextMidnightRefresh(timeMillis: Long, zone: ZoneId): Long {
    val date = Instant.ofEpochMilli(timeMillis).atZone(zone).toLocalDate().plusDays(1)
    return date.atStartOfDay(zone).plusMinutes(5).toInstant().toEpochMilli()
}

/** 向上取整剩余分钟数，且绝不返回负值。 */
internal fun remainingCourseMinutes(endMillis: Long, nowMillis: Long): Long {
    if (nowMillis >= endMillis) return 0
    val remaining = endMillis - nowMillis
    return remaining / 60_000 + if (remaining % 60_000 == 0L) 0 else 1
}

private const val FORECAST_START_MINUTE = 17 * 60
private const val MAX_COURSES = 2
