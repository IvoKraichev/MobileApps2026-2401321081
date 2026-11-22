package com.myapp.cinescope.ui.catalog

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.myapp.cinescope.CineScopeApplication
import com.myapp.cinescope.R
import com.myapp.cinescope.domain.model.MediaType
import com.myapp.cinescope.domain.model.Movie
import com.myapp.cinescope.ui.library.LibraryViewModel
import com.myapp.cinescope.ui.library.LibraryViewModelFactory
import kotlinx.coroutines.launch

class CatalogFragment : Fragment(R.layout.fragment_catalog) {
    private val catalogViewModel: CatalogViewModel by viewModels {
        val app = requireActivity().application as CineScopeApplication
        CatalogViewModelFactory(app.appContainer.catalogRepository)
    }

    private val libraryViewModel: LibraryViewModel by activityViewModels {
        val app = requireActivity().application as CineScopeApplication
        LibraryViewModelFactory(app.appContainer.movieRepository)
    }

    private lateinit var root: View
    private lateinit var searchInput: TextInputEditText
    private lateinit var progress: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var retryButton: MaterialButton
    private lateinit var adapter: CatalogMovieAdapter
    private lateinit var catalogRecyclerView: RecyclerView
    private var lastSelectedMediaType: MediaType? = null
    private var filterAnimationVersion = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        root = view
        searchInput = view.findViewById(R.id.catalogSearchInput)
        val mediaTypeFilterGroup =
            view.findViewById<MaterialButtonToggleGroup>(R.id.mediaTypeFilterGroup)
        progress = view.findViewById(R.id.catalogProgress)
        statusText = view.findViewById(R.id.catalogStatus)
        retryButton = view.findViewById(R.id.retryButton)

        configureFilter(mediaTypeFilterGroup)
        configureList(view)

        lastSelectedMediaType = catalogViewModel.uiState.value.selectedMediaType

        view.findViewById<MaterialButton>(R.id.searchButton)
            .setOnClickListener {
                catalogViewModel.search(
                    searchInput.text?.toString().orEmpty()
                )
            }

        retryButton.setOnClickListener {
            catalogViewModel.retry()
        }

        observeCatalog()
        observeLibraryWrites()
    }

    private fun configureFilter(group: MaterialButtonToggleGroup) {
        val selectedButtonId = when (
            catalogViewModel.uiState.value.selectedMediaType
        ) {
            MediaType.MOVIE -> R.id.filterMovies
            MediaType.TV_SHOW -> R.id.filterTvShows
            null -> R.id.filterAll
        }

        group.check(selectedButtonId)

        group.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            val mediaType = when (checkedId) {
                R.id.filterMovies -> MediaType.MOVIE
                R.id.filterTvShows -> MediaType.TV_SHOW
                else -> null
            }

            catalogViewModel.setMediaTypeFilter(mediaType)
        }
    }

    private fun configureList(view: View) {
        adapter = CatalogMovieAdapter { movie ->
            libraryViewModel.saveMovie(movie)
            Snackbar.make(
                root,
                "Запазвам заглавието в личната библиотека...",
                Snackbar.LENGTH_SHORT
            ).show()
        }

        catalogRecyclerView = view.findViewById(R.id.catalogRecyclerView)

        catalogRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@CatalogFragment.adapter
            itemAnimator = null
        }
    }

    private fun observeCatalog() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                catalogViewModel.uiState.collect { state ->
                    val filterChanged = state.selectedMediaType != lastSelectedMediaType

                    if (filterChanged) {
                        lastSelectedMediaType = state.selectedMediaType
                        animateFilteredResults(state.visibleResults)
                    } else {
                        adapter.submitList(state.visibleResults)
                    }

                    progress.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE

                    val hasError = state.errorMessage != null
                    val noResults =
                        !state.isLoading && !hasError && state.visibleResults.isEmpty()

                    statusText.visibility =
                        if (hasError || noResults) View.VISIBLE else View.GONE
                    retryButton.visibility =
                        if (hasError) View.VISIBLE else View.GONE

                    statusText.text = when {
                        hasError -> state.errorMessage
                        noResults && state.query.isNotBlank() ->
                            "Няма намерени заглавия."
                        noResults ->
                            "Няма заглавия за избрания филтър."
                        else -> ""
                    }

                    if (searchInput.text?.toString() != state.query) {
                        searchInput.setText(state.query)
                        searchInput.setSelection(searchInput.text?.length ?: 0)
                    }
                }
            }
        }
    }

    private fun observeLibraryWrites() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                libraryViewModel.uiState.collect { state ->
                    val message = state.errorMessage ?: state.message
                    if (message != null) {
                        Snackbar.make(root, message, Snackbar.LENGTH_LONG).show()
                        libraryViewModel.clearMessages()
                    }
                }
            }
        }
    }

    private fun animateFilteredResults(results: List<Movie>) {
        val animationVersion = ++filterAnimationVersion

        catalogRecyclerView.animate().cancel()

        catalogRecyclerView
            .animate()
            .alpha(0f)
            .setDuration(100)
            .withEndAction {
                if (animationVersion != filterAnimationVersion) {
                    return@withEndAction
                }

                adapter.submitList(results) {
                    if (animationVersion != filterAnimationVersion) {
                        return@submitList
                    }

                    catalogRecyclerView
                        .animate()
                        .alpha(1f)
                        .setDuration(120)
                        .start()
                }
            }.start()
    }
}