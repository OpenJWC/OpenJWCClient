package org.openjwc.client.data.repository

/** Agent 读取课表所需的最小投影。 */
data class TimetableCourse(
    val name: String,
    val teacher: String,
    val location: String,
    /** 1=周一 … 7=周日 */
    val dayOfWeek: Int,
    val startPeriod: Int,
    val duration: Int,
    /** 上课周次（学期内第几周）。 */
    val weeks: Set<Int>,
    val note: String,
)

data class TimetableSnapshot(
    val id: Long,
    val name: String,
    /** `yyyy-MM-dd` */
    val startDate: String,
    val totalWeeks: Int,
    /** 当前是第几周；不在学期内为 null。 */
    val currentWeek: Int?,
    /** 是否是当前正在使用的那张课表。 */
    val isCurrent: Boolean,
    val courses: List<TimetableCourse>,
)

/**
 * Agent 只依赖的课表读取能力（[CourseRepository] 实现它）。
 * 课表属于用户隐私数据，只在本机读取，不会随资讯一起上传。
 */
interface TimetableSource {
    /** 当前正在使用的课表。 */
    suspend fun currentTimetable(): TimetableSnapshot?

    /** 本机保存的全部课表（可能有多张，例如不同学期）。 */
    suspend fun timetables(): List<TimetableSnapshot>
}
