package com.jnj.vaccinetracker.common.data.helpers

import com.jnj.vaccinetracker.common.data.models.api.response.AddressHierarchyDto
import com.jnj.vaccinetracker.common.data.models.api.response.ConfigurationDto
import com.jnj.vaccinetracker.common.data.models.api.response.LocalizationMapDto
import com.jnj.vaccinetracker.common.data.models.api.response.SitesDto
import com.jnj.vaccinetracker.common.data.network.BaseUrlInterceptor
import com.jnj.vaccinetracker.common.data.network.DeviceIdInterceptor
import com.jnj.vaccinetracker.common.data.repositories.CookieRepository
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.di.NetworkModule
import com.jnj.vaccinetracker.common.domain.entities.MasterDataFile
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.NetworkConnectivity
import com.jnj.vaccinetracker.common.helpers.ServerHealthMeter
import com.jnj.vaccinetracker.sync.data.models.SyncUserCredentials
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSourceDefault
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.jnj.vaccinetracker.sync.data.repositories.SyncUserCredentialsRepository
import com.jnj.vaccinetracker.sync.domain.usecases.download.FakeCookieRepository
import com.squareup.moshi.Types
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.TestCoroutineDispatcher
import okhttp3.OkHttpClient

class Md5HashGeneratorIntegrationTest : FunSpec({
    val dispatcher = TestCoroutineDispatcher()
    val dispatchers = AppCoroutineDispatchers.fromSingleDispatcher(dispatcher)
    val md5HashGenerator = Md5HashGenerator(dispatchers)
    val backendUrl = "https://demo-iris.jnj.connect-for-life.org"
    val syncSettingsRepository: SyncSettingsRepository = mockk()
    val userRepository: UserRepository = mockk()
    val syncUserCredentialsRepository: SyncUserCredentialsRepository = mockk()
    val syncCookieRepository: CookieRepository = FakeCookieRepository(dispatchers)
    val syncUserCredentials = SyncUserCredentials("admin", "Admin123")
    coEvery { syncUserCredentialsRepository.getSyncUserCredentials() } returns syncUserCredentials
    coEvery { syncSettingsRepository.getBackendUrl() } returns backendUrl
    coEvery { userRepository.getDeviceGuid() } returns "001"
    fun createHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(BaseUrlInterceptor(syncSettingsRepository))
            .addInterceptor(DeviceIdInterceptor(userRepository))
            .cookieJar(syncCookieRepository)
            .build()
    }

    val moshi = NetworkModule().provideMoshi()
    val networkConnectivity: NetworkConnectivity = mockk()
    val serverHealthMeter: ServerHealthMeter = mockk()
    coEvery { networkConnectivity.isConnectedFast() } returns true
    coEvery { networkConnectivity.requireFastInternet() } returns Unit
    val webCallUtil = WebCallUtil(networkConnectivity, dispatchers, serverHealthMeter)
    val apiService = NetworkModule().provideVaccineTrackerSyncApiService(createHttpClient())
    val api: VaccineTrackerSyncApiDataSource = VaccineTrackerSyncApiDataSourceDefault(webCallUtil, apiService, syncUserCredentialsRepository, syncCookieRepository)

    beforeSpec {
        api.login(syncUserCredentials)
    }
    test("locations") {
        val backendResponse = api.getSites()
        val expectedHash = api.getMasterDataUpdates().find { it.name == MasterDataFile.SITES.syncName }!!.hash
        val adapter = moshi.adapter(SitesDto::class.java)
        val text = adapter.toJson(backendResponse)
        val hash = md5HashGenerator.md5(text)
        println(text)
        hash shouldBe expectedHash
    }

    test("config") {
        val backendResponse = api.getConfiguration()
        val expectedHash = api.getMasterDataUpdates().find { it.name == MasterDataFile.CONFIGURATION.syncName }!!.hash
        val adapter = moshi.adapter(ConfigurationDto::class.java)
        val text = adapter.toJson(backendResponse)
        val hash = md5HashGenerator.md5(text)
        println(text)
        hash shouldBe expectedHash
    }

    test("addressHierarchy") {
        val backendResponse = api.getCountryAddressHierarchy()
        val expectedHash = api.getMasterDataUpdates().find { it.name == MasterDataFile.ADDRESS_HIERARCHY.syncName }!!.hash
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<AddressHierarchyDto>(type)
        val text = adapter.toJson(backendResponse)
        val hash = md5HashGenerator.md5(text)
        println(text)
        hash shouldBe expectedHash
    }

    test("localization") {
        val backendResponse = api.getLocalization()
        val expectedHash = api.getMasterDataUpdates().find { it.name == MasterDataFile.LOCALIZATION.syncName }!!.hash
        val adapter = moshi.adapter(LocalizationMapDto::class.java)
        val text = adapter.toJson(backendResponse)
        val hash = md5HashGenerator.md5(text)
        println(text)
        hash shouldBe expectedHash
    }
})
