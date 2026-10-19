package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TabEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TabDao {
    @Query("SELECT * FROM tabs WHERE siteId = :siteId ORDER BY urutan ASC")
    fun getTabsForSite(siteId: String): Flow<List<TabEntity>>

    @Query("SELECT * FROM tabs WHERE siteId = :siteId ORDER BY urutan ASC")
    suspend fun getTabsForSiteList(siteId: String): List<TabEntity>

    @Query("SELECT * FROM tabs WHERE tabId = :tabId LIMIT 1")
    suspend fun getTabById(tabId: String): TabEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tab: TabEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tabs: List<TabEntity>)

    @Update
    suspend fun update(tab: TabEntity)

    @Delete
    suspend fun delete(tab: TabEntity)

    @Query("DELETE FROM tabs WHERE tabId = :tabId")
    suspend fun deleteById(tabId: String)

    @Query("DELETE FROM tabs WHERE siteId = :siteId")
    suspend fun deleteBySiteId(siteId: String)

    @Query("UPDATE tabs SET isActive = 0 WHERE siteId = :siteId")
    suspend fun deactivateAllForSite(siteId: String)

    @Query("UPDATE tabs SET isActive = 1 WHERE tabId = :tabId")
    suspend fun activateTab(tabId: String)

    @Query("UPDATE tabs SET title = :title, url = :url WHERE tabId = :tabId")
    suspend fun updateTitleAndUrl(tabId: String, title: String, url: String)

    @Query("SELECT COUNT(*) FROM tabs WHERE siteId = :siteId")
    suspend fun getTabCount(siteId: String): Int
}
