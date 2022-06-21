package com.jnj.vaccinetracker.common.data.models.api.response

import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ObservationDtoKtTest : FunSpec({

    test("toMap") {
        val visitA = ObservationDto("name1", "v1", dateNow())
        val visitB = ObservationDto("name1", "v2", dateNow())
        listOf(visitA, visitB).toMap().values.map { it.value } shouldBe listOf("v1")
    }
})
