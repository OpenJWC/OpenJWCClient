package org.openjwc.client.notification

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.openjwc.client.data.course.CourseSnapshotLoader
import org.openjwc.client.data.datastore.SettingsDataSource

object ReminderBootstrapper {
    suspend fun rescheduleCurrentTimetable(context: Context) = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val scheduler = CourseReminderScheduler(appContext)
        val enabled = SettingsDataSource(appContext).userSettings.first().courseReminderEnabled
        if (!enabled) {
            scheduler.clearAllScheduledReminders()
            return@withContext
        }
        scheduler.scheduleUpcomingReminders(CourseSnapshotLoader.loadCurrent(appContext))
    }
}
