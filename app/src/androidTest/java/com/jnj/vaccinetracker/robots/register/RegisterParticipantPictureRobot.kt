package com.jnj.vaccinetracker.robots.register

import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.robots.base.BaseRobot
import de.codecentric.androidtestktx.espresso.extensions.click
import de.codecentric.androidtestktx.espresso.extensions.waitFor


fun registerParticipantPictureRobot(func: RegisterParticipantPictureRobot.() -> Unit) = RegisterParticipantPictureRobot().apply(func)


class RegisterParticipantPictureRobot : BaseRobot() {

    fun takePicture() {
        if (!hasCameraPermission()) {
            waitFor(10000)
            allowPermissions()
        }
        waitForView(withId(R.id.camera_capture_button)).perform(click)
    }

    public override fun submit() {
        super.submit()
    }

    private fun hasCameraPermission(): Boolean {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
}

