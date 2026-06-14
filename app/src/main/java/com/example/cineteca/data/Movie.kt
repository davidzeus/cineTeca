package com.example.cineteca.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movies")
data class Movie(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val isWatched: Boolean = false,
    val thumbnailUrl: String? = null,
    val description: String? = null,
    val platform: String? = null
)
