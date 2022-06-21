package com.jnj.vaccinetracker.robots.participantflow

import androidx.test.espresso.action.ViewActions.click
import com.jnj.vaccinetracker.robots.base.BaseRobot
import de.codecentric.androidtestktx.espresso.extensions.buttonContaining

fun participationFlowIntro(func: ParticipationFlowIntroRobot.() -> Unit) = ParticipationFlowIntroRobot().apply(func)

class ParticipationFlowIntroRobot : BaseRobot() {
    fun startWorkFlow() {
        waitForView(buttonContaining("start"))
            .perform(click())
    }
}
