package de.codecentric.androidtestktx.espresso.extensions

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.Button
import androidx.core.view.isVisible
import androidx.test.espresso.*
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.util.HumanReadables
import androidx.test.espresso.util.TreeIterables
import de.codecentric.androidtestktx.common.seconds
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.any
import org.hamcrest.Matcher
import java.util.concurrent.TimeoutException
import kotlin.reflect.KClass

/**
 * Asserts whether the ListView with [textMatcher] is selected for a current adapter [Matcher]
 */
infix fun Matcher<View>.verifySelectionByTextTo(textMatcher: () -> Matcher<Any>) =
    onData(textMatcher()).inAdapterView(this).isSelected()

/**
 * Asserts the view with current [Matcher] with provided [ViewAssertion] from lambda.
 */
infix fun Matcher<View>.verifyThat(func: () -> ViewAssertion) {
    onView(this).check(func())
}

/**
 * Asserts the intent with current KClass with provided [Matcher] from lambda
 */
infix fun <T : Activity> KClass<T>.verifyThat(fn: Matcher<Intent>.() -> Unit) {
    fn(IntentMatchers.hasComponent(this.java.name))
}

/**
 * Asserts whether the view with current [Matcher] matches the text returned from a [textFn] lambda
 */
infix fun Matcher<View>.verifyText(textFn: () -> String) {
    verifyThat { ViewAssertions.matches(withText(textFn())) }
}


infix fun ViewAction.into(matcher: Matcher<View>) {
    Espresso.onView(matcher).perform(this)
}

infix fun ViewAction.onto(matcher: Matcher<View>) {
    Espresso.onView(matcher).perform(this)
}

infix fun ViewAction.on(matcher: Matcher<View>) {
    Espresso.onView(matcher).perform(this)
}

infix fun ViewAction.from(matcher: Matcher<View>) {
    Espresso.onView(matcher).perform(this)
}

fun view(viewId: Int): Matcher<View> = ViewMatchers.withId(viewId)

fun text(viewId: Int): Matcher<View> = view(viewId)

fun withTextHintContaining(hint: String): Matcher<View> = withHint(StringContainsLowerCase(hint))

fun field(viewId: Int): Matcher<View> = view(viewId)

fun button(viewId: Int): Matcher<View> = view(viewId)
fun buttonContaining(text: String): Matcher<View> = allOf(ofViewType<Button>(), textContaining(text), visible())

fun text(text: String): Matcher<View> = withText(text)

fun textContaining(text: String): Matcher<View> = withText(StringContainsLowerCase(text))


fun View.findViewWithMatcher(matcher: Matcher<View>) = TreeIterables.breadthFirstViewTraversal(this).find { matcher.matches(it) }

/**
 * Perform action of waiting for a certain view within a single root view
 * @param matcher Generic Matcher used to find our view
 */
fun searchFor(matcher: Matcher<View>): ViewAction {

    return object : ViewAction {

        override fun getConstraints(): Matcher<View> {
            return isRoot()
        }

        override fun getDescription(): String {
            return "searching for view $matcher in the root view"
        }

        override fun perform(uiController: UiController, view: View) {

            val containsView = view.findViewWithMatcher(matcher) != null
            if (containsView)
                return

            throw NoMatchingViewException.Builder()
                .withRootView(view)
                .withViewMatcher(matcher)
                .build()
        }
    }
}

/**
 * A [ViewAction] that waits up to [timeout] milliseconds for a [View] with certain [condition]
 */
class WaitUntilConditionAction(private val matcher: Matcher<View>, private val condition: (View) -> Boolean, private val timeout: Long) : ViewAction {

    override fun getConstraints(): Matcher<View> {
        return any(View::class.java)
    }

    override fun getDescription(): String {
        return "wait up to $timeout milliseconds for the view to satisfy condition"
    }

    override fun perform(uiController: UiController, view: View) {
        val endTime = System.currentTimeMillis() + timeout
        var matchedView: View? = null
        do {
            matchedView = matchedView ?: view.findViewWithMatcher(matcher)
            if (matchedView != null && condition(matchedView)){
                return
            }
            uiController.loopMainThreadForAtLeast(50)
        } while (System.currentTimeMillis() < endTime)

        throw PerformException.Builder()
            .withActionDescription(description)
            .withCause(TimeoutException("Waited $timeout milliseconds"))
            .withViewDescription(HumanReadables.describe(view))
            .build()
    }
}

/**
 * A [ViewAction] that waits up to [timeout] milliseconds for a [View]'s visibility value to change to [View.GONE].
 */
class WaitUntilGoneAction(private val matcher: Matcher<View>, private val timeout: Long) : ViewAction {

    override fun getConstraints(): Matcher<View> {
        return any(View::class.java)
    }

    override fun getDescription(): String {
        return "wait up to $timeout milliseconds for the view to be gone"
    }

    override fun perform(uiController: UiController, view: View) {
        val endTime = System.currentTimeMillis() + timeout
        var matchedView: View? = null
        do {
            matchedView = matchedView ?: view.findViewWithMatcher(matcher)
            if (matchedView != null && !matchedView.isVisible) {
                return
            }
            uiController.loopMainThreadForAtLeast(50)
        } while (System.currentTimeMillis() < endTime)

        throw PerformException.Builder()
            .withActionDescription(description)
            .withCause(TimeoutException("Waited $timeout milliseconds"))
            .withViewDescription(HumanReadables.describe(view))
            .build()
    }
}

/**
 * @return a [WaitUntilGoneAction] instance created with the given [timeout] parameter.
 */
fun waitUntilGone(matcher: Matcher<View>, timeout: Long = 5.seconds): ViewAction {
    return WaitUntilGoneAction(matcher, timeout)
}

fun waitUntilCondition(matcher: Matcher<View>, condition: (View) -> Boolean, timeout: Long = 5.seconds): ViewAction {
    return WaitUntilConditionAction(matcher, condition, timeout)
}