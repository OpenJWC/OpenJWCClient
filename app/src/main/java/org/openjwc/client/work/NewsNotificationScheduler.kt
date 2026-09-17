package org.openjwc.client.work

import android.content.Context

/**
 * 兼容旧调用点的门面：周期抓取的实际排程在 [SourceCrawlScheduler]，
 * 是否发系统通知由 `newsNotificationEnabled` 在抓取时决定。
 */
object NewsNotificationScheduler {

    suspend fun sync(context: Context) = SourceCrawlScheduler.sync(context)

    /** 立即抓取一次（用于开启通知开关时给出即时反馈）。 */
    fun runOnce(context: Context) = SourceCrawlScheduler.runOnce(context)
}
