package org.openjwc.client.data.course

import android.content.Context
import org.openjwc.client.data.db.AppDatabase
import org.openjwc.client.data.models.Course
import org.openjwc.client.data.models.Period
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * 当前活跃课表的一份只读快照，供课程提醒与桌面小组件在应用进程之外使用。
 */
data class CourseSnapshot(
    val tableId: Long,
    val tableName: String,
    val startDate: LocalDate,
    val totalWeeks: Int,
    val periods: List<Period>,
    val courses: List<Course>
) {
    fun coursesOn(day: DayOfWeek, week: Int?): List<Course> {
        if (week == null) return emptyList()
        return courses
            .filter { it.dayOfWeek == day && it.weekRule.contains(week) }
            .sortedBy { it.startPeriod }
    }

    fun endPeriodOf(course: Course): Int = course.startPeriod + course.duration - 1
}

object CourseSnapshotLoader {
    suspend fun loadCurrent(context: Context): CourseSnapshot? {
        val database = AppDatabase.getDatabase(context.applicationContext)
        val table = database.tableDao().getCurrentTableSync() ?: return null
        val courses = database.courseDao().getCoursesByTableIdSync(table.id)
        if (courses.isEmpty()) return null
        val config = table.semesterConfig
        return CourseSnapshot(
            tableId = table.id,
            tableName = table.tableName,
            startDate = config.startDate,
            totalWeeks = config.weeks,
            periods = config.periods,
            courses = courses
        )
    }
}

object CourseTimeUtils {
    fun normalizeWeekStart(date: LocalDate): LocalDate = date.with(DayOfWeek.MONDAY)

    /** 与 [org.openjwc.client.data.models.SemesterConfig.calculateCurrentWeek] 保持一致的周次计算。 */
    fun weekForDate(startDate: LocalDate, totalWeeks: Int, date: LocalDate): Int? {
        if (totalWeeks <= 0) return null
        val startMonday = normalizeWeekStart(startDate)
        val days = ChronoUnit.DAYS.between(startMonday, date)
        if (days < 0) return null
        val week = (days / 7).toInt() + 1
        return week.takeIf { it in 1..totalWeeks }
    }

    fun dateForWeekDay(startDate: LocalDate, week: Int, day: DayOfWeek): LocalDate =
        normalizeWeekStart(startDate).plusDays(((week - 1) * 7 + (day.value - 1)).toLong())

    fun millisForWeekDay(
        startDate: LocalDate,
        week: Int,
        day: DayOfWeek,
        minuteOfDay: Int,
        zone: ZoneId = ZoneId.systemDefault()
    ): Long = dateForWeekDay(startDate, week, day)
        .atStartOfDay(zone)
        .plusMinutes(minuteOfDay.toLong())
        .toInstant()
        .toEpochMilli()

    fun periodStart(periods: List<Period>, periodIndex: Int): LocalTime? =
        periods.firstOrNull { it.index == periodIndex }?.start

    fun periodEnd(periods: List<Period>, periodIndex: Int): LocalTime? =
        periods.firstOrNull { it.index == periodIndex }?.end

    fun minuteOfDay(time: LocalTime): Int = time.hour * 60 + time.minute

    fun periodStartMinute(periods: List<Period>, periodIndex: Int): Int? =
        periodStart(periods, periodIndex)?.let(::minuteOfDay)

    fun periodEndMinute(periods: List<Period>, periodIndex: Int): Int? =
        periodEnd(periods, periodIndex)?.let(::minuteOfDay)

    fun periodRangeText(periods: List<Period>, startPeriod: Int, endPeriod: Int): String? {
        val start = periodStart(periods, startPeriod) ?: return null
        val end = periodEnd(periods, endPeriod) ?: return null
        return "${formatTime(start)}-${formatTime(end)}"
    }

    fun formatTime(time: LocalTime): String =
        "%02d:%02d".format(time.hour, time.minute)
}
