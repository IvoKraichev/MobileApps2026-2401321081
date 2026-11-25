package com.myapp.cinescope.ui.catalog

import com.myapp.cinescope.MainDispatcherRule
import com.myapp.cinescope.domain.model.MediaType
import com.myapp.cinescope.domain.model.Movie
import com.myapp.cinescope.domain.repository.CatalogRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialLoad_publishesTrendingResults() =
        runTest(mainDispatcherRule.dispatcher) {
            val trending = listOf(
                Movie(tmdbId = 1, title = "Начало"),
                Movie(tmdbId = 2, title = "Одисея")
            )
            val repository = FakeCatalogRepository(trending = trending)

            val viewModel = CatalogViewModel(repository)
            advanceUntilIdle()

            assertEquals(1, repository.trendingCalls)
            assertEquals(trending, viewModel.uiState.value.results)
            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun search_trimsQueryAndPublishesResults() =
        runTest(mainDispatcherRule.dispatcher) {
            val searchResults = listOf(Movie(tmdbId = 10, title = "Дюн"))
            val repository = FakeCatalogRepository(searchResults = searchResults)
            val viewModel = CatalogViewModel(repository)

            advanceUntilIdle()
            viewModel.search("  Дюн  ")
            advanceUntilIdle()

            assertEquals("Дюн", repository.lastSearchQuery)
            assertEquals("Дюн", viewModel.uiState.value.query)
            assertEquals(searchResults, viewModel.uiState.value.results)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun blankSearch_loadsTrendingAgainAndClearsQuery() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = FakeCatalogRepository(
                trending = listOf(Movie(title = "Популярно"))
            )
            val viewModel = CatalogViewModel(repository)

            advanceUntilIdle()
            viewModel.search("   ")
            advanceUntilIdle()

            assertEquals(2, repository.trendingCalls)
            assertEquals("", viewModel.uiState.value.query)
            assertEquals("Популярно", viewModel.uiState.value.results.single().title)
        }

    @Test
    fun searchFailure_setsErrorAndClearsResults() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = FakeCatalogRepository(
                searchFailure = IllegalStateException("Няма връзка")
            )
            val viewModel = CatalogViewModel(repository)

            advanceUntilIdle()
            viewModel.search("Матрицата")
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.results.isEmpty())
            assertEquals("Няма връзка", viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun retry_repeatsCurrentSearch() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = FakeCatalogRepository(
                searchFailure = IllegalStateException("Временна грешка")
            )
            val viewModel = CatalogViewModel(repository)

            advanceUntilIdle()
            viewModel.search("Дюн")
            advanceUntilIdle()

            repository.searchFailure = null
            repository.searchResults = listOf(Movie(title = "Дюн"))
            viewModel.retry()
            advanceUntilIdle()

            assertEquals("Дюн", repository.lastSearchQuery)
            assertEquals("Дюн", viewModel.uiState.value.query)
            assertEquals("Дюн", viewModel.uiState.value.results.single().title)
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun filterMethods_updateSelectedFilters() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = CatalogViewModel(FakeCatalogRepository())
            advanceUntilIdle()

            viewModel.setMediaTypeFilter(MediaType.TV_SHOW)
            viewModel.setGenreFilter("Драма")

            assertEquals(MediaType.TV_SHOW, viewModel.uiState.value.selectedMediaType)
            assertEquals("Драма", viewModel.uiState.value.selectedGenre)
        }

    private class FakeCatalogRepository(
        private val trending: List<Movie> = emptyList(),
        var searchResults: List<Movie> = emptyList(),
        var searchFailure: Exception? = null
    ) : CatalogRepository {

        var trendingCalls = 0
            private set

        var lastSearchQuery: String? = null
            private set

        override suspend fun getTrending(): List<Movie> {
            trendingCalls++
            return trending
        }

        override suspend fun search(query: String): List<Movie> {
            lastSearchQuery = query
            searchFailure?.let { throw it }
            return searchResults
        }
    }
}