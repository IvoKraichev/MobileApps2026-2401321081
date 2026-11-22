package com.myapp.cinescope.ui.catalog

import com.myapp.cinescope.domain.model.MediaType
import com.myapp.cinescope.domain.model.Movie

data class CatalogUiState(
    val results: List<Movie> = emptyList(),
    val query: String = "",
    val selectedMediaType: MediaType? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val visibleResults: List<Movie>
        get() = selectedMediaType?.let { selectedType ->
            results.filter { it.mediaType == selectedMediaType }
        } ?: results
}
