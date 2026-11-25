package com.myapp.cinescope.ui.library

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.myapp.cinescope.CineScopeApplication
import com.myapp.cinescope.R
import com.myapp.cinescope.domain.model.MediaType
import com.myapp.cinescope.domain.model.Movie
import kotlinx.coroutines.launch
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.myapp.cinescope.ui.details.MovieDetailsFragment

class LibraryFragment : Fragment(R.layout.fragment_library) {
    private val viewModel: LibraryViewModel by activityViewModels {
        val app = requireActivity().application as CineScopeApplication
        LibraryViewModelFactory(app.appContainer.movieRepository)
    }

    private lateinit var loadingIndicator: ProgressBar
    private lateinit var emptyLibraryText: TextView
    private lateinit var addMovieButton: MaterialButton
    private lateinit var adapter: LibraryMovieAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingIndicator = view.findViewById(R.id.loadingIndicator)
        emptyLibraryText = view.findViewById(R.id.emptyLibraryText)
        addMovieButton = view.findViewById(R.id.addMovieButton)

        configureList(view)
        observeUiState(view)

        addMovieButton.setOnClickListener {
            viewModel.clearMessages()
            viewModel.clearSelectedMovie()
            openEditor(MovieFormFragment.NEW_MOVIE_ID)
        }
    }

    private fun configureList(root: View) {
        adapter = LibraryMovieAdapter(
            onOpen = ::showMovieDetails,
            onFavorite = { movie ->
                movie.localId?.let { id ->
                    viewModel.setFavorite(id, !movie.isFavorite)
                }
            },
            onWatchlist = { movie ->
                movie.localId?.let { id ->
                    viewModel.setInWatchlist(id, !movie.isInWatchlist)
                }
            },
            onDelete = ::confirmDelete
        )

        root.findViewById<RecyclerView>(R.id.libraryRecyclerView).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@LibraryFragment.adapter
        }
    }

    private fun observeUiState(root: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(root, state) }
            }
        }
    }

    private fun render(root: View, state: LibraryUiState) {
        adapter.submitList(state.movies)

        loadingIndicator.visibility = if (state.isLoading) {
            View.VISIBLE
        } else {
            View.GONE
        }

        val isEmpty = !state.isLoading && state.movies.isEmpty()
        emptyLibraryText.visibility = if (isEmpty) {
            View.VISIBLE
        } else {
            View.GONE
        }

        addMovieButton.isEnabled = !state.isSaving

        val message = state.errorMessage ?: state.message
        if (message != null) {
            Snackbar.make(root, message, Snackbar.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    private fun showMovieDetails(movie: Movie) {
        findNavController().navigate(
            R.id.action_libraryFragment_to_movieDetailsFragment,
            MovieDetailsFragment.argumentsFor(movie)
        )
    }

    private fun confirmDelete(movie: Movie) {
        val localId = movie.localId ?: return

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Изтриване на заглавие")
            .setMessage("Да изтрием ли \"${movie.title}\" от библиотеката?")
            .setNegativeButton("Отказ", null)
            .setPositiveButton("Изтрий") { _, _ ->
                viewModel.deleteMovie(localId)
            }.show()
    }

    private fun openEditor(localId: Long) {
        findNavController().navigate(
            R.id.action_libraryFragment_to_movieFormFragment,
            bundleOf(MovieFormFragment.ARG_MOVIE_ID to localId)
        )
    }
}