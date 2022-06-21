package com.jnj.vaccinetracker.common.domain.entities

import com.jnj.vaccinetracker.common.data.database.converters.BirthDateConverter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class BirthDateTest : FunSpec({


    test("given year of birth 1994 then return that date when converting back from database") {
        // arrange
        val yearOfBirth = 1994
        val birthDate = BirthDate.yearOfBirth(yearOfBirth)
        val converter = BirthDateConverter()
        // Act
        val epoch = converter.toEpochLong(birthDate)
        val convertedBirthDate = converter.toBirthDate(epoch)
        // Assert
        convertedBirthDate.shouldNotBeNull()
        convertedBirthDate.year shouldBe yearOfBirth
    }

})
