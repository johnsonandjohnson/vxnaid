package com.jnj.vaccinetracker.robots.participantflow

import com.jnj.vaccinetracker.robots.base.BaseRobot
import de.codecentric.androidtestktx.espresso.extensions.replaceText
import de.codecentric.androidtestktx.espresso.extensions.withTextHintContaining

fun participantFlowParticipantId(func: ParticipantFlowParticipantIdRobot.() -> Unit) = ParticipantFlowParticipantIdRobot().apply(func)


class ParticipantFlowParticipantIdRobot : BaseRobot() {
    fun participantId(participantId: String) {
        waitForView(withTextHintContaining("#"))
            .perform(replaceText(participantId))
    }

    public override fun submit() {
        super.submit()
    }
}

