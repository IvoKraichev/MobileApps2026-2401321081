package com.myapp.cinescope.data.repository

import com.myapp.cinescope.data.remote.TmdbApiService
import com.myapp.cinescope.data.remote.TmdbItemDto
import com.myapp.cinescope.domain.model.MediaType
import com.myapp.cinescope.domain.model.Movie
import com.myapp.cinescope.domain.repository.CatalogRepository

class TmdbCatalogRepository(
    private val api: TmdbApiService,
    private val accessToken: String
) : CatalogRepository {
    private var genreNames: Map<MediaType, Map<Int, String>>? = null

    override suspend fun getTrending(): List<Movie> {
        requireToken()

        val response = api.getTrending()
        return mapResults(response.results.orEmpty())
    }

    override suspend fun search(query: String): List<Movie> {
        requireToken()

        val normalizedQuery = query.trim()
        require(normalizedQuery.isNotEmpty()) {
            "Въведи дума за търсене."
        }

        val response = api.searchMulti(normalizedQuery)
        return mapResults(response.results.orEmpty())
    }

    private suspend fun mapResults(
        results: List<TmdbItemDto>
    ): List<Movie> {
        val genres = getGenreNames()

        return results.mapNotNull { item ->
            val mediaType = when (item.mediaType) {
                "movie" -> MediaType.MOVIE
                "tv" -> MediaType.TV_SHOW
                else -> return@mapNotNull null
            }

            val id = item.id ?: return@mapNotNull null
            val title = when (mediaType) {
                MediaType.MOVIE -> item.title ?: item.originalTitle
                MediaType.TV_SHOW -> item.name ?: item.originalName
            }?.trim()?.takeIf(String::isNotEmpty)
                ?: return@mapNotNull null

            val genreMap = genres[mediaType].orEmpty()
            val genreNames = item.genreIds
                .orEmpty()
                .mapNotNull(genreMap::get)
                .joinToString(", ")

            Movie(
                tmdbId = id,
                title = title,
                overview = item.overview.orEmpty(),
                mediaType = mediaType,
                releaseDate = when (mediaType) {
                    MediaType.MOVIE -> item.releaseDate
                    MediaType.TV_SHOW -> item.firstAirDate
                },
                genreNames = genreNames,
                posterPath = item.posterPath,
                averageRating = item.voteAverage
            )
        }
    }

    private suspend fun getGenreNames(): Map<MediaType, Map<Int, String>> {
        genreNames?.let { return it }

        val movieGenres = api.getMovieGenres()
            .genres
            .orEmpty()
            .mapNotNull { genre ->
                val id = genre.id ?: return@mapNotNull null
                val name = genre.name ?: return@mapNotNull null
                id to name
            }.toMap()

        val tvGenres = api.getTvGenres()
            .genres
            .orEmpty()
            .mapNotNull { genre ->
                val id = genre.id ?: return@mapNotNull null
                val name = genre.name ?: return@mapNotNull null
                id to name
            }.toMap()

        return mapOf(
            MediaType.MOVIE to movieGenres,
            MediaType.TV_SHOW to tvGenres
        ).also { genreNames = it }
    }

    private fun requireToken() {
        check(accessToken.isNotBlank()) {
            "Липсва TMDB_READ_ACCESS_TOKEN в глобалните Gradle настройки."
        }
    }
}