package com.myapp.cinescope.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myapp.cinescope.domain.model.Movie
import com.myapp.cinescope.domain.repository.MovieRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryViewModel(private val repository: MovieRepository) : ViewModel() {
    private val selectedMovieId = MutableStateFlow<Long?>(null)
    private val operationState = MutableStateFlow(OperationState())

    val uiState: StateFlow<LibraryUiState> =
        combine(
            repository.observeLibrary(),
            selectedMovieId,
            operationState
        ) { movies, selectedId, operation ->
            LibraryUiState(
                movies = movies,
                favoriteMovies = movies.filter {it.isFavorite},
                watchlistMovies = movies.filter {it.isInWatchlist},
                selectedMovie = movies.firstOrNull { it.localId == selectedId },
                isLoading = false,
                isSaving = operation.isSaving,
                message = operation.message,
                errorMessage = operation.errorMessage
            )
        }.catch { cause ->
            if (cause is CancellationException) {
                throw cause
            }

            emit(
                LibraryUiState(
                    isLoading = false,
                    errorMessage = "Библиотеката не може да се зареди."
                )
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LibraryUiState()
        )

    fun selectMovie(localId: Long) {
        selectedMovieId.value = localId
    }

    fun clearSelectedMovie() {
        selectedMovieId.value = null
    }

    fun saveMovie(movie: Movie) {
        performWrite("Записът е запазен.") {
            if (movie.localId == null) {
                repository.addToLibrary(movie)
                true
            } else {
                repository.updateLibraryMovie(movie)
            }
        }
    }

    fun deleteMovie(localId: Long) {
        performWrite("Заглавието е изтрито.") {
            repository.removeFromLibrary(localId)
        }
    }

    fun setFavorite(localId: Long, isFavorite: Boolean) {
        performWrite("Списъкът с любими е обновен.") {
            repository.setFavorite(localId, isFavorite)
        }
    }

    fun setInWatchlist(localId: Long, isInWatchlist: Boolean) {
        performWrite("Списъкът за гледане е обновен.") {
            repository.setInWatchlist(localId, isInWatchlist)
        }
    }

    fun clearMessages() {
        operationState.update {
            it.copy(
                message = null,
                errorMessage = null
            )
        }
    }

    private fun performWrite(
        successMessage: String,
        action: suspend () -> Boolean
    ) {
        if (operationState.value.isSaving) return

        viewModelScope.launch {
            operationState.update {
                it.copy(
                    isSaving = true,
                    message = null,
                    errorMessage = null
                )
            }

            try {
                val succeeded = action()

                operationState.update {
                    if (succeeded) {
                        it.copy(
                            isSaving = false,
                            message = successMessage
                        )
                    } else {
                        it.copy(
                            isSaving = false,
                            message = "Записът вече не съществува."
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: IllegalArgumentException) {
                operationState.update { it.copy(
                    isSaving = false,
                    errorMessage = exception.message ?: "Проверете въведените данни."
                ) }
            } catch (exception: Exception) {
                operationState.update { it.copy(
                    isSaving = false,
                    errorMessage = "Операцията не беше успешна."
                ) }
            }
        }
    }

    private data class OperationState(
        val isSaving: Boolean = false,
        val message: String? = null,
        val errorMessage: String? = null
    )
}