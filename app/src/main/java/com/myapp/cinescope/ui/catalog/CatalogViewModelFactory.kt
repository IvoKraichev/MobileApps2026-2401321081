package com.myapp.cinescope.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.myapp.cinescope.domain.repository.CatalogRepository

class CatalogViewModelFactory(
    private val repository: CatalogRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (!modelClass.isAssignableFrom(CatalogViewModel::class.java)) {
            throw IllegalArgumentException(
                "Неподдържан ViewModel: ${modelClass.name}"
            )
        }

        @Suppress("UNCHECKED_CAST")
        return CatalogViewModel(repository) as T
    }
}