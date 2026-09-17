package org.openjwc.client.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 日报生成阶段；只有 [COMPLETED] 的正文对外可见。 */
enum class DailyReportStatus(val value: String) {
    RUNNING("running"),
    FAILED("failed"),
    COMPLETED("completed");

    companion object {
        fun from(value: String): DailyReportStatus =
            entries.firstOrNull { it.value == value } ?: FAILED
    }
}

@Entity(tableName = "daily_reports")
data class DailyReportEntity(
    /** `yyyy-MM-dd` */
    @PrimaryKey val day: String,
    val status: String,
    val content: String = "",
    val sourceCount: Int = 0,
    /** 生成失败时的原因（安全摘要），供界面展示与手动重试。 */
    val error: String? = null,
    val updatedAt: Long,
)
