package org.openjwc.client.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.openjwc.client.data.dao.CourseDao
import org.openjwc.client.data.dao.TableDao
import org.openjwc.client.data.models.Course
import org.openjwc.client.data.models.Period
import org.openjwc.client.data.models.SemesterConfig
import org.openjwc.client.data.models.TableMetadata
import org.openjwc.client.data.parser.TableParserUtils
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.Date
import java.util.Locale

/**
 * 课表与课程数据仓库
 * 统一管理本地数据库中的课表配置及课程内容
 */
class CourseRepository(
    private val courseDao: CourseDao,
    private val tableDao: TableDao
) {
    private val TAG = "CourseRepository"
    /**
     * 观察所有课表列表
     */
    val allTables: Flow<List<TableMetadata>> = tableDao.getAllTables()

    /**
     * 观察当前正在使用的课表
     */
    val currentTable: Flow<TableMetadata?> = tableDao.observeCurrentTable()

    /**
     * 获取指定 ID 的课表快照
     */
    suspend fun getTableById(tableId: Long): TableMetadata? = tableDao.getTableById(tableId)

    /**
     * 插入或更新课表配置
     */
    suspend fun saveTable(table: TableMetadata): Long = tableDao.insertTable(table)

    /**
     * 更新课表信息
     */
    suspend fun updateTable(table: TableMetadata) = tableDao.updateTable(table)

    /**
     * 删除课表及其下所有课程
     */
    suspend fun deleteTable(tableId: Long) = tableDao.deleteTableById(tableId)

    /**
     * 切换当前活跃课表
     */
    suspend fun setCurrentTable(tableId: Long) = tableDao.setCurrentTable(tableId)

    suspend fun parseExternalJson(jsonString: String): Pair<TableMetadata, List<Course>> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting JSON parse process...")
        val root = JSONObject(jsonString)
        val termName = root.optString("termName", "导入课表")
        val rowsArray = root.optJSONArray("rows") ?: throw Exception("解析错误：未找到课程数据(rows)")

        if (rowsArray.length() == 0) throw Exception("解析错误：课表内容为空")

        val parseResult = TableParserUtils.parseCoursesFromJsonArray(rowsArray.toString(), 0L)
        Log.d(TAG, "Parse successful: Found ${parseResult.courses.size} courses")

        val defaultConfig = SemesterConfig.default()

        // 自动识别可见天数：若包含周末课程则显示全周
        val inferredVisibleDays = if (parseResult.hasWeekend) {
            DayOfWeek.entries.toSet()
        } else {
            DayOfWeek.entries.filter { it.value <= 5 }.toSet()
        }

        // 自动补全节次：若导入课程节次超过默认 13 节则自动扩展
        val finalPeriods = if (parseResult.maxPeriod > defaultConfig.periods.size) {
            defaultConfig.periods.toMutableList().apply {
                for (i in (size + 1)..parseResult.maxPeriod) {
                    add(Period(i, LocalTime.of(21, 30), LocalTime.of(22, 15)))
                }
            }
        } else defaultConfig.periods

        val timeStamp = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date())

        val metadata = TableMetadata(
            tableName = "$termName ($timeStamp)",
            semesterConfig = defaultConfig.copy(
                weeks = parseResult.totalWeeks,
                visibleDays = inferredVisibleDays,
                periods = finalPeriods
            ),
            isCurrent = true
        )

        Pair(metadata, parseResult.courses)
    }

    // --- 课程内容 (Course Content) ---

    /**
     * 自动观察当前活跃课表下的所有课程
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentCourses: Flow<List<Course>> = tableDao.observeCurrentTable()
        .flatMapLatest { table ->
            if (table == null) flowOf(emptyList())
            else courseDao.getCoursesByTableId(table.id)
        }

    /**
     * 观察特定课表下的课程
     */
    fun getCoursesByTableId(tableId: Long): Flow<List<Course>> =
        courseDao.getCoursesByTableId(tableId)

    /**
     * 观察特定课表在特定星期的课程
     */
    fun getCoursesByDay(tableId: Long, dayOfWeek: DayOfWeek): Flow<List<Course>> =
        courseDao.getCoursesByDay(tableId, dayOfWeek)

    /**
     * 保存或更新课程
     */
    suspend fun saveCourse(course: Course) = courseDao.insertCourse(course)

    /**
     * 批量保存课程
     */
    suspend fun saveCourses(courses: List<Course>) = courseDao.insertCourses(courses)

    /**
     * 删除单条课程
     */
    suspend fun deleteCourse(courseId: Long) = courseDao.deleteById(courseId)

    /**
     * 清空特定课表下的所有课程
     */
    suspend fun clearCoursesInTable(tableId: Long) = courseDao.deleteByTableId(tableId)

    /**
     * 清理无用的空课表
     */
    suspend fun cleanEmptyTables() = tableDao.deleteEmptyTables()
}
