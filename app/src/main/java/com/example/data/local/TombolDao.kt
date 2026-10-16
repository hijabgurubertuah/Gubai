package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Tombol
import kotlinx.coroutines.flow.Flow

@Dao
interface TombolDao {
    @Query("SELECT * FROM tombol ORDER BY urutan ASC")
    fun getAllTombol(): Flow<List<Tombol>>

    @Query("SELECT * FROM tombol ORDER BY urutan ASC")
    suspend fun getAllTombolList(): List<Tombol>

    @Query("SELECT * FROM tombol WHERE id = :id LIMIT 1")
    fun getTombolById(id: String): Flow<Tombol?>

    @Query("SELECT * FROM tombol WHERE id = :id LIMIT 1")
    suspend fun getTombolByIdSync(id: String): Tombol?

    @Query("SELECT COUNT(*) FROM tombol")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tombol: Tombol)

    @Update
    suspend fun update(tombol: Tombol)

    @Delete
    suspend fun delete(tombol: Tombol)

    @Query("DELETE FROM tombol WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE tombol SET alamatTerakhir = :url WHERE id = :id")
    suspend fun updateLastUrl(id: String, url: String)

    @Query("UPDATE tombol SET posisiTombolX = :x, posisiTombolY = :y WHERE id = :id")
    suspend fun updateButtonPosition(id: String, x: Float, y: Float)

    @Query("UPDATE tombol SET urutan = :urutan WHERE id = :id")
    suspend fun updateOrder(id: String, urutan: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tombolList: List<Tombol>)
}
