package com.myapp.cinescope.domain.model

enum class MediaType {
    MOVIE,
    TV_SHOW
}

data class Movie(
    val localId: Long? = null,
    val tmdbId: Int? = null,
    val title: String,
    val overview: String = "",
    val mediaType: MediaType = MediaType.MOVIE,
    val releaseDate: String? = null,
    val genreNames: String = "",
    val posterPath: String? = null,
    val averageRating: Double? = null,
    val userRating: Float? = null,
    val isFavorite: Boolean = false,
    val isInWatchlist: Boolean = false,
    val personalNotes: String = ""
)
