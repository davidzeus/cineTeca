package com.example.cineteca.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movie: Movie)

    @Query("SELECT * FROM movies ORDER BY addedAt DESC")
    fun getAllMovies(): Flow<List<Movie>>
}
