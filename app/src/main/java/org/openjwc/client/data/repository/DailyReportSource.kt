package org.openjwc.client.data.repository

/** Agent 读取本地日报所需的最小投影。 */
interface DailyReportSource {
    /** 已完成日报的正文；该日没有或尚未完成时返回 null。 */
    suspend fun completedReport(day: String): String?
}
