package com.myapp.cinescope.data.mapper

import com.myapp.cinescope.data.local.LibraryMovieEntity
import com.myapp.cinescope.domain.model.MediaType
import com.myapp.cinescope.domain.model.Movie

fun LibraryMovieEntity.toDomain() : Movie {
    val parsedMediaType = runCatching {
        MediaType.valueOf(mediaType)
    }.getOrDefault(MediaType.MOVIE)

    return Movie(
        localId = id,
        tmdbId = tmdbId,
        title = title,
        overview = overview,
        mediaType = parsedMediaType,
        releaseDate = releaseDate,
        genreNames = genreNames,
        posterPath = posterPath,
        averageRating = averageRating,
        userRating = userRating,
        isFavorite = isFavorite,
        isInWatchlist = isInWatchlist,
        personalNotes = personalNotes
    )
}

fun Movie.toEntity(): LibraryMovieEntity {
    return LibraryMovieEntity(
        id = localId ?: 0,
        tmdbId = tmdbId,
        title = title,
        overview = overview,
        mediaType = mediaType.name,
        releaseDate = releaseDate,
        genreNames = genreNames,
        posterPath = posterPath,
        averageRating = averageRating,
        userRating = userRating,
        isFavorite = isFavorite,
        isInWatchlist = isInWatchlist,
        personalNotes = personalNotes
    )
}