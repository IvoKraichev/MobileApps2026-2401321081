package com.myapp.cinescope.ui.library

import com.myapp.cinescope.MainDispatcherRule
import com.myapp.cinescope.domain.model.MediaType
import com.myapp.cinescope.domain.model.Movie
import com.myapp.cinescope.domain.repository.MovieRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun saveMovie_newMovieAddsItAndPublishesSuccessMessage() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = FakeMovieRepository()
            val viewModel = LibraryViewModel(repository)
            observe(viewModel)

            viewModel.saveMovie(
                Movie(title = "Интерстелар", mediaType = MediaType.MOVIE)
            )
            advanceUntilIdle()

            assertEquals("Интерстелар", viewModel.uiState.value.movies.single().title)
            assertEquals("Записът е запазен.", viewModel.uiState.value.message)
            assertFalse(viewModel.uiState.value.isSaving)
        }

    @Test
    fun saveMovie_existingMovieUpdatesIt() =
        runTest(mainDispatcherRule.dispatcher) {
            val existingMovie = Movie(localId = 7L, title = "Старо заглавие")
            val repository = FakeMovieRepository(listOf(existingMovie))
            val viewModel = LibraryViewModel(repository)
            observe(viewModel)

            viewModel.saveMovie(existingMovie.copy(title = "Ново заглавие"))
            advanceUntilIdle()

            assertEquals(
                "Ново заглавие",
                viewModel.uiState.value.movies.single().title
            )
            assertEquals("Записът е запазен.", viewModel.uiState.value.message)
        }

    @Test
    fun favoriteWatchlistAndSelection_updateUiState() =
        runTest(mainDispatcherRule.dispatcher) {
            val movie = Movie(localId = 3L, title = "Матрицата")
            val viewModel = LibraryViewModel(FakeMovieRepository(listOf(movie)))
            observe(viewModel)

            viewModel.selectMovie(3L)
            advanceUntilIdle()
            assertEquals(movie, viewModel.uiState.value.selectedMovie)

            viewModel.setFavorite(3L, true)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.favoriteMovies.single().isFavorite)

            viewModel.setInWatchlist(3L, true)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.watchlistMovies.single().isInWatchlist)

            viewModel.clearSelectedMovie()
            advanceUntilIdle()
            assertNull(viewModel.uiState.value.selectedMovie)
        }

    @Test
    fun deleteMovie_removesRecordAndPublishesMessage() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = LibraryViewModel(
                FakeMovieRepository(listOf(Movie(localId = 5L, title = "Изтрий ме")))
            )
            observe(viewModel)

            viewModel.deleteMovie(5L)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.movies.isEmpty())
            assertEquals("Заглавието е изтрито.", viewModel.uiState.value.message)
        }

    @Test
    fun failedWritePublishesErrorMessage() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = FakeMovieRepository().apply {
                nextWriteFailure = IllegalArgumentException("Невалидни данни")
            }
            val viewModel = LibraryViewModel(repository)
            observe(viewModel)

            viewModel.saveMovie(Movie(title = "Заглавие"))
            advanceUntilIdle()

            assertEquals(
                "Невалидни данни",
                viewModel.uiState.value.errorMessage
            )
            assertFalse(viewModel.uiState.value.isSaving)
        }

    @Test
    fun missingRecordPublishesMessageAndCanBeCleared() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = LibraryViewModel(FakeMovieRepository())
            observe(viewModel)

            viewModel.deleteMovie(404L)
            advanceUntilIdle()

            assertEquals(
                "Записът вече не съществува.",
                viewModel.uiState.value.message
            )

            viewModel.clearMessages()
            runCurrent()

            assertNull(viewModel.uiState.value.message)
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun repositoryObservationFailurePublishesLibraryError() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = FakeMovieRepository(failObservation = true)
            val viewModel = LibraryViewModel(repository)
            observe(viewModel)
            advanceUntilIdle()

            assertEquals(
                "Библиотеката не може да се зареди.",
                viewModel.uiState.value.errorMessage
            )
            assertFalse(viewModel.uiState.value.isLoading)
        }

    private fun TestScope.observe(viewModel: LibraryViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
    }

    private class FakeMovieRepository(
        initialMovies: List<Movie> = emptyList(),
        private val failObservation: Boolean = false
    ) : MovieRepository {

        private val movies = MutableStateFlow(initialMovies)
        private var nextId = (initialMovies.maxOfOrNull { it.localId ?: 0L } ?: 0L) + 1L

        var nextWriteFailure: Exception? = null

        override fun observeLibrary(): Flow<List<Movie>> {
            return if (failObservation) {
                flow { throw IllegalStateException("Database error") }
            } else {
                movies
            }
        }

        override fun observeFavorites(): Flow<List<Movie>> {
            return movies.map { list -> list.filter(Movie::isFavorite) }
        }

        override fun observeWatchlist(): Flow<List<Movie>> {
            return movies.map { list -> list.filter(Movie::isInWatchlist) }
        }

        override suspend fun findLibraryMovie(localId: Long): Movie? {
            return movies.value.firstOrNull { it.localId == localId }
        }

        override suspend fun addToLibrary(movie: Movie): Long {
            throwPendingFailure()
            val id = nextId++
            movies.value = movies.value + movie.copy(localId = id)
            return id
        }

        override suspend fun updateLibraryMovie(movie: Movie): Boolean {
            throwPendingFailure()
            val id = movie.localId ?: return false
            val index = movies.value.indexOfFirst { it.localId == id }
            if (index == -1) return false

            movies.value = movies.value.toMutableList().apply {
                set(index, movie)
            }
            return true
        }

        override suspend fun removeFromLibrary(localId: Long): Boolean {
            throwPendingFailure()
            val updated = movies.value.filterNot { it.localId == localId }
            if (updated.size == movies.value.size) return false

            movies.value = updated
            return true
        }

        override suspend fun setFavorite(
            localId: Long,
            isFavorite: Boolean
        ): Boolean {
            return updateMovie(localId) { it.copy(isFavorite = isFavorite) }
        }

        override suspend fun setInWatchlist(
            localId: Long,
            isInWatchlist: Boolean
        ): Boolean {
            return updateMovie(localId) { it.copy(isInWatchlist = isInWatchlist) }
        }

        private fun updateMovie(
            localId: Long,
            transform: (Movie) -> Movie
        ): Boolean {
            val index = movies.value.indexOfFirst { it.localId == localId }
            if (index == -1) return false

            movies.value = movies.value.toMutableList().apply {
                this[index] = transform(this[index])
            }
            return true
        }

        private fun throwPendingFailure() {
            val failure = nextWriteFailure ?: return
            nextWriteFailure = null
            throw failure
        }
    }
}