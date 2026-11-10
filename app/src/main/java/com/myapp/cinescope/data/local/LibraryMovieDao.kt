package com.myapp.cinescope.data.local

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryMovieDao {
    @Query(
        """
            SELECT * FROM library_movies
            ORDER BY title COLLATE NOCASE
        """
    )
    fun observeAll(): Flow<List<LibraryMovieEntity>>

    @Query(
        """
            SELECT * FROM library_movies
            WHERE id = :id
            LIMIT 1
        """
    )
    suspend fun findById(id: Long): LibraryMovieEntity?

    @Query(
        """
            SELECT * FROM library_movies
            WHERE tmdbId = :tmdbId
            LIMIT 1
        """
    )
    suspend fun findByTmdbId(tmdbId: Int): LibraryMovieEntity?

    @Query(
        """
            SELECT * FROM library_movies
            WHERE isFavorite = 1
            ORDER BY title COLLATE NOCASE
        """
    )
    fun observeFavorites(): Flow<List<LibraryMovieEntity>>

    @Query(
        """
            SELECT * FROM library_movies
            WHERE isInWatchlist = 1
            ORDER BY title COLLATE NOCASE
        """
    )
    fun observeWatchlist(): Flow<List<LibraryMovieEntity>>

    @Insert
    suspend fun insert(movie: LibraryMovieEntity): Long

    @Update
    suspend fun update(movie: LibraryMovieEntity): Int

    @Delete
    suspend fun delete(movie: LibraryMovieEntity): Int

    @Query(
        """
            DELETE FROM library_movies
            WHERE id = :id
        """
    )
    suspend fun deleteById(id: Long): Int
}