package com.myapp.cinescope

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @Test
    fun userCanAddMovieToLibrary() {
        val title = "Espresso CineScope ${System.currentTimeMillis()}"
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        try {
            // Отвори личната библиотека през долната навигация.
            onView(withId(R.id.libraryFragment))
                .perform(click())

            onView(withId(R.id.libraryTitle))
                .check(matches(isDisplayed()))

            // Отвори формата за ново заглавие.
            onView(withId(R.id.addMovieButton))
                .perform(click())

            onView(withId(R.id.formHeading))
                .check(matches(isDisplayed()))

            // Попълни задължителното поле и запиши.
            onView(withId(R.id.titleInput))
                .perform(
                    replaceText(title),
                    closeSoftKeyboard()
                )

            // Новият запис по подразбиране е от тип „Филм“.
            onView(withId(R.id.saveMovieButton))
                .perform(click())

            // След успешния запис формата се затваря и заглавието
            // трябва да се появи в списъка.
            onView(withText(title))
                .check(matches(isDisplayed()))
        } finally {
            scenario.close()
        }
    }
}