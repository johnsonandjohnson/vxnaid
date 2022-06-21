package com.jnj.vaccinetracker.robots.participantflow

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import com.hbb20.CountryCodePicker
import com.jnj.vaccinetracker.robots.base.BaseRobot
import de.codecentric.androidtestktx.espresso.extensions.click
import de.codecentric.androidtestktx.espresso.extensions.ofViewType
import de.codecentric.androidtestktx.espresso.extensions.replaceText
import de.codecentric.androidtestktx.espresso.extensions.withTextHintContaining

fun participantFlowPhone(func: ParticipantFlowPhoneRobot.() -> Unit): ParticipantFlowPhoneRobot {
    return ParticipantFlowPhoneRobot().apply(func)
}


class ParticipantFlowPhoneRobot : BaseRobot() {
    fun areaCode(areaCode: String) {
        waitForView(ofViewType<CountryCodePicker>()).perform(click)
        onView(withTextHintContaining("search"))
            .inRoot(isDialog())
            .perform(replaceText(areaCode))
        onView(ofViewType<RecyclerView>())
            .check(matches(isDisplayed()))
            .inRoot(isDialog())
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click));
    }

    fun phone(phone: String) {
        onView(withTextHintContaining("phone"))
            .perform(replaceText(phone))
    }

    public override fun submit() {
        super.submit()
    }
}

