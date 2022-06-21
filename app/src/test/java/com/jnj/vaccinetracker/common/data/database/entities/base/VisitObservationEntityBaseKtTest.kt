package com.jnj.vaccinetracker.common.data.database.entities.base

import com.jnj.vaccinetracker.common.data.database.entities.VisitObservationEntity
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class VisitObservationEntityBaseKtTest : FunSpec({

    test("toMap") {
        val visitA = VisitObservationEntity(VisitObservationEntity.Id("id1", "name1"), "v1", dateNow())
        val visitB = VisitObservationEntity(VisitObservationEntity.Id("id2", "name1"), "v2", dateNow())
        listOf(visitA, visitB).toMap().values.map { it.value } shouldBe listOf("v1")
    }
})
