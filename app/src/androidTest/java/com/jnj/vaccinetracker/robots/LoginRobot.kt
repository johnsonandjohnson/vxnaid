package com.jnj.vaccinetracker.robots

import androidx.test.espresso.action.ViewActions.replaceText
import com.jnj.vaccinetracker.robots.base.BaseRobot
import de.codecentric.androidtestktx.espresso.extensions.buttonContaining
import de.codecentric.androidtestktx.espresso.extensions.click
import de.codecentric.androidtestktx.espresso.extensions.on
import de.codecentric.androidtestktx.espresso.extensions.withTextHintContaining

fun login(func: LoginRobot.() -> Unit): LoginRobot {
    return LoginRobot().apply(func)
}

infix fun LoginRobot.verifyThat(fn: LoginRobotResult.() -> Unit) = LoginRobotResult().apply(fn)

class LoginRobot : BaseRobot() {
    fun username(username: String) {
        replaceText(username) on withTextHintContaining("username")
    }

    fun password(password: String) {
        replaceText(password) on withTextHintContaining("password")
    }

    public override fun submit() {
        click on buttonContaining("sign in")
    }
}

class LoginRobotResult {

}