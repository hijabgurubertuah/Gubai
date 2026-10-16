package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tombol")
data class Tombol(
    @PrimaryKey
    val id: String,
    val nama: String,
    val alamat: String,
    val warna: String = "#14B8A6",
    val ikon: String? = null,
    val urutan: Int = 0,
    val alamatTerakhir: String = alamat,
    val posisiTombolX: Float = 24f,
    val posisiTombolY: Float = 36f,
    val mode: String = "Mobile" // "Mobile" or "Desktop"
)
