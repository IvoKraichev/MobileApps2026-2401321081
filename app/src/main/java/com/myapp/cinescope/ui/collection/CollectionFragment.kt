package com.myapp.cinescope.ui.collection

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.myapp.cinescope.CineScopeApplication
import com.myapp.cinescope.R
import com.myapp.cinescope.domain.model.Movie
import com.myapp.cinescope.ui.details.MovieDetailsFragment
import com.myapp.cinescope.ui.library.LibraryMovieAdapter
import com.myapp.cinescope.ui.library.LibraryViewModel
import com.myapp.cinescope.ui.library.LibraryViewModelFactory
import kotlinx.coroutines.launch

class CollectionFragment : Fragment(R.layout.fragment_collection) {
    private val viewModel: LibraryViewModel by activityViewModels {
        val app = requireActivity().application as CineScopeApplication
        LibraryViewModelFactory(app.appContainer.movieRepository)
    }

    private lateinit var root: View
    private lateinit var titleText: TextView
    private lateinit var emptyMessage: TextView
    private lateinit var progress: ProgressBar
    private lateinit var adapter: LibraryMovieAdapter

    private val collectionType: String
        get() = requireArguments()
            .getString(ARG_COLLECTION_TYPE, TYPE_FAVORITES)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        root = view
        titleText = view.findViewById(R.id.collectionTitle)
        emptyMessage = view.findViewById(R.id.collectionEmptyMessage)
        progress = view.findViewById(R.id.collectionProgress)

        titleText.text = when (collectionType) {
            TYPE_WATCHLIST -> getString(R.string.watchlist_title)
            else -> getString(R.string.favorites_title)
        }

        emptyMessage.text = when (collectionType) {
            TYPE_WATCHLIST -> getString(R.string.watchlist_empty)
            else -> getString(R.string.favorites_empty)
        }

        configureList(view)
        observeLibrary()
    }

    private fun configureList(view: View) {
        adapter = LibraryMovieAdapter(
            onOpen = ::openDetails,
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

        view.findViewById<RecyclerView>(R.id.collectionRecyclerView).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@CollectionFragment.adapter
        }
    }

    private fun observeLibrary() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val movies = when (collectionType) {
                        TYPE_WATCHLIST -> state.watchlistMovies
                        else -> state.favoriteMovies
                    }

                    adapter.submitList(movies)
                    progress.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE
                    emptyMessage.visibility =
                        if (!state.isLoading && movies.isEmpty()) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    val message = state.errorMessage ?: state.message
                    if (message != null) {
                        Snackbar.make(root, message, Snackbar.LENGTH_LONG).show()
                        viewModel.clearMessages()
                    }
                }
            }
        }
    }

    private fun openDetails(movie: Movie) {
        val actionId = when (collectionType) {
            TYPE_WATCHLIST ->
                R.id.action_watchlistFragment_to_movieDetailsFragment
            else ->
                R.id.action_favoritesFragment_to_movieDetailsFragment
        }

        findNavController().navigate(
            actionId,
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
            }
            .show()
    }

    companion object {
        private const val ARG_COLLECTION_TYPE = "collectionType"
        private const val TYPE_FAVORITES = "FAVORITES"
        private const val TYPE_WATCHLIST = "WATCHLIST"
    }
}