package com.jnj.vaccinetracker.robots.register

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import com.jnj.vaccinetracker.robots.base.BaseRobot
import de.codecentric.androidtestktx.espresso.extensions.buttonContaining
import de.codecentric.androidtestktx.espresso.extensions.click
import de.codecentric.androidtestktx.espresso.extensions.textContaining

fun registerSuccess(func: RegisterSuccessRobot.() -> Unit) = RegisterSuccessRobot().apply(func)

infix fun RegisterSuccessRobot.verifyThat(fn: RegisterSuccessRobotResult.() -> Unit) = RegisterSuccessRobotResult().apply(fn)


class RegisterSuccessRobot : BaseRobot() {
    init {
        onView(textContaining("success"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    fun continueWithVisit() {
        onView(buttonContaining("visit"))
            .inRoot(isDialog())
            .perform(click)
    }

    fun finishWorkflow() {
        onView(buttonContaining("finish"))
            .inRoot(isDialog())
            .perform(click)
    }

    fun verifyIsHome() {
        waitForView(textContaining("welcome"))
    }
}

class RegisterSuccessRobotResult {
}