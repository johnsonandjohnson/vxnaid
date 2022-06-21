package com.jnj.vaccinetracker.sync.domain.usecases.download

import com.jnj.vaccinetracker.common.data.helpers.WebCallUtil
import com.jnj.vaccinetracker.common.data.network.BaseUrlInterceptor
import com.jnj.vaccinetracker.common.data.network.DeviceIdInterceptor
import com.jnj.vaccinetracker.common.data.repositories.CookieRepository
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.di.NetworkModule
import com.jnj.vaccinetracker.common.exceptions.SiteNotFoundException
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.Logger
import com.jnj.vaccinetracker.common.helpers.NetworkConnectivity
import com.jnj.vaccinetracker.common.helpers.ServerHealthMeter
import com.jnj.vaccinetracker.common.ui.format
import com.jnj.vaccinetracker.sync.data.models.*
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSourceDefault
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.jnj.vaccinetracker.sync.data.repositories.SyncUserCredentialsRepository
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import com.jnj.vaccinetracker.sync.domain.usecases.ValidateSyncResponseUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.download.base.DownloadSyncRecordsUseCaseBase
import com.jnj.vaccinetracker.sync.domain.usecases.failed.DeleteFailedSyncRecordUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.failed.StoreFailedSyncRecordDownloadUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.store.StoreParticipantBiometricsTemplateSyncRecordUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.store.StoreParticipantImageSyncRecordUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.store.StoreParticipantSyncRecordUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.store.StoreVisitSyncRecordUseCase
import com.squareup.moshi.Types
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import okhttp3.OkHttpClient


@OptIn(ExperimentalCoroutinesApi::class)
class DownloadSyncRecordsUseCaseIntegrationTest : FunSpec({
    // SETTINGS
    val backendUrl = "https://demo-iris.jnj.connect-for-life.org"
    val siteUuid = "1424ae66-aaa6-4c33-b2b1-9789df1dba64"
    val syncScopeLevel = SyncScopeLevel.SITE



    Logger.ENABLED = false
    fun List<SyncRequest>.toJson(): String {
        val moshi = NetworkModule().provideMoshi()
        val type = Types.newParameterizedType(List::class.java, SyncRequest::class.java)
        return moshi.adapter<List<SyncRequest>>(type).toJson(this)!!
    }

    val syncRequests = mutableListOf<SyncRequest>()
    val dispatcher = TestCoroutineDispatcher()
    val dispatchers = AppCoroutineDispatchers.fromSingleDispatcher(dispatcher)
    val syncSettingsRepository: SyncSettingsRepository = mockk()
    val userRepository: UserRepository = mockk()
    val syncUserCredentialsRepository: SyncUserCredentialsRepository = mockk()
    val syncCookieRepository: CookieRepository = FakeCookieRepository(dispatchers)
    val syncUserCredentials = SyncUserCredentials("admin", "Admin123")
    coEvery { syncUserCredentialsRepository.getSyncUserCredentials() } returns syncUserCredentials
    coEvery { syncSettingsRepository.getSiteUuid() } returns siteUuid
    coEvery { syncSettingsRepository.getBackendUrl() } returns backendUrl
    coEvery { userRepository.getDeviceGuid() } returns "001"
    fun createHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(BaseUrlInterceptor(syncSettingsRepository))
            .addInterceptor(DeviceIdInterceptor(userRepository))
            .cookieJar(syncCookieRepository)
            .build()
    }


    val networkConnectivity: NetworkConnectivity = mockk()
    val serverHealthMeter: ServerHealthMeter = mockk()
    coEvery { networkConnectivity.isConnectedFast() } returns true
    coEvery { networkConnectivity.requireFastInternet() } returns Unit
    val webCallUtil = WebCallUtil(networkConnectivity, dispatchers, serverHealthMeter)
    val apiService = NetworkModule().provideVaccineTrackerSyncApiService(createHttpClient())
    val api: VaccineTrackerSyncApiDataSource = VaccineTrackerSyncApiDataSourceDefault(webCallUtil, apiService, syncUserCredentialsRepository, syncCookieRepository)
    val syncLogger: SyncLogger = mockk()
    coEvery { syncLogger.logSyncError(any(), any()) } returns Unit
    coEvery { syncLogger.logSyncCompletedDateReported(any()) } returns Unit
    coEvery { syncLogger.clearSyncError(any()) } returns Unit

    val validateSyncResponseUseCase: ValidateSyncResponseUseCase = mockk()
    val storeFailedSyncRecordsUseCase: StoreFailedSyncRecordDownloadUseCase = mockk()
    val syncResponseSlot = CapturingSlot<SyncResponse<*>>()
    coEvery { validateSyncResponseUseCase.validate(capture(syncResponseSlot), any(), any()) } returns Unit
    coEvery { storeFailedSyncRecordsUseCase.store(any()) } throws Exception("unexpected failed record")
    val deleteFailedSyncRecordsUseCase: DeleteFailedSyncRecordUseCase = mockk()
    coEvery { deleteFailedSyncRecordsUseCase.delete(any()) } returns Unit

    suspend fun <R : SyncRecordBase, U : DownloadSyncRecordsUseCaseBase<R>> doTest(syncRecordRepo: FakeSyncRecordRepo<R>, downloadVisitSyncRecordsUseCase: U, limit: Int) {
        api.login(syncUserCredentials)
        syncRequests.clear()
        val site = api.getSites().results.find { it.uuid == siteUuid } ?: throw SiteNotFoundException(siteUuid)
        while (true) {

            val syncScope = site.toSyncScopeDto(syncScopeLevel)
            val mostRecentDate = syncRecordRepo.values.maxByOrNull { it.dateModified }?.dateModified
            val offsetUuids = syncRecordRepo.values.filter { it.dateModified == mostRecentDate }.map { it.uuid }
            println("offset value: $offsetUuids ${mostRecentDate?.format()}")
            val syncRequest = SyncRequest(mostRecentDate, syncScope, limit = limit, optimize = false, uuidsWithDateModifiedOffset = offsetUuids)
            syncRequests += syncRequest
            println("sending request: $syncRequest")

            val syncStatus = downloadVisitSyncRecordsUseCase.download(syncRequest)

            @Suppress("UNCHECKED_CAST")
            val syncResponse = syncResponseSlot.captured as SyncResponse<R>
            if (syncStatus == SyncStatus.OK) {
                kotlin.runCatching {
                    syncRecordRepo.values.size shouldBe syncResponse.totalSyncScopeRecordCount
                }.onFailure {
                    println("===sync requests====")
                    println(syncRequests.toJson())
                }.getOrThrow()
                break
            }
        }
        println("syncRecordRepo: ${syncRecordRepo.size}")

    }



    context("visits") {
        val limit = 100
        test("given limit = $limit then after sync status OK we have syncResponse.tableCount stored") {
            // Arrange
            val storeVisitSyncRecordUseCase: StoreVisitSyncRecordUseCase = mockk()
            val syncRecordRepo = FakeSyncRecordRepo<VisitSyncRecord>()
            coEvery { storeVisitSyncRecordUseCase.store(any()) } coAnswers { inv ->
                val record = inv.invocation.args.first() as VisitSyncRecord
                syncRecordRepo.storeSyncRecord(record)
            }
            val downloadVisitSyncRecordsUseCase =
                DownloadVisitSyncRecordsUseCase(api,
                    storeVisitSyncRecordUseCase,
                    syncLogger,
                    validateSyncResponseUseCase,
                    storeFailedSyncRecordsUseCase,
                    deleteFailedSyncRecordsUseCase)

            // Act
            doTest(syncRecordRepo, downloadVisitSyncRecordsUseCase, limit)
        }
    }

    context("participants") {
        val limit = 100
        test("given limit = $limit then after sync status OK we have syncResponse.tableCount stored") {
            // Arrange
            val storeSyncRecordUseCase: StoreParticipantSyncRecordUseCase = mockk()
            val syncRecordRepo = FakeSyncRecordRepo<ParticipantSyncRecord>()
            coEvery { storeSyncRecordUseCase.store(any()) } coAnswers { inv ->
                val record = inv.invocation.args.first() as ParticipantSyncRecord
                syncRecordRepo.storeSyncRecord(record)
            }
            val downloadSyncRecordsUseCase =
                DownloadParticipantSyncRecordsUseCase(api,
                    storeSyncRecordUseCase,
                    syncLogger,
                    validateSyncResponseUseCase,
                    storeFailedSyncRecordsUseCase,
                    deleteFailedSyncRecordsUseCase)

            // Act
            doTest(syncRecordRepo, downloadSyncRecordsUseCase, limit)
        }
    }

    context("templates") {
        val limit = 50
        test("given limit = $limit then after sync status OK we have syncResponse.tableCount stored") {
            // Arrange
            val storeSyncRecordUseCase: StoreParticipantBiometricsTemplateSyncRecordUseCase = mockk()
            val syncRecordRepo = FakeSyncRecordRepo<ParticipantBiometricsTemplateSyncRecord>()
            coEvery { storeSyncRecordUseCase.store(any()) } coAnswers { inv ->
                val record = inv.invocation.args.first() as ParticipantBiometricsTemplateSyncRecord
                syncRecordRepo.storeSyncRecord(record)
            }
            val downloadSyncRecordsUseCase =
                DownloadParticipantBiometricsTemplateSyncRecordsUseCase(api,
                    storeSyncRecordUseCase,
                    validateSyncResponseUseCase,
                    syncLogger,
                    storeFailedSyncRecordsUseCase,
                    deleteFailedSyncRecordsUseCase)

            // Act
            doTest(syncRecordRepo, downloadSyncRecordsUseCase, limit)
        }
    }

    context("images") {
        val limit = 50
        test("given limit = $limit then after sync status OK we have syncResponse.tableCount stored") {
            // Arrange
            val storeSyncRecordUseCase: StoreParticipantImageSyncRecordUseCase = mockk()
            val syncRecordRepo = FakeSyncRecordRepo<ParticipantImageSyncRecord>()
            coEvery { storeSyncRecordUseCase.store(any()) } coAnswers { inv ->
                val record = inv.invocation.args.first() as ParticipantImageSyncRecord
                syncRecordRepo.storeSyncRecord(record)
            }
            val downloadSyncRecordsUseCase =
                DownloadParticipantImageSyncRecordsUseCase(api,
                    storeSyncRecordUseCase,
                    syncLogger,
                    validateSyncResponseUseCase,
                    storeFailedSyncRecordsUseCase,
                    deleteFailedSyncRecordsUseCase)

            // Act
            doTest(syncRecordRepo, downloadSyncRecordsUseCase, limit)
        }
    }


})
