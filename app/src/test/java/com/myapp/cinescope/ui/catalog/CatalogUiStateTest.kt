package com.myapp.cinescope.ui.catalog

import com.myapp.cinescope.domain.model.MediaType
import com.myapp.cinescope.domain.model.Movie
import org.junit.Test
import org.junit.Assert.assertEquals

class CatalogUiStateTest {
    private val movies = listOf(
        Movie(
            tmdbId = 1,
            title = "Начало",
            mediaType = MediaType.MOVIE,
            genreNames = "Научна фантастика, Екшън"
        ),
        Movie(
            tmdbId = 2,
            title = "Тъмно",
            mediaType = MediaType.TV_SHOW,
            genreNames = "Драма, Научна фантастика"
        ),
        Movie(
            tmdbId = 3,
            title = "Одисея",
            mediaType = MediaType.MOVIE,
            genreNames = "Приключение"
        )
    )

    @Test
    fun availableGenres_removesDuplicatesIgnoringCase() {
        val state = CatalogUiState(results = movies)

        assertEquals(
            listOf("Драма", "Екшън", "Научна фантастика", "Приключение"),
            state.availableGenres
        )
    }

    @Test
    fun visibleResults_filtersByMediaType() {
        val state = CatalogUiState(
            results = movies,
            selectedMediaType = MediaType.TV_SHOW
        )

        assertEquals(listOf("Тъмно"), state.visibleResults.map(Movie::title))
    }

    @Test
    fun visibleResults_filtersGenreIgnoringCaseAndWhitespace() {
        val state = CatalogUiState(
            results = movies,
            selectedGenre = "  научна фантастика "
        )

        assertEquals(
            listOf("Начало", "Тъмно"),
            state.visibleResults.map(Movie::title)
        )
    }

    @Test
    fun visibleResults_combinesMediaTypeAndGenreFilters() {
        val state = CatalogUiState(
            results = movies,
            selectedMediaType = MediaType.MOVIE,
            selectedGenre = "Научна фантастика"
        )

        assertEquals(listOf("Начало"), state.visibleResults.map(Movie::title))
    }
}