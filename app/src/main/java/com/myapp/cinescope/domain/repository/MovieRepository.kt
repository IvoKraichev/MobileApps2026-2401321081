package com.myapp.cinescope.domain.repository

import com.myapp.cinescope.domain.model.Movie
import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    fun observeLibrary(): Flow<List<Movie>>

    fun observeFavorites(): Flow<List<Movie>>

    fun observeWatchlist(): Flow<List<Movie>>

    suspend fun findLibraryMovie(localId: Long): Movie?

    suspend fun addToLibrary(movie: Movie): Long

    suspend fun updateLibraryMovie(movie: Movie): Boolean

    suspend fun removeFromLibrary(localId: Long): Boolean

    suspend fun setFavorite(localId: Long, isFavorite: Boolean): Boolean

    suspend fun setInWatchlist(localId: Long, isInWatchlist: Boolean): Boolean
}