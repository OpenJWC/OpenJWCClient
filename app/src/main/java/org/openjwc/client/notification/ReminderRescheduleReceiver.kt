package org.openjwc.client.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.openjwc.client.widget.WidgetDataManager

/**
 * 开机、应用升级或系统时间变更后重新排程课程提醒，并刷新桌面小组件。
 */
class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderBootstrapper.rescheduleCurrentTimetable(context.applicationContext)
                WidgetDataManager.refreshWidget(context.applicationContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
