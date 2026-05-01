package org.openjwc.client.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import org.openjwc.client.data.dao.CourseDao
import org.openjwc.client.data.dao.TableDao
import org.openjwc.client.data.models.Course
import org.openjwc.client.data.models.TableMetadata
import java.time.DayOfWeek

/**
 * 课表与课程数据仓库
 * 统一管理本地数据库中的课表配置及课程内容
 */
class CourseRepository(
    private val courseDao: CourseDao,
    private val tableDao: TableDao
) {


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
