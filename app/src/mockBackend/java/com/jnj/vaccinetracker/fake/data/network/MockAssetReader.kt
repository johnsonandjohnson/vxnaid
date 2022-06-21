package com.jnj.vaccinetracker.fake.data.network

import android.content.Context
import com.jnj.vaccinetracker.common.data.mappers.AddressHierarchyDtoMapper
import com.jnj.vaccinetracker.common.data.models.api.response.ConfigurationDto
import com.jnj.vaccinetracker.common.data.models.api.response.LocalizationMapDto
import com.jnj.vaccinetracker.common.data.models.api.response.LoginResponse
import com.jnj.vaccinetracker.common.data.models.api.response.SitesDto
import com.jnj.vaccinetracker.common.domain.entities.AddressHierarchy
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.sync.data.models.ActiveUsersResponse
import com.jnj.vaccinetracker.sync.data.models.VaccineSchedule
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class MockAssetReader @Inject constructor(
    private val context: Context,
    private val moshi: Moshi,
    private val dispatchers: AppCoroutineDispatchers,
    private val addressHierarchyDtoMapperProvider: Provider<AddressHierarchyDtoMapper>,
) {

    companion object {
        private const val mockFolder = "mock"
    }

    private val addressHierarchyDtoMapper by lazy { addressHierarchyDtoMapperProvider.get() }

    private val cache = mutableMapOf<String, Any>()

    private fun openAsset(name: String): InputStream {
        return context.assets.open(name)
    }

    private val image by lazy {
        openAsset(mockFile("image.jpg")).use { it.readBytes() }

    }

    private suspend fun loadIrisTemplates(): List<ByteArray> = withContext(dispatchers.io) {
        require(irisTemplateAssetNames.isNotEmpty()) { "irisTemplateAssetNames must not be empty" }
        irisTemplateAssetNames.map { assetName -> openAsset(assetName).use { it.readBytes() }.also { require(it.isNotEmpty()) } }
    }

    private val mutex = Mutex()

    private var irisTemplates: List<ByteArray>? = null

    private val irisTemplateAssetNames by lazy {
        val path = mockFile("iris_templates")
        context.assets.list(path)!!.map { "$path/$it" }
    }

    private fun mockFile(fileName: String) = "$mockFolder/$fileName"

    private fun readFile(fileName: String): String {
        return openAsset(mockFile(fileName)).use { it.readBytes() }.decodeToString()
    }

    private inline fun <reified T> readJson(fileName: String): T {
        return moshi.adapter(T::class.java).fromJson(readFile(fileName))!!
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : Any> cached(key: String, block: suspend () -> T): T = withContext(dispatchers.io) {
        (cache[key] ?: run {
            block().also {
                cache[key] = it
            }
        }) as T
    }

    suspend fun readConfiguration(): ConfigurationDto = cached("config") {
        moshi.adapter(ConfigurationDto::class.java).fromJson(readFile("configuration.json"))!!
    }

    suspend fun readSites(): SitesDto = cached("sites") {
        moshi.adapter(SitesDto::class.java).fromJson(readFile("locations.json"))!!
    }

    suspend fun readLocalization(): LocalizationMapDto = cached("loc") {
        moshi.adapter(LocalizationMapDto::class.java).fromJson(readFile("localization.json"))!!
    }

    private fun readAddressHierarchyRaw() = moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
        .fromJson(readFile("addresshierarchy.json"))!!

    suspend fun readAddressHierarchy() = cached("addresshierarchy") {
        readAddressHierarchyRaw()
    }

    suspend fun readAddressHierarchyDomain(): AddressHierarchy = cached("addresshierarchy_domain") {
        readAddressHierarchyRaw().let { addressHierarchyDtoMapper.toDomain(it) }
    }

    suspend fun readLoginResponse(): LoginResponse = cached("loginResponse") {
        readJson("login_response_admin.json")
    }

    suspend fun readActiveUsers(): ActiveUsersResponse = cached("activeUsers") {
        readJson("active_users.json")
    }

    suspend fun readVaccineSchedule(): VaccineSchedule = cached("vaccineSchedule") {
        readJson("vaccine_schedule.json")
    }

    fun readImage(): ByteArray = image

    suspend fun readRandomIrisTemplate(): ByteArray {
        val items = mutex.withLock {
            irisTemplates ?: loadIrisTemplates().also { irisTemplates = it }
        }
        return items.random()
    }
}