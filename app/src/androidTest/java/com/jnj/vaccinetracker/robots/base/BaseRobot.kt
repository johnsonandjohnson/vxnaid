package com.jnj.vaccinetracker.robots.base

import android.view.View
import android.widget.ProgressBar
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiSelector
import de.codecentric.androidtestktx.common.instrumentation
import de.codecentric.androidtestktx.espresso.extensions.*
import org.hamcrest.Matcher
import java.lang.Thread.sleep

/**
 * @see <a href="https://academy.realm.io/posts/kau-jake-wharton-testing-robots">Robot Pattern</a>
 */
open class BaseRobot {
    protected fun waitUntilProgressBarGone(): ViewInteraction = onView(isRoot()).perform(waitUntilGone(ofViewType<ProgressBar>()))
    protected fun doOnView(matcher: Matcher<View>, vararg actions: ViewAction) {
        actions.forEach {
            waitForView(matcher).perform(it)
        }
    }

    protected fun assertOnView(matcher: Matcher<View>, vararg assertions: ViewAssertion) {
        assertions.forEach {
            waitForView(matcher).check(it)
        }
    }

    protected open fun submit() {
        waitForView(buttonContaining("submit"))
            .perform(de.codecentric.androidtestktx.espresso.extensions.click)
    }

    /**
     * Perform action of implicitly waiting for a certain view.
     * This differs from EspressoExtensions.searchFor in that,
     * upon failure to locate an element, it will fetch a new root view
     * in which to traverse searching for our @param match
     *
     * @param viewMatcher ViewMatcher used to find our view
     */
    protected fun waitForView(
        viewMatcher: Matcher<View>,
        waitMillis: Int = 2500,
        waitMillisPerTry: Long = 100,
    ): ViewInteraction {

        // Derive the max tries
        val maxTries = waitMillis / waitMillisPerTry.toInt()

        var tries = 0

        for (i in 0..maxTries)
            try {
                // Track the amount of times we've tried
                tries++

                // Search the root for the view
                onView(isRoot())
                    .perform(searchFor(viewMatcher))

                // If we're here, we found our view. Now return it
                return onView(viewMatcher)

            } catch (e: Exception) {

                if (tries == maxTries) {
                    throw e
                }
                sleep(waitMillisPerTry)
            }

        throw Exception("Error finding a view matching $viewMatcher")
    }

    protected fun dropDownClick(matcher: Matcher<out Any>, position: Int) {
        onData(matcher)
            .inRoot(isPlatformPopup())
            .atPosition(position)
            .check(matches(isDisplayed()))
            .perform(click())
    }

    protected fun allowPermissions() {
        try {
            waitFor(1000)
            val device = UiDevice.getInstance(instrumentation)
            val allowPermissions = device.findObject(UiSelector()
                .clickable(true)
                .checkable(false)
                .index(1))
            if (allowPermissions.exists()) {
                allowPermissions.click()
            }
        } catch (e: UiObjectNotFoundException) {
            println("There is no permissions dialog to interact with")
        }
    }
}