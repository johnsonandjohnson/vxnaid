package com.jnj.vaccinetracker.fake.data.random.helper

import com.jnj.vaccinetracker.fake.data.random.helper.RandomPhoneGenerator.Companion.digitsOnly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RandomPhoneGeneratorKtTest : FunSpec({

    test("digitsOnly") {
        "-+1 23),;45 a)".digitsOnly() shouldBe "12345"
    }

})
