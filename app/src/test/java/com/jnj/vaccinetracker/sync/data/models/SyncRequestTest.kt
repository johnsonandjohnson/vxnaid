package com.jnj.vaccinetracker.sync.data.models

import com.jnj.vaccinetracker.common.di.NetworkModule
import com.jnj.vaccinetracker.common.helpers.uuid
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain

class SyncRequestTest : FunSpec({

    val moshi = NetworkModule().provideMoshi()

    test("given null dateModifiedOffset then still serialize it") {
        val syncRequest = SyncRequest(dateModifiedOffset = null, syncScope = SyncScopeDto(country = "test", cluster = "test", siteUuid = uuid()),
            uuidsWithDateModifiedOffset = emptyList(),
            limit = 2,
            optimize = false)
        val jsonAdapter = moshi.adapter(SyncRequest::class.java)
        val json = jsonAdapter
            .toJson(syncRequest)
        json shouldContain "dateModifiedOffset"
    }
})
