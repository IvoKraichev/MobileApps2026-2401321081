package com.myapp.cinescope.di

import android.content.Context
import androidx.room3.Room
import com.myapp.cinescope.BuildConfig
import com.myapp.cinescope.data.local.CineScopeDatabase
import com.myapp.cinescope.data.repository.RoomMovieRepository
import com.myapp.cinescope.domain.repository.MovieRepository
import com.myapp.cinescope.data.remote.TmdbApiFactory
import com.myapp.cinescope.data.repository.TmdbCatalogRepository
import com.myapp.cinescope.domain.repository.CatalogRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: CineScopeDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            CineScopeDatabase::class.java,
            "cinescope_db"
        ).build()
    }

    val movieRepository: MovieRepository by lazy {
        RoomMovieRepository(database.libraryMovieDao())
    }

    val catalogRepository: CatalogRepository by lazy {
        val token = BuildConfig.TMDB_READ_ACCESS_TOKEN
        TmdbCatalogRepository(
            api = TmdbApiFactory.create(),
            accessToken = token
        )
    }
}