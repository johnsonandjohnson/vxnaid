package com.jnj.vaccinetracker.common.data.repositories

import com.jnj.vaccinetracker.common.data.encryption.EncryptionService
import com.jnj.vaccinetracker.common.data.helpers.AndroidFiles
import com.jnj.vaccinetracker.common.data.helpers.Md5HashGenerator
import com.jnj.vaccinetracker.common.data.models.api.response.AddressHierarchyDto
import com.jnj.vaccinetracker.common.data.models.api.response.LocalizationMapDto
import com.jnj.vaccinetracker.common.domain.entities.Configuration
import com.jnj.vaccinetracker.common.domain.entities.MasterDataFile
import com.jnj.vaccinetracker.common.domain.entities.Sites
import com.jnj.vaccinetracker.common.domain.entities.SubstancesConfig
import com.jnj.vaccinetracker.common.domain.entities.SubstancesGroupConfig
import com.jnj.vaccinetracker.common.domain.entities.substancesConfigAdapter
import com.jnj.vaccinetracker.common.domain.entities.substancesGroupConfigAdapter
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.logWarn
import com.jnj.vaccinetracker.common.helpers.toTemp
import com.jnj.vaccinetracker.sync.data.models.SyncDate
import com.jnj.vaccinetracker.sync.data.models.VaccineSchedule
import com.jnj.vaccinetracker.sync.data.models.vaccineScheduleAdapter
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject


class MasterDataRepository @Inject constructor(
    androidFiles: AndroidFiles,
    private val moshi: Moshi,
    private val encryptionService: EncryptionService,
    private val dispatchers: AppCoroutineDispatchers,
    private val md5HashGenerator: Md5HashGenerator,
) {
    companion object {
        private const val masterDataFolderName = "master_data"
    }

    private val filesDir: File = androidFiles.externalFiles

    private val configAdapter get() = moshi.adapter(Configuration::class.java)
    private val sitesAdapter get() = moshi.adapter(Sites::class.java)
    private val addressHierarchyAdapter get() = moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
    private val localizationAdapter get() = moshi.adapter(LocalizationMapDto::class.java)
    private val vaccineScheduleAdapter = moshi.vaccineScheduleAdapter()
    private val substancesConfigAdapter = moshi.substancesConfigAdapter()
    private val substancesGroupConfigAdapter = moshi.substancesGroupConfigAdapter()

    private val masterDataDir get() = File(filesDir, masterDataFolderName)
    private fun masterDataFile(fileName: String): File {
        val folder = masterDataDir.apply {
            mkdirs()
        }
        return File(folder, fileName)
    }

    private suspend fun writeFile(masterDataFile: MasterDataFile, json: String) = withContext(dispatchers.io) {
        logInfo("writeFile: $masterDataFile")
        val dstFile = masterDataFile(masterDataFile.fileName)
        val tmpFile = dstFile.toTemp()
        tmpFile.delete()
        if (masterDataFile.isEncrypted)
            encryptionService.writeEncryptedFile(tmpFile, json)
        else
            tmpFile.writeText(json)
        dstFile.delete()
        if (!tmpFile.renameTo(dstFile))
            throw IOException("failed to rename $tmpFile to $dstFile [masterDataFile:$masterDataFile]")
    }

    private suspend fun MasterDataFile.readContentDecrypted(): String? {
        val masterDataFile = this
        return withContext(dispatchers.io) {
            val srcFile = masterDataFile(masterDataFile.fileName)
            if (srcFile.canRead()) {
                if (masterDataFile.isEncrypted) {
                    encryptionService.readEncryptedFileAsText(srcFile)
                } else srcFile.readText()
            } else {
                logWarn("$masterDataFile not available")
                null
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun <T> readFile(masterDataFile: MasterDataFile, adapter: JsonAdapter<T>) = withContext(dispatchers.io) {
        masterDataFile.readContentDecrypted()?.let { json -> adapter.fromJson(json) }
    }

    fun deleteAll() {
        masterDataDir.listFiles()?.forEach { it.delete() }
    }

    suspend fun writeConfiguration(configuration: Configuration) = withContext(dispatchers.io) {
        writeFile(MasterDataFile.CONFIGURATION, configAdapter.toJson(configuration))
    }

    suspend fun writeSites(sites: Sites) = withContext(dispatchers.io) {
        writeFile(MasterDataFile.SITES, sitesAdapter.toJson(sites))
    }

    suspend fun writeVaccineSchedule(vaccineSchedule: VaccineSchedule) = withContext(dispatchers.io) {
        writeFile(MasterDataFile.VACCINE_SCHEDULE, vaccineScheduleAdapter.toJson(vaccineSchedule))
    }

    suspend fun writeSubstancesConfig(substancesConfig: SubstancesConfig) = withContext(dispatchers.io) {
        writeFile(MasterDataFile.SUBSTANCES_CONFIG, substancesConfigAdapter.toJson(substancesConfig))
    }

    suspend fun writeSubstancesGroupConfig(substancesGroupConfig: SubstancesGroupConfig) = withContext(dispatchers.io) {
        writeFile(MasterDataFile.SUBSTANCES_GROUP_CONFIG, substancesGroupConfigAdapter.toJson(substancesGroupConfig))
    }

    suspend fun writeAddressHierarchy(addressHierarchy: AddressHierarchyDto) = withContext(dispatchers.io) {
        writeFile(MasterDataFile.ADDRESS_HIERARCHY, addressHierarchyAdapter.toJson(addressHierarchy))
    }

    suspend fun writeLocalizationMap(localization: LocalizationMapDto) = withContext(dispatchers.io) {
        writeFile(MasterDataFile.LOCALIZATION, localizationAdapter.toJson(localization))
    }

    fun storeDateModifiedOrThrow(masterDataFile: MasterDataFile, dateModified: SyncDate) {
        val success = storeDateModified(masterDataFile, dateModified)
        if (!success)
            error("Failed to set date modified for $masterDataFile")
    }

    private fun storeDateModified(masterDataFile: MasterDataFile, dateModified: SyncDate): Boolean {
        val f = masterDataFile(masterDataFile.fileName)
        if (f.canRead()) {
            return f.setLastModified(dateModified.time)
        } else {
            logWarn("file $masterDataFile cannot be read so cannot set date modified")
        }

        return false
    }


    fun getDateModified(masterDataFile: MasterDataFile): SyncDate? {
        val f = masterDataFile(masterDataFile.fileName)
        if (f.canRead()) {
            val lastModified = f.lastModified()
            if (lastModified > 0) {
                return SyncDate(lastModified)
            }
        }

        return null
    }

    suspend fun readLocalizationMap(): LocalizationMapDto? = readFile(MasterDataFile.LOCALIZATION, localizationAdapter)

    suspend fun readSites(): Sites? = readFile(MasterDataFile.SITES, sitesAdapter)

    suspend fun readConfiguration(): Configuration? = readFile(MasterDataFile.CONFIGURATION, configAdapter)

    suspend fun readAddressHierarchy(): AddressHierarchyDto? = readFile(MasterDataFile.ADDRESS_HIERARCHY, addressHierarchyAdapter)

    suspend fun readVaccineSchedule(): VaccineSchedule? = readFile(MasterDataFile.VACCINE_SCHEDULE, vaccineScheduleAdapter)

    suspend fun readSubstanceConfig(): SubstancesConfig? = readFile(MasterDataFile.SUBSTANCES_CONFIG, substancesConfigAdapter)

    suspend fun readSubstancesGroupConfig(): SubstancesGroupConfig? = readFile(MasterDataFile.SUBSTANCES_GROUP_CONFIG, substancesGroupConfigAdapter)

    suspend fun md5Hash(masterDataFile: MasterDataFile): String? {
        return masterDataFile.readContentDecrypted()?.let { md5HashGenerator.md5(it) }
    }
}