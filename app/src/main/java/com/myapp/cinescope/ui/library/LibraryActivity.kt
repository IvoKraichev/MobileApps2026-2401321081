package com.myapp.cinescope.ui.library

import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.view.WindowInsets
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

class LibraryActivity : AppCompatActivity() {
    private val viewModel: LibraryViewModel by viewModels {
        val app = application as CineScopeApplication
        LibraryViewModelFactory(app.appContainer.movieRepository)
    }

    private lateinit var root: View
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var emptyLibraryText: TextView
    private lateinit var addMovieButton: MaterialButton
    private lateinit var adapter: LibraryMovieAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_library)

        root = findViewById(R.id.libraryRoot)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        emptyLibraryText = findViewById(R.id.emptyLibraryText)
        addMovieButton = findViewById(R.id.addMovieButton)

        applySystemInsets()
        configureList()
        observeUiState()

        addMovieButton.setOnClickListener {
            Snackbar.make(
                root,
                "Формата за добавяне ще бъде следващата стъпка.",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun applySystemInsets() {
        val initialLeft = root.paddingLeft
        val initialTop = root.paddingTop
        val initialRight = root.paddingRight
        val initialBottom = root.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            view.setPadding(
                initialLeft + systemBars.left,
                initialTop + systemBars.top,
                initialRight + systemBars.right,
                initialBottom + systemBars.bottom
            )

            insets
        }
    }

    private fun configureList() {
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

        findViewById<RecyclerView>(R.id.libraryRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@LibraryActivity)
            adapter = this@LibraryActivity.adapter
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: LibraryUiState) {
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

        state.errorMessage?.let { message ->
            Snackbar.make(root, message, Snackbar.LENGTH_LONG).show()
            viewModel.clearMessages()
        } ?: state.message?.let { message ->
            Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    private fun showMovieDetails(movie: Movie) {
        movie.localId?.let(viewModel::selectMovie)

        val details = buildString {
            append(
                when (movie.mediaType) {
                    MediaType.MOVIE -> "Филм"
                    MediaType.TV_SHOW -> "Сериал"
                }
            )
            movie.releaseDate?.take(4)?.let { append(" · $it") }
            append("\n\n")
            append(movie.overview.ifBlank { "Няма описание." })
            movie.userRating?.let {
                append("\n\nМоята оценка: $it / 10")
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(movie.title)
            .setMessage(details)
            .setPositiveButton("Затвори", null)
            .setOnDismissListener {
                viewModel.clearSelectedMovie()
            }.show()
    }

    private fun confirmDelete(movie: Movie) {
        val localId = movie.localId ?: return

        MaterialAlertDialogBuilder(this)
            .setTitle("Изтриване на заглавие")
            .setMessage("Да изтрием ли \"${movie.title}\" от библиотеката?")
            .setNegativeButton("Отказ", null)
            .setPositiveButton("Изтрий") { _, _ ->
                viewModel.deleteMovie(localId)
            }.show()
    }
}