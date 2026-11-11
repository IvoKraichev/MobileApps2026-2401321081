package com.myapp.cinescope.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.myapp.cinescope.domain.repository.MovieRepository

class LibraryViewModelFactory(
    private val repository: MovieRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (!modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            throw IllegalArgumentException(
                "Неподдържан ViewModel: ${modelClass.name}"
            )
        }

        @Suppress("UNCHECKED_CAST")
        return LibraryViewModel(repository) as T
    }
}