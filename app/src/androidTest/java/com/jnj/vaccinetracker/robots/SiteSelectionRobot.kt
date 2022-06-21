package com.jnj.vaccinetracker.robots

import com.jnj.vaccinetracker.robots.base.BaseRobot
import de.codecentric.androidtestktx.espresso.extensions.on
import de.codecentric.androidtestktx.espresso.extensions.replaceText
import de.codecentric.androidtestktx.espresso.extensions.waitFor
import de.codecentric.androidtestktx.espresso.extensions.withTextHintContaining

fun siteSelection(func: SiteSelectionRobot.() -> Unit): SiteSelectionRobot {
    return SiteSelectionRobot().apply(func)
}

infix fun SiteSelectionRobot.verifyThat(fn: SiteSelectionRobotResult.() -> Unit) = SiteSelectionRobotResult().apply(fn)

class SiteSelectionRobot : BaseRobot() {
    init {
        waitUntilProgressBarGone()
    }

    fun selectSite(siteName: String) {
        waitFor(1000)
        replaceText(siteName) on withTextHintContaining("select site")
    }

    public override fun submit() {
        super.submit()
    }
}

class SiteSelectionRobotResult {

}