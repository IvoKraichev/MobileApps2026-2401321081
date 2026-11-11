package com.myapp.cinescope.ui.library

import com.myapp.cinescope.domain.model.Movie

data class LibraryUiState(
    val movies: List<Movie> = emptyList(),
    val favoriteMovies: List<Movie> = emptyList(),
    val watchlistMovies: List<Movie> = emptyList(),
    val selectedMovie: Movie? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null
)
