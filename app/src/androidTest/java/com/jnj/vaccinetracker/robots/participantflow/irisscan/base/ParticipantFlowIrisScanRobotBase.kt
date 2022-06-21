package com.jnj.vaccinetracker.robots.participantflow.irisscan.base

import androidx.test.espresso.Espresso.onView
import com.jnj.vaccinetracker.robots.base.BaseRobot
import de.codecentric.androidtestktx.espresso.extensions.buttonContaining
import de.codecentric.androidtestktx.espresso.extensions.click
import de.codecentric.androidtestktx.espresso.extensions.textContaining


abstract class ParticipantFlowIrisScanRobotBase : BaseRobot() {
    init {
        waitUntilProgressBarGone()
    }

    fun loadImage(): Boolean {
        try {
            waitForView(buttonContaining("load image"))
                .perform(click)
        } catch (ex: Exception) {
            //license might have failed to accept so skip it
            return false
        }
        waitForView(textContaining("successful"))
        return true
    }

    fun skip() {
        onView(buttonContaining("skip")).perform(click)
    }

    public override fun submit() {
        super.submit()
    }
}
