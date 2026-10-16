package com.example.data.repository

import com.example.data.local.TombolDao
import com.example.data.model.Tombol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class TombolRepository(private val tombolDao: TombolDao) {

    val allTombol: Flow<List<Tombol>> = tombolDao.getAllTombol()

    fun getTombolById(id: String): Flow<Tombol?> {
        return tombolDao.getTombolById(id)
    }

    suspend fun getTombolByIdSync(id: String): Tombol? = withContext(Dispatchers.IO) {
        tombolDao.getTombolByIdSync(id)
    }

    suspend fun insert(tombol: Tombol) = withContext(Dispatchers.IO) {
        tombolDao.insert(tombol)
    }

    suspend fun update(tombol: Tombol) = withContext(Dispatchers.IO) {
        tombolDao.update(tombol)
    }

    suspend fun delete(tombol: Tombol) = withContext(Dispatchers.IO) {
        tombolDao.delete(tombol)
    }

    suspend fun updateLastUrl(id: String, url: String) = withContext(Dispatchers.IO) {
        tombolDao.updateLastUrl(id, url)
    }

    suspend fun updateButtonPosition(id: String, x: Float, y: Float) = withContext(Dispatchers.IO) {
        tombolDao.updateButtonPosition(id, x, y)
    }

    suspend fun reorder(items: List<Tombol>) = withContext(Dispatchers.IO) {
        val updated = items.mapIndexed { index, tombol ->
            tombol.copy(urutan = index)
        }
        tombolDao.insertAll(updated)
    }

    suspend fun ensureDefaultSeedIfEmpty() = withContext(Dispatchers.IO) {
        if (tombolDao.getCount() == 0) {
            val defaultTombol = Tombol(
                id = UUID.randomUUID().toString(),
                nama = "Google",
                alamat = "https://www.google.com",
                warna = "#14B8A6",
                ikon = null,
                urutan = 0,
                alamatTerakhir = "https://www.google.com",
                posisiTombolX = 24f,
                posisiTombolY = 36f
            )
            tombolDao.insert(defaultTombol)
        }
    }
}
