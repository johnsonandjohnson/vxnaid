package com.jnj.vaccinetracker.barcode

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BarcodeUtilsKtTest : FunSpec({

    listOf(
        "12345A 134" to "12345A134",
        "12345A\n134" to "12345A134",
        "12345A\u001D134" to "12345A134",
    ).forEach { (arg, expectedResult) ->
        test("given $arg then return $expectedResult") {
            formatBarcode(arg) shouldBe expectedResult
        }
    }
})
