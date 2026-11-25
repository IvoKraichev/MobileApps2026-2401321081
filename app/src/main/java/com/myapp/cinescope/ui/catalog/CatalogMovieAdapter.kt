package com.myapp.cinescope.ui.catalog

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
import com.myapp.cinescope.ui.images.loadTmdbPoster
import com.myapp.cinescope.ui.common.bindMovieRatings

class CatalogMovieAdapter(
    private val onAddToLibrary: (Movie) -> Unit,
    private val onOpenDetails: (Movie) -> Unit
) : ListAdapter<Movie, CatalogMovieAdapter.MovieViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_catalog_movie, parent, false)

        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MovieViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView =
            itemView.findViewById(R.id.catalogMovieTitle)
        private val metadata: TextView =
            itemView.findViewById(R.id.catalogMovieMetadata)
        private val ratings: TextView =
            itemView.findViewById(R.id.catalogMovieRatings)
        private val overview: TextView =
            itemView.findViewById(R.id.catalogMovieOverview)
        private val poster: ImageView =
            itemView.findViewById(R.id.catalogMoviePoster)
        private val addButton: MaterialButton =
            itemView.findViewById(R.id.addToLibraryButton)

        private val detailsButton: MaterialButton =
            itemView.findViewById(R.id.openMovieDetailsButton)

        fun bind(movie: Movie) {
            title.text = movie.title

            val type = when (movie.mediaType) {
                MediaType.MOVIE -> "Филм"
                MediaType.TV_SHOW -> "Сериал"
            }

            val year = movie.releaseDate?.take(4) ?: "Без година"
            val genres = movie.genreNames
                .takeIf(String::isNotBlank)
                ?.let { " · $it" }
                .orEmpty()

            metadata.text = "$type · $year$genres"
            ratings.bindMovieRatings(movie)

            overview.text = movie.overview.ifBlank {
                "Няма описание"
            }

            poster.loadTmdbPoster(
                posterPath = movie.posterPath,
                title = movie.title
            )

            addButton.setOnClickListener {
                onAddToLibrary(movie)
            }

            detailsButton.setOnClickListener {
                onOpenDetails(movie)
            }
        }
    }

    private companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<Movie>() {
            override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean {
                return oldItem.tmdbId == newItem.tmdbId &&
                        oldItem.mediaType == newItem.mediaType
            }

            override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean {
                return oldItem == newItem
            }
        }
    }
}