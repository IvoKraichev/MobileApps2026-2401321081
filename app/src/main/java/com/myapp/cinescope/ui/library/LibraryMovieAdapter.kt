package com.myapp.cinescope.ui.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.myapp.cinescope.R
import com.myapp.cinescope.domain.model.MediaType
import com.myapp.cinescope.domain.model.Movie
import com.myapp.cinescope.ui.common.bindMovieRatings
import com.myapp.cinescope.ui.images.loadTmdbPoster

class LibraryMovieAdapter(
    private val onOpen: (Movie) -> Unit,
    private val onFavorite: (Movie) -> Unit,
    private val onWatchlist: (Movie) -> Unit,
    private val onDelete: (Movie) -> Unit
): ListAdapter<Movie, LibraryMovieAdapter.MovieViewHolder>(MovieDiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_library_movie, parent, false)

        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MovieViewHolder(
        itemView: View
    ): RecyclerView.ViewHolder(itemView) {
        private val poster: ImageView =
            itemView.findViewById(R.id.libraryMoviePoster)
        private val title: TextView = itemView.findViewById(R.id.movieTitle)
        private val subtitle: TextView = itemView.findViewById(R.id.movieSubtitle)
        private val ratings: TextView = itemView.findViewById(R.id.libraryMovieRatings)
        private val favoriteButton: MaterialButton = itemView.findViewById(R.id.favoriteButton)
        private val watchlistButton: MaterialButton = itemView.findViewById(R.id.watchlistButton)
        private val deleteButton: MaterialButton = itemView.findViewById(R.id.deleteMovieButton)

        fun bind(movie: Movie) {
            poster.loadTmdbPoster(
                posterPath = movie.posterPath,
                title = movie.title,
                size = "w342"
            )

            title.text = movie.title

            val typeLabel = when (movie.mediaType) {
                MediaType.MOVIE -> "Филм"
                MediaType.TV_SHOW -> "Сериал"
            }
            val year = movie.releaseDate?.take(4) ?: "Без година"

            subtitle.text = "$typeLabel · $year"
            ratings.bindMovieRatings(movie)

            favoriteButton.text = if (movie.isFavorite) {
                "Премахни от любими"
            } else {
                "Добави в любими"
            }

            watchlistButton.text = if (movie.isInWatchlist) {
                "Премахни от списъка"
            } else {
                "Добави за гледане"
            }

            itemView.setOnClickListener {
                onOpen(movie)
            }
            favoriteButton.setOnClickListener {
                onFavorite(movie)
            }
            watchlistButton.setOnClickListener {
                onWatchlist(movie)
            }
            deleteButton.setOnClickListener {
                onDelete(movie)
            }
        }
    }

    private companion object {
        val MovieDiffCallback = object : DiffUtil.ItemCallback<Movie>() {
            override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean {
                return oldItem.localId == newItem.localId
            }

            override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean {
                return oldItem == newItem
            }
        }
    }
}