package com.myapp.cinescope.domain.repository

import com.myapp.cinescope.domain.model.Movie

interface CatalogRepository {
    suspend fun getTrending(): List<Movie>

    suspend fun search(query: String): List<Movie>
}