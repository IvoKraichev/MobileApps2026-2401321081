package com.myapp.cinescope

import android.app.Application
import com.myapp.cinescope.di.AppContainer

class CineScopeApplication: Application() {
    val appContainer: AppContainer by lazy {
        AppContainer(this)
    }
}