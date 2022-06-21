package de.codecentric.androidtestktx.espresso

import android.app.Activity
import android.content.Intent

open class EspressoRobot<T : Activity>(protected val activityTestRule: MockableTestRule<T>, autoStart: Boolean) {
    init {
        if (autoStart) {
            launchActivity()
        }
    }

    fun launchActivity() {
        activityTestRule.launchActivity(Intent())
    }
}