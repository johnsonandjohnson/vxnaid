package com.jnj.vaccinetracker.common.ui

import com.soywiz.klock.DateTime
import com.soywiz.klock.jvm.toDate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DateExtensionsKtTest : FunSpec({

    context("date.dateDayStart") {
        test("given date with time then return date without time") {
            val dateExpected = DateTime(2021, 12, 10).dateDayStart.toDate()
            val dateTime = DateTime(2021, 12, 10, 10, 10, 10, 10)
            val date = dateTime.toDate()
            val dateWithoutTime = date.dateDayStart
            println("dateWithoutTime=$dateWithoutTime")
            dateWithoutTime shouldBe dateExpected
        }
    }

})