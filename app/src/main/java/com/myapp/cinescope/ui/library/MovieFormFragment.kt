package com.myapp.cinescope.ui.library

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Filter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import com.myapp.cinescope.CineScopeApplication
import com.myapp.cinescope.R
import com.myapp.cinescope.domain.model.MediaType
import com.myapp.cinescope.domain.model.Movie
import kotlinx.coroutines.launch

class MovieFormFragment : Fragment(R.layout.fragment_movie_form) {
    private val viewModel: LibraryViewModel by activityViewModels {
        val app = requireActivity().application as CineScopeApplication
        LibraryViewModelFactory(app.appContainer.movieRepository)
    }

    private var existingMovie: Movie? = null
    private var formPopulated = false
    private var saveRequested = false
    private var missingRecordHandled = false

    private lateinit var formRoot: View
    private lateinit var heading: MaterialTextView
    private lateinit var titleLayout: TextInputLayout
    private lateinit var titleInput: TextInputEditText
    private lateinit var typeInput: MaterialAutoCompleteTextView
    private lateinit var releaseDateInput: TextInputEditText
    private lateinit var genresInput: TextInputEditText
    private lateinit var overviewInput: TextInputEditText
    private lateinit var ratingLayout: TextInputLayout
    private lateinit var ratingInput: TextInputEditText
    private lateinit var notesInput: TextInputEditText
    private lateinit var favoriteCheckbox: MaterialCheckBox
    private lateinit var watchlistCheckbox: MaterialCheckBox
    private lateinit var progress: ProgressBar
    private lateinit var saveButton: MaterialButton

    private val movieId: Long
        get() = requireArguments().getLong(ARG_MOVIE_ID, NEW_MOVIE_ID)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        configureTypeMenu()

        if (movieId == NEW_MOVIE_ID) {
            viewModel.clearSelectedMovie()
            formPopulated = true
            heading.setText(R.string.movie_form_add_title)
            typeInput.setText(getString(R.string.movie_type_movie), false)
        } else {
            heading.setText(R.string.movie_form_edit_title)
            viewModel.selectMovie(movieId)
            saveButton.isEnabled = false
            progress.visibility = View.VISIBLE
        }

        view.findViewById<MaterialButton>(R.id.cancelMovieButton)
            .setOnClickListener {
                closeForm()
            }

        saveButton.setOnClickListener {
            submitForm()
        }

        observeState()
    }

    private fun bindViews(view: View) {
        formRoot = view.findViewById(R.id.movieFormRoot)
        heading = view.findViewById(R.id.formHeading)
        titleLayout = view.findViewById(R.id.titleInputLayout)
        titleInput = view.findViewById(R.id.titleInput)
        typeInput = view.findViewById(R.id.typeInput)
        releaseDateInput = view.findViewById(R.id.releaseDateInput)
        genresInput = view.findViewById(R.id.genresInput)
        overviewInput = view.findViewById(R.id.overviewInput)
        ratingLayout = view.findViewById(R.id.userRatingInputLayout)
        ratingInput = view.findViewById(R.id.userRatingInput)
        notesInput = view.findViewById(R.id.notesInput)
        favoriteCheckbox = view.findViewById(R.id.favoriteCheckbox)
        watchlistCheckbox = view.findViewById(R.id.watchlistCheckbox)
        progress = view.findViewById(R.id.formProgress)
        saveButton = view.findViewById(R.id.saveMovieButton)
    }

    private fun configureTypeMenu() {
        val typeLabels = listOf(
            getString(R.string.movie_type_movie),
            getString(R.string.movie_type_tv)
        )

        val typeAdapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            typeLabels.toMutableList()
        ) {
            override fun getFilter(): Filter {
                return object : Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults? {
                        return FilterResults().apply {
                            values = typeLabels
                            count = typeLabels.size
                        }
                    }

                    override fun publishResults(
                        constraint: CharSequence?,
                        results: Filter.FilterResults?
                    ) {
                        val items = (results?.values as? List<*>)
                            ?.filterIsInstance<String>()
                            .orEmpty()

                        clear()
                        addAll(items.ifEmpty { typeLabels })
                    }

                    override fun convertResultToString(resultValue: Any?): CharSequence? {
                        return resultValue.toString()
                    }
                }
            }
        }

        typeInput.setAdapter(typeAdapter)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (movieId != NEW_MOVIE_ID) {
                        val movie = state.selectedMovie
                        if (movie?.localId == movieId && !formPopulated) {
                            existingMovie = movie
                            populateForm(movie)
                            formPopulated = true
                            progress.visibility = View.GONE
                        }

                        if (
                            !state.isLoading &&
                            state.selectedMovie == null &&
                            existingMovie == null &&
                            !missingRecordHandled
                        ) {
                            missingRecordHandled = true
                            Snackbar.make(
                                formRoot,
                                R.string.movie_not_found,
                                Snackbar.LENGTH_LONG
                            ).show()
                            closeForm()
                            return@collect
                        }
                    }

                    saveButton.isEnabled =
                        !saveRequested && (movieId == NEW_MOVIE_ID || formPopulated)

                    progress.visibility = if (state.isSaving) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                    if (saveRequested && !state.isSaving) {
                        when {
                            state.errorMessage != null -> {
                                saveRequested = false
                                Snackbar.make(
                                    formRoot,
                                    state.errorMessage,
                                    Snackbar.LENGTH_LONG
                                ).show()
                                viewModel.clearMessages()
                            }

                            state.message != null -> {
                                findNavController()
                                    .previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set(SAVE_RESULT_KEY, state.message)

                                saveRequested = false
                                viewModel.clearMessages()
                                closeForm()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun populateForm(movie: Movie) {
        titleInput.setText(movie.title)
        typeInput.setText(
            getString(
                if (movie.mediaType == MediaType.MOVIE) {
                    R.string.movie_type_movie
                } else {
                    R.string.movie_type_tv
                }
            ),
            false
        )
        releaseDateInput.setText(movie.releaseDate.orEmpty())
        genresInput.setText(movie.genreNames)
        overviewInput.setText(movie.overview)
        ratingInput.setText(movie.userRating?.toString().orEmpty())
        notesInput.setText(movie.personalNotes)
        favoriteCheckbox.isChecked = movie.isFavorite
        watchlistCheckbox.isChecked = movie.isInWatchlist
    }

    private fun submitForm() {
        val title = titleInput.text?.toString()?.trim().orEmpty()
        val typeLabel = typeInput.text?.toString()?.trim().orEmpty()
        val ratingText = ratingInput.text?.toString()?.trim().orEmpty()
        val rating = ratingText.toFloatOrNull()

        titleLayout.error = if (title.isBlank()) {
            getString(R.string.movie_title_required)
        } else {
            null
        }

        val mediaType = when (typeLabel) {
            getString(R.string.movie_type_movie) -> MediaType.MOVIE
            getString(R.string.movie_type_tv) -> MediaType.TV_SHOW
            else -> null
        }

        val ratingIsInvalid =
            ratingText.isNotEmpty() && (rating == null || rating !in 0f..10f)

        ratingLayout.error = if (ratingIsInvalid) {
            getString(R.string.movie_rating_invalid)
        } else {
            null
        }

        if (title.isBlank() || mediaType == null || ratingIsInvalid) {
            if (mediaType == null) {
                Snackbar.make(
                    formRoot,
                    R.string.movie_type_required,
                    Snackbar.LENGTH_LONG
                ).show()
            }
            return
        }

        val previous = existingMovie
        val movie = Movie(
            localId = previous?.localId,
            tmdbId = previous?.tmdbId,
            title = title,
            overview = overviewInput.text?.toString()?.trim().orEmpty(),
            mediaType = mediaType,
            releaseDate = releaseDateInput.text
                ?.toString()
                ?.trim()
                ?.ifBlank { null },
            genreNames = genresInput.text
                ?.toString()
                ?.split(",")
                ?.map(String::trim)
                ?.filter(String::isNotEmpty)
                ?.joinToString(", ")
                .orEmpty(),
            posterPath = previous?.posterPath,
            averageRating = previous?.averageRating,
            userRating = rating,
            isFavorite = favoriteCheckbox.isChecked,
            isInWatchlist = watchlistCheckbox.isChecked,
            personalNotes = notesInput.text?.toString()?.trim().orEmpty()
        )

        viewModel.clearMessages()
        saveRequested = true
        viewModel.saveMovie(movie)
    }

    private fun closeForm() {
        viewModel.clearSelectedMovie()
        findNavController().navigateUp()
    }

    companion object {
        const val ARG_MOVIE_ID = "movieId"
        const val NEW_MOVIE_ID = -1L
        const val SAVE_RESULT_KEY = "movieSaveResult"
    }
}