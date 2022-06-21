@file:Suppress("BlockingMethodInNonBlockingContext")

package com.jnj.vaccinetracker.sync.data.models

import com.jnj.vaccinetracker.common.di.NetworkModule
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Types
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import org.junit.jupiter.api.fail

class SyncResponseTest : FunSpec({


    val jsonWithUuids = """
        {
    "dateModifiedOffset": 1628074115000,
    "syncScope": {
        "siteUuid": null,
        "country": "India"
    },
    "optimize": false,
        "uuidsWithDateModifiedOffset": ["eb89ee7a-4f18-4e60-82ae-22b7581b781e"],
    "syncStatus": "OUT_OF_SYNC",
    "offset": 0,
    "limit": 50,
    "tableCount": 256381,
    "records": []
        }
    """.trimIndent()


    val jsonWithoutUuids = """
        {
    "dateModifiedOffset": 1628074115000,
    "syncScope": {
        "siteUuid": null,
        "country": "India"
    },
    "optimize": false,
    "syncStatus": "OUT_OF_SYNC",
    "offset": 0,
    "limit": 50,
    "tableCount": 256381,
    "records": []
        }
    """.trimIndent()

    val jsonWithoutOffset = """
        {
    "dateModifiedOffset": 1628074115000,
    "syncScope": {
        "siteUuid": null,
        "country": "India"
    },
      "uuidsWithDateModifiedOffset": ["eb89ee7a-4f18-4e60-82ae-22b7581b781e"],
    "optimize": false,
    "syncStatus": "OUT_OF_SYNC",
    "limit": 50,
    "tableCount": 256381,
    "records": []
        }
    """.trimIndent()

    val moshi = NetworkModule().provideMoshi()

    val type = Types.newParameterizedType(SyncResponse::class.java, VisitSyncRecord::class.java)
    val adapter = moshi.adapter<SyncResponse<VisitSyncRecord>>(type)
    test("don't crash with uuids") {
        adapter.fromJson(jsonWithUuids).shouldNotBeNull()
    }

    test("crash without uuids") {
        try {
            adapter.fromJson(jsonWithoutUuids)
            fail("expected JsonDataException")
        } catch (ex: JsonDataException) {

        }
    }

    test("don't crash without offset") {
        adapter.fromJson(jsonWithoutOffset).shouldNotBeNull()
    }
})
