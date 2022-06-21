package com.jnj.vaccinetracker.e2etest.helper

import com.jnj.vaccinetracker.common.domain.entities.Gender
import com.jnj.vaccinetracker.robots.login
import com.jnj.vaccinetracker.robots.participantflow.irisscan.participantFlowIrisScanLeft
import com.jnj.vaccinetracker.robots.participantflow.irisscan.participantFlowIrisScanRight
import com.jnj.vaccinetracker.robots.participantflow.participantFlowMatching
import com.jnj.vaccinetracker.robots.participantflow.participantFlowParticipantId
import com.jnj.vaccinetracker.robots.participantflow.participantFlowPhone
import com.jnj.vaccinetracker.robots.participantflow.participationFlowIntro
import com.jnj.vaccinetracker.robots.register.registerParticipantDetailsRobot
import com.jnj.vaccinetracker.robots.register.registerParticipantPictureRobot
import com.jnj.vaccinetracker.robots.register.registerSuccess
import com.jnj.vaccinetracker.robots.siteSelection

fun goThroughRegistrationFlow(skipLogin: Boolean) {
    val participantId = "123123"
    val phone = "033534566"
    val phoneAreaCode = "+32"
    val gender = Gender.MALE
    val birthYear = 1994
    val street = "Koekoekstraa<t"
    val houseNumber = "41"
    if (!skipLogin) {
        login {
            username("admin")
            password("Admin123")
            submit()
        }
    }
    siteSelection {
        selectSite("Bangalore clinic")
        submit()
    }
    participationFlowIntro {
        startWorkFlow()
    }
    participantFlowParticipantId {
        participantId(participantId)
        submit()
    }
    participantFlowPhone {
        areaCode(phoneAreaCode)
        phone(phone)
        submit()
    }
    participantFlowIrisScanRight {
        if (loadImage())
            submit()
        else
            skip()
    }
    participantFlowIrisScanLeft {
        if (loadImage())
            submit()
        else
            skip()
    }
    participantFlowMatching {
        newParticipant()
    }
    registerParticipantPictureRobot {
        takePicture()
        submit()
    }
    registerParticipantDetailsRobot {
        gender(gender)
        birthYear(birthYear)
        homeLocation(street = street, houseNumber = houseNumber)
        regimen(0)
        language(0)
        submit()
    }

    registerSuccess {
        finishWorkflow()
        verifyIsHome()
    }
}