package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Tombol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(entities = [Tombol::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tombolDao(): TombolDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tombol ADD COLUMN mode TEXT NOT NULL DEFAULT 'Mobile'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gubai_database"
                )
                    .addMigrations(MIGRATION_1_2)
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
