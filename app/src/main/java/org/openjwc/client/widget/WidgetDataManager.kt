package org.openjwc.client.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.openjwc.client.log.Logger

object WidgetDataManager {
    private const val TAG = "WidgetDataManager"

    fun refreshWidget(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                refreshWidgetAndWait(context)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 定时闹钟与宿主更新会重试刷新。
                Logger.e(TAG, "Widget refresh failed", e)
            }
        }
    }

    suspend fun refreshWidgetAndWait(context: Context) {
        val state = WidgetModels.computeWidgetDisplayState(context)
        Logger.d(
            TAG,
            "Widget state: entries=${state.entries.size}, day=${state.dayOfWeek}, " +
                "week=${state.weekNumber}, tomorrow=${state.isTomorrow}"
        )
        state.nextRefreshAtMillis?.let { triggerAtMillis ->
            WidgetUpdateScheduler.scheduleRefresh(context, triggerAtMillis)
        }
        CourseWidget().updateAll(context)
    }
}
