package com.myapp.cinescope.data.local

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "library_movies",
    indices = [Index(value = ["tmdbId"])]
)
data class LibraryMovieEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val tmdbId: Int? = null,
    val title: String,
    val overview: String = "",
    val mediaType: String = "MOVIE",
    val releaseDate: String? = null,
    val genreNames: String = "",
    val posterPath: String? = null,
    val averageRating: Double? = null,
    val userRating: Float? = null,
    val isFavorite: Boolean = false,
    val isInWatchlist: Boolean = false,
    val personalNotes: String = ""
)
