package de.codecentric.androidtestktx.espresso.extensions

import android.view.View
import androidx.annotation.StringRes
import androidx.test.espresso.matcher.ViewMatchers
import de.codecentric.androidtestktx.common.appContext
import de.codecentric.androidtestktx.common.stringOf
import org.hamcrest.Description
import org.hamcrest.Factory
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.SubstringMatcher
import java.util.*

/**
 * Returns a [Matcher] for a given [viewId]
 */
fun viewById(viewId: Int): Matcher<View> = ViewMatchers.withId(viewId)

/**
 * Returns a [Matcher] for a string value fetched from a resource for [stringResId]. Value returned from a resource
 * is localized on a currently running localization.
 */
fun viewByText(@StringRes stringResId: Int): Matcher<View> =
    viewByText(appContext stringOf stringResId)

/**
 * Returns a [Matcher] for a given [text]
 */
fun viewByText(text: String): Matcher<View> = ViewMatchers.withText(text)

/**
 * Returns a [Matcher] for a first visible item by id.
 */
fun firstVisibleItemById(viewId: Int): Matcher<View> = firstVisibleItem(
    viewById(viewId)
)

/**
 * Returns a [Matcher] for a first visible item by text.
 */
fun firstVisibleItemByText(text: String): Matcher<View> = firstVisibleItem(
    viewByText(text)
)

fun firstVisibleItem(matcher: Matcher<View>): Matcher<View> {
    return object : TypeSafeMatcher<View>() {

        var found = false

        override fun describeTo(description: Description?) {
            description?.appendText("Matcher first ($matcher).")
        }

        override fun matchesSafely(item: View?): Boolean {
            return if (!found && allOf(matcher, ViewMatchers.isCompletelyDisplayed()).matches(item)) {
                found = true
                true
            } else {
                false
            }
        }
    }
}


/**
 * Tests if the argument is a string that contains a substring.
 */
class StringContainsLowerCase(substring: String) : SubstringMatcher(substring) {
    override fun evalSubstringOf(s: String): Boolean {
        return s.toLowerCase(Locale.ROOT).indexOf(substring.toLowerCase(Locale.ROOT)) >= 0
    }

    override fun relationship(): String {
        return "containing lower case"
    }

    companion object {
        /**
         * Creates a matcher that matches if the examined [String] contains the specified
         * [String] anywhere.
         *
         *
         * For example:
         * <pre>assertThat("myStringOfNote", containsString("ring"))</pre>
         *
         * @param substring
         * the substring that the returned matcher will expect to find within any examined string
         */
        @Factory
        fun containsStringLowerCase(substring: String): Matcher<String> {
            return StringContainsLowerCase(substring)
        }
    }
}

inline fun <reified T : View> ofViewType(): Matcher<View> = instanceOf(T::class.java)