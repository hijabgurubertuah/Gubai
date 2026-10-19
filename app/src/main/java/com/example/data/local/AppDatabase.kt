package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Tombol
import com.example.data.model.TabEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(entities = [Tombol::class, TabEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tombolDao(): TombolDao
    abstract fun tabDao(): TabDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tombol ADD COLUMN mode TEXT NOT NULL DEFAULT 'Mobile'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tabs (
                        tabId TEXT PRIMARY KEY NOT NULL,
                        siteId TEXT NOT NULL,
                        url TEXT NOT NULL,
                        title TEXT NOT NULL,
                        isDesktopMode INTEGER NOT NULL DEFAULT 0,
                        urutan INTEGER NOT NULL DEFAULT 0,
                        isActive INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tabs_siteId ON tabs (siteId)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gubai_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database.tombolDao())
                    }
                }
            }

            private suspend fun populateInitialData(dao: TombolDao) {
                if (dao.getCount() == 0) {
                    val defaultTombol = Tombol(
                        id = UUID.randomUUID().toString(),
                        nama = "Google",
                        alamat = "https://www.google.com",
                        warna = "#14B8A6",
                        ikon = null,
                        urutan = 0,
                        alamatTerakhir = "https://www.google.com",
                        posisiTombolX = 24f,
                        posisiTombolY = 36f,
                        mode = "Mobile"
                    )
                    dao.insert(defaultTombol)
                }
            }
        }
    }
}
