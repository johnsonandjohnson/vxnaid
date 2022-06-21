package com.jnj.vaccinetracker.sync.domain.entities

import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.data.models.LicenseType
import com.jnj.vaccinetracker.common.di.NetworkModule
import com.jnj.vaccinetracker.common.domain.entities.MasterDataFile
import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import com.jnj.vaccinetracker.common.helpers.uuid
import com.jnj.vaccinetracker.sync.data.models.*
import com.squareup.moshi.Types
import io.kotest.core.spec.style.FunSpec

@Suppress("BlockingMethodInNonBlockingContext")
class SyncErrorMetadataTest : FunSpec({
    fun createStackTrace(): String = "Exception(test)"
    val moshi = NetworkModule().provideMoshi()
    test("create all combination json") {
        fun <A, B> Array<A>.attachTo(listOther: Array<B>): Array<Pair<A, B>> {
            var indexB = 0
            return map { a ->
                if (indexB == listOther.size)
                    indexB = 0
                a to listOther[indexB++]
            }.toTypedArray()
        }

        val allLicenseErrors = SyncErrorMetadata.License.Action.values().attachTo(LicenseType.values()).map { (action, license) ->
            SyncErrorMetadata.License(license, action)
        }.toTypedArray()
        val syncCompleteError = SyncErrorMetadata.ReportSyncCompletedDateCall(SyncDate(dateNow()))
        val syncRequest = SyncRequest(dateModifiedOffset = SyncDate(dateNow()),
            syncScope = SyncScopeDto(country = "India", cluster = "Test", siteUuid = uuid()),
            uuidsWithDateModifiedOffset = listOf("uuid"),
            limit = 100,
            optimize = false)
        val getAllSyncRecordsCallErrors = SyncEntityType.values().attachTo(arrayOf(*SyncStatus.values(), null)).map { (syncEntityType, _) ->
            SyncErrorMetadata.GetAllSyncRecordsCall(syncEntityType, syncRequest)
        }.toTypedArray()
        val pendingCallErrors = ParticipantPendingCall.Type.values().map { type ->
            SyncErrorMetadata.UploadParticipantPendingCall(type, uuid(), if (type.toString().contains("visit", ignoreCase = true)) uuid() else null, uuid(), "pa001")
        }.toTypedArray()

        val masterDataErrors = MasterDataFile.values().attachTo(SyncErrorMetadata.MasterData.Action.values()).map { (masterDataFile, action) ->
            SyncErrorMetadata.MasterData(masterDataFile, action)
        }.toTypedArray()
        val storeSyncRecordErrors = SyncEntityType.values().map { type ->
            SyncErrorMetadata.StoreSyncRecord(type, uuid(), if (type.toString().contains("visit", ignoreCase = true)) uuid() else null)
        }.toTypedArray()

        val allMetadata = listOf(
            *allLicenseErrors,
            syncCompleteError,
            *getAllSyncRecordsCallErrors,
            *pendingCallErrors,
            SyncErrorMetadata.MasterDataUpdatesCall(),
            *masterDataErrors,
            SyncErrorMetadata.FindAllRelatedDraftDataPendingUpload(uuid()),
            *storeSyncRecordErrors
        )

        val syncErrors = allMetadata.map {
            SyncErrorDto(it, createStackTrace(), dateNow())
        }

        val json = moshi.adapter<List<SyncErrorDto>>(Types.newParameterizedType(List::class.java, SyncErrorDto::class.java))
            .toJson(syncErrors)
        println(json)
        0.until(5).forEach { _ ->
            println("++++++++++")
        }
        val json2 = moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
            .toJson(syncErrors.take(2).map { it.key })
        println(json2)

    }

    test("when json is missing nullable field from class then don't crash") {
        val json = """{"type": "uploadParticipantPendingCall",
      "pendingCallType": "UPDATE_VISIT",
      "participantUuid": "394c2a6c-3436-4351-aa33-a8a7e0d9ee75",
      "visitUuid": "87321bf4-ec51-44d3-9295-aa8689400dd5"
    }
        """.trimIndent()
        moshi.adapter(SyncErrorMetadata.UploadParticipantPendingCall::class.java).fromJson(json)
    }


})
