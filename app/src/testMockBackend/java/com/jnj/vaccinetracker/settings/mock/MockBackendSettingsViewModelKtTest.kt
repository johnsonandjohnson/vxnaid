package com.jnj.vaccinetracker.settings.mock

import com.jnj.vaccinetracker.common.helpers.hours
import com.jnj.vaccinetracker.common.helpers.minutes
import com.jnj.vaccinetracker.common.helpers.seconds
import com.jnj.vaccinetracker.settings.mock.MockBackendSettingsViewModel.Companion.formatTime
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MockBackendSettingsViewModelKtTest : FunSpec({


    test("test format time") {
        val hours = 100
        val minutes = 30
        val seconds = 3
        formatTime(hours.hours + minutes.minutes + seconds.seconds) shouldBe "${hours}h${minutes}m${seconds}s"
    }

})
