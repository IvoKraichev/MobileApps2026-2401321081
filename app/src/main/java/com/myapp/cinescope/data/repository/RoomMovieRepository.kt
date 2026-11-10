package com.myapp.cinescope.data.repository

import com.myapp.cinescope.data.local.LibraryMovieDao
import com.myapp.cinescope.data.mapper.toDomain
import com.myapp.cinescope.data.mapper.toEntity
import com.myapp.cinescope.domain.model.Movie
import com.myapp.cinescope.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomMovieRepository(private val movieDao: LibraryMovieDao): MovieRepository {
    override fun observeLibrary(): Flow<List<Movie>> {
        return movieDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeFavorites(): Flow<List<Movie>> {
        return movieDao.observeFavorites().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeWatchlist(): Flow<List<Movie>> {
        return movieDao.observeWatchlist().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun findLibraryMovie(localId: Long): Movie? {
        return movieDao.findById(localId)?.toDomain()
    }

    override suspend fun addToLibrary(movie: Movie): Long {
        val title = movie.title.trim()
        require(title.isNotEmpty()) {
            "Заглавието не може да бъде празно."
        }

        require(movie.userRating == null || movie.userRating in 0f..10f) {
            "Личната оценка трябва да бъде между 0 и 10."
        }

        val existingMovie = movie.tmdbId?.let { movieDao.findByTmdbId(it) }

        if (existingMovie != null) {
            return existingMovie.id
        }

        val newMovie = movie.copy(
            localId = null,
            title = title
        )

        return movieDao.insert(newMovie.toEntity())
    }

    override suspend fun updateLibraryMovie(movie: Movie): Boolean {
        val localId = requireNotNull(movie.localId) {
            "Редактираният запис трябва да има локален идентификатор."
        }

        val title = movie.title.trim()
        require(title.isNotEmpty()) {
            "Заглавието не може да бъде празно."
        }

        require(movie.userRating == null || movie.userRating in 0f..10f) {
            "Личната оценка трябва да бъде между 0 и 10."
        }

        val updatedMovie = movie.copy(
            localId = localId,
            title = title
        )

        return movieDao.update(updatedMovie.toEntity()) > 0
    }

    override suspend fun removeFromLibrary(localId: Long): Boolean {
        return movieDao.deleteById(localId) > 0
    }

    override suspend fun setFavorite(localId: Long, isFavorite: Boolean): Boolean {
        val movie = movieDao.findById(localId) ?: return false
        return movieDao.update(
            movie.copy(isFavorite = isFavorite)
        ) > 0
    }

    override suspend fun setInWatchlist(localId: Long, isInWatchlist: Boolean): Boolean {
        val movie = movieDao.findById(localId) ?: return false
        return movieDao.update(
            movie.copy(isInWatchlist = isInWatchlist)
        ) > 0
    }
}