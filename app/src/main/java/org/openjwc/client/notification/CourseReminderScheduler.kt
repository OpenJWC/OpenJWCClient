package org.openjwc.client.notification

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import org.openjwc.client.data.course.CourseSnapshot
import org.openjwc.client.data.course.CourseTimeUtils

/**
 * 使用 [AlarmManager] 为未来一段时间内的每节课安排精确的上课前提醒。
 * 排程结果以 requestCode 集合保存在 SharedPreferences 中，便于全量取消后重排。
 */
class CourseReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun scheduleUpcomingReminders(snapshot: CourseSnapshot?) {
        clearAllScheduledReminders()

        if (snapshot == null) return
        if (snapshot.totalWeeks <= 0 || snapshot.periods.isEmpty()) return
        if (snapshot.courses.isEmpty()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val now = System.currentTimeMillis()
        val horizon = now + SCHEDULE_WINDOW_MILLIS
        val requestCodes = mutableSetOf<Int>()

        for (week in 1..snapshot.totalWeeks) {
            snapshot.courses.forEach { course ->
                if (!course.weekRule.contains(week)) return@forEach

                val startMinute = CourseTimeUtils.periodStartMinute(snapshot.periods, course.startPeriod)
                    ?: return@forEach
                val classStartMillis = CourseTimeUtils.millisForWeekDay(
                    startDate = snapshot.startDate,
                    week = week,
                    day = course.dayOfWeek,
                    minuteOfDay = startMinute
                )
                val reminderMillis = classStartMillis - REMINDER_LEAD_MILLIS

                if (reminderMillis <= now || reminderMillis > horizon) return@forEach

                val endPeriod = snapshot.endPeriodOf(course)
                val timeText = CourseTimeUtils.periodRangeText(
                    snapshot.periods,
                    course.startPeriod,
                    endPeriod
                ).orEmpty()
                val requestCode = buildRequestCode(snapshot.tableId, course.id, week, classStartMillis)

                val reminderIntent = Intent(context, CourseReminderReceiver::class.java).apply {
                    putExtra(CourseReminderContract.EXTRA_NOTIFICATION_ID, requestCode)
                    putExtra(CourseReminderContract.EXTRA_COURSE_NAME, course.name)
                    putExtra(CourseReminderContract.EXTRA_TIME_TEXT, timeText)
                    putExtra(CourseReminderContract.EXTRA_CLASSROOM, course.location)
                    putExtra(CourseReminderContract.EXTRA_TEACHER, course.teacher)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    reminderIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                scheduleExactSafely(reminderMillis, pendingIntent)

                requestCodes.add(requestCode)
            }
        }

        prefs.edit {
            putStringSet(KEY_REQUEST_CODES, requestCodes.map { it.toString() }.toSet())
        }
    }

    fun clearAllScheduledReminders() {
        val requestCodes = prefs.getStringSet(KEY_REQUEST_CODES, emptySet()).orEmpty()
            .mapNotNull { it.toIntOrNull() }

        requestCodes.forEach { requestCode ->
            val intent = Intent(context, CourseReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }

        prefs.edit { remove(KEY_REQUEST_CODES) }
    }

    private fun buildRequestCode(
        tableId: Long,
        courseId: Long,
        week: Int,
        classStartMillis: Long
    ): Int = ("$tableId|$courseId|$week|$classStartMillis".hashCode() and 0x7fffffff)

    private fun scheduleExactSafely(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    companion object {
        private const val PREFS_NAME = "course_reminder_scheduler"
        private const val KEY_REQUEST_CODES = "request_codes"
        private const val REMINDER_LEAD_MILLIS = 10 * 60 * 1000L
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
        private const val SCHEDULE_WINDOW_MILLIS = 21 * DAY_MILLIS
    }
}
