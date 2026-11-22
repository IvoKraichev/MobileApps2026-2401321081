package com.myapp.cinescope.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface TmdbApiService {
    @GET("trending/all/day")
    suspend fun getTrending(
        @Query("language") language: String = "bg-BG"
    ): TmdbPageDto

    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("language") language: String = "bg-BG",
        @Query("include_adult") includeAdult: Boolean = false
    ): TmdbPageDto

    @GET("genre/movie/list")
    suspend fun getMovieGenres(
        @Query("language") language: String = "bg-BG"
    ): TmdbGenreListDto

    @GET("genre/tv/list")
    suspend fun getTvGenres(
        @Query("language") language: String = "bg-BG"
    ): TmdbGenreListDto
}

data class TmdbPageDto(
    @SerializedName("results")
    val results: List<TmdbItemDto>? = null
)

data class TmdbItemDto(
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("media_type")
    val mediaType: String? = null,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("original_title")
    val originalTitle: String? = null,

    @SerializedName("original_name")
    val originalName: String? = null,

    @SerializedName("overview")
    val overview: String? = null,

    @SerializedName("release_date")
    val releaseDate: String? = null,

    @SerializedName("first_air_date")
    val firstAirDate: String? = null,

    @SerializedName("genre_ids")
    val genreIds: List<Int>? = null,

    @SerializedName("poster_path")
    val posterPath: String? = null,

    @SerializedName("vote_average")
    val voteAverage: Double? = null
)

data class TmdbGenreListDto(
    @SerializedName("genres")
    val genres: List<TmdbGenreDto>? = null
)

data class TmdbGenreDto(
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("name")
    val name: String? = null
)