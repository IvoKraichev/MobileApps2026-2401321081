package com.myapp.cinescope.data.repository

import com.myapp.cinescope.data.local.LibraryMovieDao
import com.myapp.cinescope.data.local.LibraryMovieEntity
import com.myapp.cinescope.domain.model.MediaType
import com.myapp.cinescope.domain.model.Movie
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomMovieRepositoryTest {
    private val dao = FakeLibraryMovieDao()
    private val repository = RoomMovieRepository(dao)

    @Test
    fun addToLibrary_trimsTitleAndStoresMovie() = runBlocking {
        val id = repository.addToLibrary(
            Movie(
                title = "  Интерстелар  ",
                mediaType = MediaType.MOVIE,
                genreNames = "Научна фантастика"
            )
        )

        val savedMovie = repository.findLibraryMovie(id)

        assertNotNull(savedMovie)
        assertEquals("Интерстелар", savedMovie?.title)
        assertEquals(MediaType.MOVIE, savedMovie?.mediaType)
        assertEquals(1, repository.observeLibrary().first().size)
    }

    @Test
    fun addToLibrary_returnsExistingIdForDuplicateTmdbMovie() = runBlocking {
        val movie = Movie(tmdbId = 42, title = "Дюн")

        val firstId = repository.addToLibrary(movie)
        val secondId = repository.addToLibrary(movie.copy(title = "Дюн: Част втора"))

        assertEquals(firstId, secondId)
        assertEquals(1, repository.observeLibrary().first().size)
    }

    @Test
    fun updateAndDelete_modifyLibraryRecord() = runBlocking {
        val id = repository.addToLibrary(Movie(title = "Преди редакцията"))

        val updated = repository.updateLibraryMovie(
            Movie(
                localId = id,
                title = "  След редакцията  ",
                mediaType = MediaType.TV_SHOW
            )
        )

        assertTrue(updated)
        assertEquals(
            "След редакцията",
            repository.findLibraryMovie(id)?.title
        )
        assertEquals(
            MediaType.TV_SHOW,
            repository.findLibraryMovie(id)?.mediaType
        )

        assertTrue(repository.removeFromLibrary(id))
        assertNull(repository.findLibraryMovie(id))
        assertFalse(repository.removeFromLibrary(id))
    }

    @Test
    fun favoriteAndWatchlistFlags_areSaved() = runBlocking {
        val id = repository.addToLibrary(Movie(title = "Матрицата"))

        assertTrue(repository.setFavorite(id, true))
        assertTrue(repository.setInWatchlist(id, true))

        val savedMovie = repository.findLibraryMovie(id)
        assertTrue(savedMovie?.isFavorite == true)
        assertTrue(savedMovie?.isInWatchlist == true)
        assertEquals(1, repository.observeFavorites().first().size)
        assertEquals(1, repository.observeWatchlist().first().size)
    }

    @Test
    fun addToLibrary_rejectsBlankTitleAndInvalidRating() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.addToLibrary(Movie(title = "   "))
            }
        }

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.addToLibrary(
                    Movie(title = "Матрицата", userRating = 11f)
                )
            }
        }
    }

    private class FakeLibraryMovieDao : LibraryMovieDao {
        private val records =
            MutableStateFlow<List<LibraryMovieEntity>>(emptyList())

        private var nextId = 1L

        override fun observeAll(): Flow<List<LibraryMovieEntity>> = records

        override suspend fun findById(id: Long): LibraryMovieEntity? {
            return records.value.firstOrNull { it.id == id }
        }

        override suspend fun findByTmdbId(tmdbId: Int): LibraryMovieEntity? {
            return records.value.firstOrNull { it.tmdbId == tmdbId }
        }

        override fun observeFavorites(): Flow<List<LibraryMovieEntity>> {
            return records.map { movies -> movies.filter { it.isFavorite } }
        }

        override fun observeWatchlist(): Flow<List<LibraryMovieEntity>> {
            return records.map { movies -> movies.filter { it.isInWatchlist } }
        }

        override suspend fun insert(movie: LibraryMovieEntity): Long {
            val id = nextId++
            records.value = records.value + movie.copy(id = id)
            return id
        }

        override suspend fun update(movie: LibraryMovieEntity): Int {
            val index = records.value.indexOfFirst { it.id == movie.id }
            if (index == -1) return 0

            records.value = records.value.toMutableList().apply {
                set(index, movie)
            }
            return 1
        }

        override suspend fun delete(movie: LibraryMovieEntity): Int {
            return deleteById(movie.id)
        }

        override suspend fun deleteById(id: Long): Int {
            val updated = records.value.filterNot { it.id == id }
            if (updated.size == records.value.size) return 0

            records.value = updated
            return 1
        }
    }
}