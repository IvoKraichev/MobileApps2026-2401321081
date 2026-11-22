package com.myapp.cinescope.ui.images

import android.widget.ImageView
import coil3.load
import coil3.request.crossfade
import coil3.request.placeholder
import coil3.request.error

fun ImageView.loadTmdbPoster(
    posterPath: String?,
    title: String,
    size: String = "w342"
) {
    contentDescription = "Постер: $title"

    val path = posterPath
        ?.trim()
        ?.takeIf(String::isNotEmpty)

    if (path == null) {
        setImageResource(android.R.drawable.ic_menu_report_image)
        return
    }

    val imageUrl =
        "https://image.tmdb.org/t/p/$size/${path.trimStart('/')}"

    load(imageUrl) {
        crossfade(true)
        placeholder(android.R.drawable.ic_menu_report_image)
        error(android.R.drawable.ic_menu_report_image)
    }
}