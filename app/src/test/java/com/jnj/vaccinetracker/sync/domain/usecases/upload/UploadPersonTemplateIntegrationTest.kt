package com.jnj.vaccinetracker.sync.domain.usecases.upload

import com.jnj.vaccinetracker.common.data.helpers.WebCallUtil
import com.jnj.vaccinetracker.common.data.network.BaseUrlInterceptor
import com.jnj.vaccinetracker.common.data.network.DeviceIdInterceptor
import com.jnj.vaccinetracker.common.data.network.apiexceptioninterceptor.SyncApiExceptionInterceptor
import com.jnj.vaccinetracker.common.data.repositories.CookieRepository
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.di.NetworkModule
import com.jnj.vaccinetracker.common.domain.entities.BiometricsTemplateBytes
import com.jnj.vaccinetracker.common.exceptions.TemplateAlreadyExistsException
import com.jnj.vaccinetracker.common.exceptions.WebCallException
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.Logger
import com.jnj.vaccinetracker.common.helpers.NetworkConnectivity
import com.jnj.vaccinetracker.common.helpers.ServerHealthMeter
import com.jnj.vaccinetracker.sync.data.models.SyncUserCredentials
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSourceDefault
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.jnj.vaccinetracker.sync.data.repositories.SyncUserCredentialsRepository
import com.jnj.vaccinetracker.sync.domain.usecases.download.FakeCookieRepository
import io.kotest.core.spec.style.FunSpec
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import okhttp3.OkHttpClient
import readResourceBytes


@OptIn(ExperimentalCoroutinesApi::class)
class UploadPersonTemplateIntegrationTest : FunSpec({
    // SETTINGS
    val backendUrl = "https://vxnaid-development.jnj.connect-for-life.org"
    Logger.TEST_MODE = true

    val dispatcher = TestCoroutineDispatcher()
    val moshi = NetworkModule().provideMoshi()
    val dispatchers = AppCoroutineDispatchers.fromSingleDispatcher(dispatcher)
    val syncSettingsRepository: SyncSettingsRepository = mockk()
    val userRepository: UserRepository = mockk()
    val syncUserCredentialsRepository: SyncUserCredentialsRepository = mockk()
    val syncCookieRepository: CookieRepository = FakeCookieRepository(dispatchers)
    val syncUserCredentials = SyncUserCredentials("syncadmin", "Admin123")
    val apiInterceptor = SyncApiExceptionInterceptor(syncCookieRepository, moshi)
    coEvery { syncUserCredentialsRepository.getSyncUserCredentials() } returns syncUserCredentials
    coEvery { syncSettingsRepository.getBackendUrl() } returns backendUrl
    coEvery { userRepository.getDeviceGuid() } returns "001"
    fun createHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(BaseUrlInterceptor(syncSettingsRepository))
            .addInterceptor(DeviceIdInterceptor(userRepository))
            .addInterceptor(apiInterceptor)
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

    test("test person template call") {
        // Arrange
        val template = BiometricsTemplateBytes(readResourceBytes("iris_template.dat"))
        val participantUuid = "a07c72b5-a9ea-4d30-8b54-ff9b53d81c96"
        api.login(syncUserCredentials)
        // Act
        try {
            api.personTemplate(participantUuid, template)
            println("test success")
        } catch (ex: WebCallException) {
            when (ex.cause) {
                is TemplateAlreadyExistsException -> {
                    println("test success already exists")
                }
                else -> throw ex
            }
        }
    }
})
