package com.myapp.cinescope.ui.catalog

import com.myapp.cinescope.domain.model.MediaType
import com.myapp.cinescope.domain.model.Movie

data class CatalogUiState(
    val results: List<Movie> = emptyList(),
    val query: String = "",
    val selectedMediaType: MediaType? = null,
    val selectedGenre: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val availableGenres: List<String>
        get() = results
            .asSequence()
            .flatMap { movie ->
                movie.genreNames
                    .split(",")
                    .asSequence()
                    .map(String::trim)
            }
            .filter(String::isNotEmpty)
            .distinctBy(String::lowercase)
            .sorted()
            .toList()

    val visibleResults: List<Movie>
        get() = results.filter { movie ->
            val matchesMediaType = selectedMediaType == null ||
                    movie.mediaType == selectedMediaType

            val matchesGenre = selectedGenre == null ||
                    movie.genreNames
                        .split(",")
                        .map(String::trim)
                        .any { it.equals(selectedGenre, ignoreCase = true) }

            matchesMediaType && matchesGenre
        }
}
