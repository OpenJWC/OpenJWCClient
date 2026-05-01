package org.openjwc.client.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.openjwc.client.data.models.TableMetadata

/**
 * 课表元数据数据访问对象 (DAO)
 * 负责课表配置信息的增删改查逻辑
 */
@Dao
interface TableDao {

    /**
     * 获取所有课表配置，按 ID 降序排列
     */
    @Query("SELECT * FROM table_metadata ORDER BY id DESC")
    fun getAllTables(): Flow<List<TableMetadata>>

    /**
     * 根据 ID 观察特定课表信息
     */
    @Query("SELECT * FROM table_metadata WHERE id = :id LIMIT 1")
    fun getTableByIdFlow(id: Long): Flow<TableMetadata?>

    /**
     * 实时观察当前标记为活跃的课表
     */
    @Query("SELECT * FROM table_metadata WHERE isCurrent = 1 LIMIT 1")
    fun observeCurrentTable(): Flow<TableMetadata?>

    /**
     * 根据 ID 查询课表元数据快照
     */
    @Query("SELECT * FROM table_metadata WHERE id = :id LIMIT 1")
    suspend fun getTableById(id: Long): TableMetadata?

    /**
     * 同步获取当前活跃课表快照
     */
    @Query("SELECT * FROM table_metadata WHERE isCurrent = 1 LIMIT 1")
    suspend fun getCurrentTableSync(): TableMetadata?

    /**
     * 插入新课表，若 ID 冲突则覆盖
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTable(table: TableMetadata): Long

    /**
     * 更新课表信息
     */
    @Update
    suspend fun updateTable(table: TableMetadata)

    /**
     * 重置所有课表的活跃状态为 0
     */
    @Query("UPDATE table_metadata SET isCurrent = 0")
    suspend fun internalResetAllCurrent()

    /**
     * 将特定 ID 的课表活跃状态设为 1
     */
    @Query("UPDATE table_metadata SET isCurrent = 1 WHERE id = :tableId")
    suspend fun internalSetSingleTableCurrent(tableId: Long)

    /**
     * 切换当前活跃课表（原子事务）
     */
    @Transaction
    suspend fun setCurrentTable(tableId: Long) {
        internalResetAllCurrent()
        internalSetSingleTableCurrent(tableId)
    }

    /**
     * 根据 ID 删除指定课表
     */
    @Query("DELETE FROM table_metadata WHERE id = :tableId")
    suspend fun deleteTableById(tableId: Long)

    /**
     * 自动清理：删除所有没有课程关联的空课表
     */
    @Query("DELETE FROM table_metadata WHERE id NOT IN (SELECT DISTINCT tableId FROM courses)")
    suspend fun deleteEmptyTables()
}