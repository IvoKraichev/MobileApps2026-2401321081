package com.myapp.cinescope.data.local

import androidx.room3.Database
import androidx.room3.RoomDatabase

@Database(
    entities = [LibraryMovieEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CineScopeDatabase: RoomDatabase() {
    abstract fun libraryMovieDao(): LibraryMovieDao
}