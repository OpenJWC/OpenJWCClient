package org.openjwc.client.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.openjwc.client.data.models.Course
import java.time.DayOfWeek

/**
 * 课程数据访问对象 (DAO)
 */
@Dao
interface CourseDao {
    /**
     * 获取所有课程
     */
    @Query("SELECT * FROM courses")
    fun getAllCourses(): Flow<List<Course>>

    /**
     * 根据课表 ID 观察课程列表
     */
    @Query("SELECT * FROM courses WHERE tableId = :tableId")
    fun getCoursesByTableId(tableId: Long): Flow<List<Course>>

    /**
     * 根据课表 ID 和星期观察课程，按起始节次升序排序
     */
    @Query("SELECT * FROM courses WHERE tableId = :tableId AND dayOfWeek = :dayOfWeek ORDER BY startPeriod ASC")
    fun getCoursesByDay(tableId: Long, dayOfWeek: DayOfWeek): Flow<List<Course>>

    /**
     * 根据课表 ID 同步获取课程列表
     */
    @Query("SELECT * FROM courses WHERE tableId = :tableId")
    suspend fun getCoursesByTableIdSync(tableId: Long): List<Course>

    /**
     * 根据课表 ID 和星期同步获取课程列表，按起始节次升序排序
     */
    @Query("SELECT * FROM courses WHERE tableId = :tableId AND dayOfWeek = :dayOfWeek ORDER BY startPeriod ASC")
    suspend fun getCoursesByDaySync(tableId: Long, dayOfWeek: DayOfWeek): List<Course>


    /**
     * 插入单条课程，若冲突则覆盖
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course)

    /**
     * 批量插入课程，若冲突则覆盖
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<Course>)


    /**
     * 根据课程 ID 删除
     */
    @Query("DELETE FROM courses WHERE id = :courseId")
    suspend fun deleteById(courseId: Long)

    /**
     * 根据课表 ID 删除该表下所有课程
     */
    @Query("DELETE FROM courses WHERE tableId = :tableId")
    suspend fun deleteByTableId(tableId: Long)
}