package org.openjwc.client.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 资讯数据源（脚本）。内置脚本 origin = "builtin"，侧载脚本 origin = "sideload"。
 * `subscribed` 同时控制「是否抓取」和「是否出现在资讯流」。
 */
@Entity(tableName = "notice_sources")
data class SourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val version: String,
    val origin: String,
    /** 侧载脚本的落盘文件名（filesDir/sources/ 下）；内置脚本为 null，从 assets 读取。 */
    val scriptFile: String? = null,
    val domains: List<String> = emptyList(),
    val labels: List<String> = emptyList(),
    val scheduleMinutes: Int = 360,
    val subscribed: Boolean = false,
    val lastRunAt: Long? = null,
    /** 最近一次运行**新增**的条数（不是本地总量）。 */
    val lastCount: Int = 0,
    /** 最近一次运行的结果：致命错误，或「首行摘要 + 逐条警告」的多行文本（属性页可点开查看）。 */
    val lastError: String? = null,
) {
    val isBuiltIn: Boolean get() = origin == ORIGIN_BUILTIN

    companion object {
        const val ORIGIN_BUILTIN = "builtin"
        const val ORIGIN_SIDELOAD = "sideload"
    }
}
