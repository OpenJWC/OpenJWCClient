package org.openjwc.client.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.openjwc.client.data.models.NoticeEntity

@Dao
interface NewsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(notice: NoticeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorites(notices: List<NoticeEntity>)

    @Delete
    suspend fun deleteFavorite(notice: NoticeEntity)

    @Query("DELETE FROM favorite_notices WHERE id = :noticeId")
    suspend fun deleteFavoriteById(noticeId: String)

    @Query("DELETE FROM favorite_notices")
    suspend fun deleteAllFavorites()

    @Query("SELECT * FROM favorite_notices ORDER BY date DESC")
    fun getAllFavorites(): Flow<List<NoticeEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_notices WHERE id = :noticeId)")
    suspend fun isFavorited(noticeId: String): Boolean
}