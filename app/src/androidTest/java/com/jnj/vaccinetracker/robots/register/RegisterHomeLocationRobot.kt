package com.jnj.vaccinetracker.robots.register

import android.widget.Spinner
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import com.jnj.vaccinetracker.robots.base.BaseRobot
import de.codecentric.androidtestktx.espresso.extensions.*
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf


fun homeLocation(func: RegisterHomeLocationRobot.() -> Unit) = RegisterHomeLocationRobot().apply(func)


class RegisterHomeLocationRobot : BaseRobot() {

    private fun clickSpinner(displayName: String) {
        waitForView(allOf(ofViewType<Spinner>(), hasSibling(textContaining(displayName)))).perform(click)
        waitFor(300)
    }

    fun country(position: Int) {
        clickSpinner("country")
        dropDownClick(instanceOf(String::class.java), position)
    }

    fun state(position: Int) {
        clickSpinner("state")
        dropDownClick(instanceOf(String::class.java), position)
    }

    fun city(position: Int) {
        clickSpinner("city")
        dropDownClick(instanceOf(String::class.java), position)
    }

    fun postalCode(position: Int) {
        clickSpinner("postal")
        dropDownClick(instanceOf(String::class.java), position)
    }

    fun street(street: String) {
        replaceText(street) into withTextHintContaining("street")
    }

    fun houseNumber(houseNumber: String) {
        replaceText(houseNumber) into withTextHintContaining("number")
    }

    fun confirm() {
        waitForView(buttonContaining("confirm")).perform(click)
    }
}