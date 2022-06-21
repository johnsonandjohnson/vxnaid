package com.jnj.vaccinetracker.robots.register

import androidx.test.espresso.matcher.ViewMatchers.withId
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.domain.entities.Gender
import com.jnj.vaccinetracker.robots.base.BaseRobot
import de.codecentric.androidtestktx.espresso.extensions.*
import org.hamcrest.CoreMatchers.instanceOf


fun registerParticipantDetailsRobot(func: RegisterParticipantDetailsRobot.() -> Unit) = RegisterParticipantDetailsRobot().apply(func)


class RegisterParticipantDetailsRobot : BaseRobot() {

    init {
        waitUntilProgressBarGone()
    }

    fun gender(gender: Gender) {
        val resId = when (gender) {
            Gender.MALE -> R.id.rb_gender_male
            Gender.FEMALE -> R.id.rb_gender_female
            Gender.OTHER -> R.id.rb_gender_other
        }
        click on withId(resId)
    }

    fun birthYear(year: Int) {
        replaceText("$year") on withTextHintContaining("xxxx")
    }

    fun homeLocation(street: String, houseNumber: String) {
        click on withId(R.id.btn_set_home_location)
        homeLocation {
            country(0)
            state(1)
            city(1)
            postalCode(1)
            street(street)
            houseNumber(houseNumber)
            confirm()
        }
    }

    fun regimen(position: Int) {
        click on withTextHintContaining("regimen")
        dropDownClick(instanceOf(String::class.java), position)
    }

    fun language(position: Int) {
        click on withTextHintContaining("lang")
        dropDownClick(instanceOf(String::class.java), position)
    }

    public override fun submit() {
        waitForView(buttonContaining("register")).perform(click)
        waitUntilProgressBarGone()
    }


}



