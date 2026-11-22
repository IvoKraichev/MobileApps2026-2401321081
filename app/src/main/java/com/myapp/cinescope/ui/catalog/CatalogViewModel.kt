package com.myapp.cinescope.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myapp.cinescope.domain.model.MediaType
import com.myapp.cinescope.domain.model.Movie
import com.myapp.cinescope.domain.repository.CatalogRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CatalogViewModel(
    private val repository: CatalogRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        CatalogUiState(isLoading = true)
    )
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    private var requestJob: Job? = null

    init {
        loadTrending()
    }

    fun loadTrending() {
        _uiState.update {
            it.copy(query = "")
        }
        request {
            repository.getTrending()
        }
    }

    fun search(query: String) {
        val normalizedQuery = query.trim()

        _uiState.update {
            it.copy(query = normalizedQuery)
        }

        if (normalizedQuery.isBlank()) {
            loadTrending()
        } else {
            request {
                repository.search(normalizedQuery)
            }
        }
    }

    fun setMediaTypeFilter(mediaType: MediaType?) {
        _uiState.update {
            it.copy(selectedMediaType = mediaType)
        }
    }

    fun retry() {
        val query = uiState.value.query

        if (query.isBlank()) {
            loadTrending()
        } else {
            search(query)
        }
    }

    private fun request(
        load: suspend () -> List<Movie>
    ) {
        requestJob?.cancel()

        requestJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            try {
                val results = load()

                _uiState.update {
                    it.copy(
                        results = results,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        results = emptyList(),
                        isLoading = false,
                        errorMessage = exception.message
                            ?: "Каталогът не можа да се зареди."
                    )
                }
            }
        }
    }
}