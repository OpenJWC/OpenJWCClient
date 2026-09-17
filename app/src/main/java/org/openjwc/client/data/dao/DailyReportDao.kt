package org.openjwc.client.data.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.openjwc.client.data.models.DailyReportEntity

@Dao
interface DailyReportDao {

    @Query("SELECT * FROM daily_reports WHERE day = :day LIMIT 1")
    suspend fun get(day: String): DailyReportEntity?

    /** 不晚于 [before] 的最近一份已完成日报。 */
    @Query(
        "SELECT * FROM daily_reports WHERE status = 'completed' AND day <= :before " +
            "ORDER BY day DESC LIMIT 1"
    )
    suspend fun latestCompleted(before: String): DailyReportEntity?

    @Query("SELECT * FROM daily_reports ORDER BY day DESC")
    fun observeAll(): Flow<List<DailyReportEntity>>

    /** 保存阶段结果；已完成的日报不会被覆盖。 */
    @Query(
        """
        INSERT INTO daily_reports(day, status, content, sourceCount, error, updatedAt)
        VALUES (:day, :status, :content, :sourceCount, :error, :updatedAt)
        ON CONFLICT(day) DO UPDATE SET
            status = excluded.status,
            content = excluded.content,
            sourceCount = excluded.sourceCount,
            error = excluded.error,
            updatedAt = excluded.updatedAt
        WHERE daily_reports.status <> 'completed'
        """
    )
    suspend fun save(
        day: String,
        status: String,
        content: String,
        sourceCount: Int,
        error: String?,
        updatedAt: Long,
    )

    @Query("DELETE FROM daily_reports")
    suspend fun clearAll()
}
