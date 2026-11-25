package com.myapp.cinescope.ui.details

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.myapp.cinescope.CineScopeApplication
import com.myapp.cinescope.R
import com.myapp.cinescope.domain.model.MediaType
import com.myapp.cinescope.domain.model.Movie
import com.myapp.cinescope.ui.common.bindMovieRatings
import com.myapp.cinescope.ui.images.loadTmdbPoster
import com.myapp.cinescope.ui.library.LibraryViewModel
import com.myapp.cinescope.ui.library.LibraryViewModelFactory
import com.myapp.cinescope.ui.library.MovieFormFragment
import kotlinx.coroutines.launch

class MovieDetailsFragment : Fragment(R.layout.fragment_movie_details) {
    private val libraryViewModel: LibraryViewModel by activityViewModels {
        val app = requireActivity().application as CineScopeApplication
        LibraryViewModelFactory(app.appContainer.movieRepository)
    }

    private lateinit var movie: Movie
    private lateinit var posterImage: ImageView
    private lateinit var titleText: TextView
    private lateinit var metadataText: TextView
    private lateinit var ratingsText: TextView
    private lateinit var genresText: TextView
    private lateinit var overviewText: TextView
    private lateinit var personalInfoText: TextView
    private lateinit var addButton: MaterialButton
    private lateinit var favoriteButton: MaterialButton
    private lateinit var watchlistButton: MaterialButton
    private lateinit var editButton: MaterialButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        movie = readMovieArguments(requireArguments())

        posterImage = view.findViewById(R.id.detailsPoster)
        titleText = view.findViewById(R.id.detailsTitle)
        metadataText = view.findViewById(R.id.detailsMetadata)
        ratingsText = view.findViewById(R.id.detailsRatings)
        genresText = view.findViewById(R.id.detailsGenres)
        overviewText = view.findViewById(R.id.detailsOverview)
        personalInfoText = view.findViewById(R.id.detailsPersonalInfo)
        addButton = view.findViewById(R.id.addDetailsToLibraryButton)
        favoriteButton = view.findViewById(R.id.detailsFavoriteButton)
        watchlistButton = view.findViewById(R.id.detailsWatchlistButton)
        editButton = view.findViewById(R.id.editDetailsButton)

        view.findViewById<MaterialButton>(R.id.backButton).setOnClickListener {
            findNavController().navigateUp()
        }

        view.findViewById<MaterialButton>(R.id.shareMovieButton)
            .setOnClickListener { shareMovie() }

        addButton.setOnClickListener {
            libraryViewModel.saveMovie(movie)
        }

        favoriteButton.setOnClickListener {
            movie.localId?.let { id ->
                libraryViewModel.setFavorite(id, !movie.isFavorite)
            }
        }

        watchlistButton.setOnClickListener {
            movie.localId?.let { id ->
                libraryViewModel.setInWatchlist(id, !movie.isInWatchlist)
            }
        }

        editButton.setOnClickListener {
            movie.localId?.let { id ->
                findNavController().navigate(
                    R.id.action_movieDetailsFragment_to_movieFormFragment,
                    bundleOf("movieId" to id)
                )
            }
        }

        renderMovie(movie)
        observeLibrary()
        observeSaveResult()
    }

    private fun observeLibrary() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                libraryViewModel.uiState.collect { state ->
                    val localId = movie.localId
                    if (localId != null) {
                        state.movies.firstOrNull { it.localId == localId }?.let {
                            movie = it
                            renderMovie(it)
                        }
                    }

                    val message = state.errorMessage ?: state.message
                    if (message != null) {
                        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
                        libraryViewModel.clearMessages()
                    }
                }
            }
        }
    }

    private fun renderMovie(value: Movie) {
        posterImage.loadTmdbPoster(
            posterPath = value.posterPath,
            title = value.title,
            size = "w500"
        )

        titleText.text = value.title

        val mediaLabel = when (value.mediaType) {
            MediaType.MOVIE -> "Филм"
            MediaType.TV_SHOW -> "Сериал"
        }
        val year = value.releaseDate?.take(4) ?: "Без година"

        metadataText.text = "$mediaLabel · $year"
        ratingsText.bindMovieRatings(value)
        genresText.text = value.genreNames.ifBlank { "Жанровете не са посочени." }
        overviewText.text = value.overview.ifBlank { "Няма описание." }

        personalInfoText.text = buildString {
            append("Лична оценка: ")
            append(value.userRating?.let { "$it / 10" } ?: "няма")

            if (value.personalNotes.isNotBlank()) {
                append("\nБележки: ")
                append(value.personalNotes)
            }
        }

        personalInfoText.visibility = View.VISIBLE

        val isInLibrary = value.localId != null
        addButton.visibility = if (isInLibrary) View.GONE else View.VISIBLE
        favoriteButton.visibility = if (isInLibrary) View.VISIBLE else View.GONE
        watchlistButton.visibility = if (isInLibrary) View.VISIBLE else View.GONE
        editButton.visibility = if (isInLibrary) View.VISIBLE else View.GONE

        favoriteButton.text = if (value.isFavorite) {
            "Премахни от любими"
        } else {
            "Добави в любими"
        }

        watchlistButton.text = if (value.isInWatchlist) {
            "Премахни от списъка за гледане"
        } else {
            "Добави за гледане"
        }
    }

    private fun shareMovie() {
        val typePath = when (movie.mediaType) {
            MediaType.MOVIE -> "movie"
            MediaType.TV_SHOW -> "tv"
        }

        val tmdbUrl = movie.tmdbId?.let {
            "\nhttps://www.themoviedb.org/$typePath/$it"
        }.orEmpty()

        val shareText = buildString {
            append(movie.title)
            append("\n")
            append(movie.overview.ifBlank { "Няма описание." })
            append(tmdbUrl)
            append("\n\nСподелено от CineScope")
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, movie.title)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        startActivity(
            Intent.createChooser(sendIntent, "Сподели заглавие")
        )
    }

    private fun readMovieArguments(args: Bundle): Movie {
        val mediaType = when (args.getString(ARG_MEDIA_TYPE)) {
            MediaType.TV_SHOW.name -> MediaType.TV_SHOW
            else -> MediaType.MOVIE
        }

        val tmdbId = args.getInt(ARG_TMDB_ID, -1)
            .takeIf { it >= 0 }

        val localId = args.getLong(ARG_LOCAL_ID, -1L)
            .takeIf { it >= 0L }

        val averageRating = args.getDouble(ARG_AVERAGE_RATING, -1.0)
            .takeIf { it >= 0.0 }

        val userRating = args.getFloat(ARG_USER_RATING, -1f)
            .takeIf { it >= 0f }

        return Movie(
            localId = localId,
            tmdbId = tmdbId,
            title = requireNotNull(args.getString(ARG_TITLE)),
            overview = args.getString(ARG_OVERVIEW).orEmpty(),
            mediaType = mediaType,
            releaseDate = args.getString(ARG_RELEASE_DATE),
            genreNames = args.getString(ARG_GENRES).orEmpty(),
            posterPath = args.getString(ARG_POSTER_PATH),
            averageRating = averageRating,
            userRating = userRating,
            isFavorite = args.getBoolean(ARG_IS_FAVORITE),
            isInWatchlist = args.getBoolean(ARG_IS_IN_WATCHLIST),
            personalNotes = args.getString(ARG_NOTES).orEmpty()
        )
    }

    private fun observeSaveResult() {
        val savedStateHandle =
            findNavController().currentBackStackEntry?.savedStateHandle ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                savedStateHandle
                    .getStateFlow<String?>(MovieFormFragment.SAVE_RESULT_KEY, null)
                    .collect { message ->
                        if (message != null) {
                            Snackbar.make(
                                requireView(),
                                message,
                                Snackbar.LENGTH_SHORT
                            ).show()

                            savedStateHandle.remove<String>(
                                MovieFormFragment.SAVE_RESULT_KEY
                            )
                        }
                    }
            }
        }
    }

    companion object {
        private const val ARG_LOCAL_ID = "localId"
        private const val ARG_TMDB_ID = "tmdbId"
        private const val ARG_TITLE = "title"
        private const val ARG_OVERVIEW = "overview"
        private const val ARG_MEDIA_TYPE = "mediaType"
        private const val ARG_RELEASE_DATE = "releaseDate"
        private const val ARG_GENRES = "genreNames"
        private const val ARG_POSTER_PATH = "posterPath"
        private const val ARG_AVERAGE_RATING = "averageRating"
        private const val ARG_USER_RATING = "userRating"
        private const val ARG_IS_FAVORITE = "isFavorite"
        private const val ARG_IS_IN_WATCHLIST = "isInWatchlist"
        private const val ARG_NOTES = "personalNotes"

        fun argumentsFor(movie: Movie): Bundle = bundleOf(
            ARG_LOCAL_ID to (movie.localId ?: -1L),
            ARG_TMDB_ID to (movie.tmdbId ?: -1),
            ARG_TITLE to movie.title,
            ARG_OVERVIEW to movie.overview,
            ARG_MEDIA_TYPE to movie.mediaType.name,
            ARG_RELEASE_DATE to movie.releaseDate,
            ARG_GENRES to movie.genreNames,
            ARG_POSTER_PATH to movie.posterPath,
            ARG_AVERAGE_RATING to (movie.averageRating ?: -1.0),
            ARG_USER_RATING to (movie.userRating ?: -1f),
            ARG_IS_FAVORITE to movie.isFavorite,
            ARG_IS_IN_WATCHLIST to movie.isInWatchlist,
            ARG_NOTES to movie.personalNotes
        )
    }
}