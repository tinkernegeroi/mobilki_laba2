package com.example.skyway

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises observable form behavior and Android activity recreation, rather than internals. */
@RunWith(AndroidJUnit4::class)
class FlightSearchScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun swappingTwiceRestoresTheRoute() {
        val moscow = compose.activity.getString(R.string.city_moscow)
        val sochi = compose.activity.getString(R.string.city_sochi)

        compose.onNodeWithTag("origin").assertTextContains(moscow)
        compose.onNodeWithTag("destination").assertTextContains(sochi)
        compose.onNodeWithTag("swap_route").performScrollTo().performClick()
        compose.onNodeWithTag("origin").assertTextContains(sochi)
        compose.onNodeWithTag("destination").assertTextContains(moscow)

        compose.onNodeWithTag("swap_route").performClick()
        compose.onNodeWithTag("origin").assertTextContains(moscow)
        compose.onNodeWithTag("destination").assertTextContains(sochi)
    }

    @Test
    fun identicalCitiesShowAValidationErrorInsteadOfSearch() {
        val moscow = compose.activity.getString(R.string.city_moscow)
        compose.onNodeWithTag("destination").performScrollTo().performTextReplacement(moscow)
        dismissCitySuggestionsAndKeyboard()
        compose.onNodeWithTag("search_flights").performScrollTo().performClick()

        compose.onNodeWithText(compose.activity.getString(R.string.error_same_city)).assertExists()
        compose.onNodeWithText(compose.activity.getString(R.string.notice_not_implemented)).assertDoesNotExist()
    }

    @Test
    fun passengersStayWithinOneAndNine() {
        compose.onNodeWithTag("passengers_minus").performScrollTo().assertIsNotEnabled()
        repeat(8) { compose.onNodeWithTag("passengers_plus").performClick() }
        compose.onNodeWithTag("passenger_count")
            .assertTextEquals(compose.activity.getString(R.string.passengers_number, 9))
        compose.onNodeWithTag("passengers_plus").assertIsNotEnabled()

        repeat(8) { compose.onNodeWithTag("passengers_minus").performClick() }
        compose.onNodeWithTag("passenger_count")
            .assertTextEquals(compose.activity.getString(R.string.passengers_number, 1))
        compose.onNodeWithTag("passengers_minus").assertIsNotEnabled()
    }

    @Test
    fun oneWayHidesReturnDateAndRoundTripRestoresIt() {
        compose.onNodeWithTag("return_date").assertExists()
        compose.onNodeWithTag("one_way").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("return_date").assertDoesNotExist()
        compose.onNodeWithTag("departure_date").assertExists()

        compose.onNodeWithTag("round_trip").performClick().assertIsSelected()
        compose.onNodeWithTag("return_date").assertExists()
    }

    @Test
    fun formSurvivesActivityRecreation() {
        // A custom city exercises saved text as well as resource-backed city selections.
        val customCity = "Kazan"
        compose.onNodeWithTag("origin").performScrollTo().performTextReplacement(customCity)
        dismissCitySuggestionsAndKeyboard()
        compose.onNodeWithTag("one_way").performScrollTo().performClick()
        compose.onNodeWithTag("passengers_plus").performScrollTo().performClick().performClick()
        compose.onNodeWithTag("class_1").performScrollTo().performClick()
        compose.onNodeWithTag("direct_only").performScrollTo().performClick()

        compose.activityRule.scenario.recreate()
        compose.waitForIdle()

        compose.onNodeWithTag("origin").assertTextContains(customCity)
        compose.onNodeWithTag("destination")
            .assertTextContains(compose.activity.getString(R.string.city_sochi))
        compose.onNodeWithTag("one_way").assertIsSelected()
        compose.onNodeWithTag("return_date").assertDoesNotExist()
        compose.onNodeWithTag("passenger_count")
            .assertTextEquals(compose.activity.getString(R.string.passengers_number, 3))
        compose.onNodeWithTag("class_1").assertIsSelected()
        compose.onNodeWithTag("direct_only").assertIsOn()
    }

    @Test
    fun validSearchShowsNotImplementedSnackbar() {
        compose.onNodeWithTag("search_flights").performScrollTo().performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.notice_not_implemented))
            .assertIsDisplayed()
    }

    @Test
    fun favoritesShowNotImplementedSnackbar() {
        compose.onNodeWithTag("nav_favorites").performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.notice_not_implemented))
            .assertIsDisplayed()
    }

    private fun dismissCitySuggestionsAndKeyboard() {
        // Replacing editable text expands its suggestions popup. Back dismisses that
        // popup; closeSoftKeyboard then removes the IME without navigating the activity.
        Espresso.pressBack()
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
    }
}
