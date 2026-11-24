package com.myapp.cinescope.ui.common

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.myapp.cinescope.R
import com.myapp.cinescope.domain.model.Movie
import java.text.NumberFormat

fun TextView.bindMovieRatings(movie: Movie) {
    val ratings = SpannableStringBuilder()

    movie.averageRating?.let {
        ratings.appendRating(context, "TMDB", it)
    }

    text = ratings
    visibility = if (ratings.isEmpty()) View.GONE else View.VISIBLE
}

private fun SpannableStringBuilder.appendRating(
    context: android.content.Context,
    label: String,
    value: Double
) {
    if (length > 0) append("  ·  ")

    append("$label ")

    val starStart = length
    append("★")
    setSpan(
        ForegroundColorSpan(
            ContextCompat.getColor(context, R.color.cine_tertiary)
        ),
        starStart,
        length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )

    val number = NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }.format(value)

    append(" $number/10")
}