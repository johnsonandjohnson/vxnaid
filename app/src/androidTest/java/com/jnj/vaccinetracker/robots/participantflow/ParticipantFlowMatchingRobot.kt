package com.jnj.vaccinetracker.robots.participantflow

import com.jnj.vaccinetracker.robots.base.BaseRobot
import de.codecentric.androidtestktx.espresso.extensions.buttonContaining
import de.codecentric.androidtestktx.espresso.extensions.click
import de.codecentric.androidtestktx.espresso.extensions.textContaining

fun participantFlowMatching(func: ParticipantFlowMatchingRobot.() -> Unit): ParticipantFlowMatchingRobot {
    return ParticipantFlowMatchingRobot().apply(func)
}


class ParticipantFlowMatchingRobot : BaseRobot() {
    init {
        waitForView(textContaining("no match"))
    }

    fun newParticipant() {
        waitForView(buttonContaining("new Child")).perform(click)
    }
}

