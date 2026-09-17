package org.openjwc.client.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.openjwc.client.data.models.SourceEntity

@Dao
interface SourceDao {

    @Query("SELECT * FROM notice_sources ORDER BY subscribed DESC, CASE WHEN id = 'seu-jwc' THEN 0 ELSE 1 END, origin DESC, name ASC")
    fun observeAll(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM notice_sources ORDER BY subscribed DESC, CASE WHEN id = 'seu-jwc' THEN 0 ELSE 1 END, origin DESC, name ASC")
    suspend fun getAll(): List<SourceEntity>

    @Query("SELECT * FROM notice_sources WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SourceEntity?

    @Query("SELECT * FROM notice_sources WHERE subscribed = 1 ORDER BY subscribed DESC, CASE WHEN id = 'seu-jwc' THEN 0 ELSE 1 END, origin DESC, name ASC")
    suspend fun getSubscribed(): List<SourceEntity>

    @Query("SELECT * FROM notice_sources WHERE subscribed = 1 ORDER BY subscribed DESC, CASE WHEN id = 'seu-jwc' THEN 0 ELSE 1 END, origin DESC, name ASC")
    fun observeSubscribed(): Flow<List<SourceEntity>>

    @Upsert
    suspend fun upsert(source: SourceEntity)

    @Upsert
    suspend fun upsertAll(sources: List<SourceEntity>)

    @Query("UPDATE notice_sources SET subscribed = :subscribed WHERE id = :id")
    suspend fun setSubscribed(id: String, subscribed: Boolean)

    @Query(
        """
        UPDATE notice_sources
        SET lastRunAt = :timestamp, lastCount = :count, lastError = :error
        WHERE id = :id
        """
    )
    suspend fun updateResult(id: String, timestamp: Long, count: Int, error: String?)

    @Query("DELETE FROM notice_sources WHERE id = :id")
    suspend fun deleteById(id: String)
}
