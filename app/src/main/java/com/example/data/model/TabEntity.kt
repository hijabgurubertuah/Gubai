package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tabs",
    indices = [Index(value = ["siteId"])]
)
data class TabEntity(
    @PrimaryKey
    val tabId: String,
    val siteId: String,
    val url: String,
    val title: String,
    val isDesktopMode: Boolean = false,
    val urutan: Int = 0,
    val isActive: Boolean = false
)
