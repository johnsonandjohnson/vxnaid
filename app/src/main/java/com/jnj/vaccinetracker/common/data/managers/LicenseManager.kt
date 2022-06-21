package com.jnj.vaccinetracker.common.data.managers

import android.content.Context
import com.jnj.vaccinetracker.common.data.encryption.SecretSharedPreferences
import com.jnj.vaccinetracker.common.data.helpers.AndroidFiles
import com.jnj.vaccinetracker.common.data.helpers.license.ObtainedLicenseLogger
import com.jnj.vaccinetracker.common.data.models.LicenseType
import com.jnj.vaccinetracker.common.data.models.api.request.GetLicensesRequest
import com.jnj.vaccinetracker.common.data.models.api.request.ReleaseLicensesRequest
import com.jnj.vaccinetracker.common.domain.entities.ActivatedLicense
import com.jnj.vaccinetracker.common.domain.entities.LicenseObtainedStatus
import com.jnj.vaccinetracker.common.exceptions.LicensesNotObtainedException
import com.jnj.vaccinetracker.common.exceptions.ManualLicenseActivationRequiredException
import com.jnj.vaccinetracker.common.exceptions.NoNetworkException
import com.jnj.vaccinetracker.common.exceptions.WebCallException
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.config.appSettings
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorMetadata
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import com.neurotec.lang.NCore
import com.neurotec.licensing.NLicense
import com.neurotec.licensing.NLicenseManager
import com.tfcporciuncula.flow.FlowSharedPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for Neurotechnology license retrieval and activation.
 *
 * @author druelens
 * @version 2
 */
@Suppress("BlockingMethodInNonBlockingContext")
@Singleton
class LicenseManager @Inject constructor(
    androidFiles: AndroidFiles,
    private val apiService: VaccineTrackerSyncApiDataSource,
    private val dispatchers: AppCoroutineDispatchers,
    private val secretSharedPreferences: SecretSharedPreferences,
    private val prefs: FlowSharedPreferences,
    private val network: NetworkConnectivity,
    private val syncLogger: SyncLogger,
    private val isEmulator: IsEmulator,
    private val obtainedLicenseLogger: ObtainedLicenseLogger,
) {

    companion object {
        private const val LICENSE_SERVER_PORT = 5000
        fun initState(context: Context) {
            NLicenseManager.setTrialMode(false) // Trial mode works only on hardware device, not in emulator
            NCore.setContext(context)
            System.setProperty("jna.nounpack", "true")
            System.setProperty("java.io.tmpdir", context.cacheDir.absolutePath)
        }
    }

    private val writeDir = androidFiles.externalFiles.path

    private val mutex = Mutex()

    init {
        setWritableStoragePath()
    }

    /**
     * Sets the writable storage path for the NLicenseManager if not already set.
     *
     * The Neurotechnology library expects this to be set only once before initialization of either
     * the NLicenseManager or the NBioMetricsClient.
     */
    fun setWritableStoragePath() {

        if (NLicenseManager.getWritableStoragePath() != writeDir) {
            NLicenseManager.setWritableStoragePathN(writeDir)
        }
    }

    private suspend fun getLicenseFromApiOrThrow(licenseType: LicenseType): String {
        val apiSyncErrorMetadata = SyncErrorMetadata.License(licenseType, SyncErrorMetadata.License.Action.GET_LICENSE_CALL)
        return try {
            val serialLicense = getSerialLicenseFromAPI(licenseType).also {
                syncLogger.clearSyncError(apiSyncErrorMetadata)
            } ?: throw Exception("Received null license for $licenseType from backend")
            logInfo("serial license from backend: {}", serialLicense)
            serialLicense
        } catch (ex: Exception) {
            yield()
            ex.rethrowIfFatal()
            syncLogger.logSyncError(apiSyncErrorMetadata, ex)
            logError("getSerialLicenseFromAPI failed: ", ex)
            throw ex
        }
    }

    private suspend fun activateLicense(licenseType: LicenseType, serialLicense: String): ActivatedLicense {
        val activateLicenseSyncErrorMetadata = SyncErrorMetadata.License(licenseType, SyncErrorMetadata.License.Action.ACTIVATE_LICENSE)
        return try {
            // Generate device- and app-specific ID to activate the serial number
            val activationId = NLicense.generateID(serialLicense)
            logInfo("Generated $licenseType activation id")
            // Online activation of the license
            val activatedLicense = NLicense.activateOnline(activationId)
            logInfo("Activated $licenseType license")

            // Save activated license to shared preferences
            saveStoredLicense(licenseType, activatedLicense)
            // clear possible existing sync error
            syncLogger.clearSyncError(activateLicenseSyncErrorMetadata)
            getStoredLicense(licenseType) ?: error("getStoredLicense returns null right after saveStoredLicense for $licenseType")
        } catch (ex: Exception) {
            yield()
            ex.rethrowIfFatal()
            syncLogger.logSyncError(activateLicenseSyncErrorMetadata, ex)
            logError("error during license activation $licenseType", ex)
            throw ex
        }
    }

    private fun LicenseType.isComponentActivated(): Boolean {
        return NLicense.isComponentActivated(primaryComponent) && getStoredLicense(this) != null
    }

    /**
     * Obtain the license of the given type within the application.
     * This will first check if it is already activated,
     * if not, it will check if an activated product license is saved which can be obtained in the application
     * if not, a new serial license will be requested from the backend
     *
     * @param licenseType   LicenseType to activate within the application
     * @return Boolean: true if successful, false if not.
     */
    private suspend fun obtainLicense(licenseType: LicenseType, fromUserInput: Boolean): LicenseObtainedStatus = withContext(dispatchers.io) {
        try {
            // STEP 1: Check if component already activated
            if (licenseType.isComponentActivated()) {
                // License already activated
                logInfo("License $licenseType already activated")
                return@withContext LicenseObtainedStatus.OBTAINED
            }

            // STEP 2: Check if activated product license saved and we can re-obtain the product
            // Try to load encrypted license from shared preferences
            val storedLicense = getStoredLicense(licenseType)
            if (storedLicense != null) {
                logInfo("License $licenseType obtained from stored activation")
                return@withContext obtainFromString(storedLicense)
            }
            if (appSettings.manualLicenseActivationRequired && !fromUserInput)
                throw ManualLicenseActivationRequiredException(licenseType)

            // STEP 3: Otherwise, we need to get a new serial number license
            // Retrieve serial license number
            val serialLicense = getLicenseFromApiOrThrow(licenseType)

            // activate online
            val activatedLicense = activateLicense(licenseType, serialLicense)

            // obtain product
            obtainFromString(activatedLicense)
        } catch (ex: Throwable) {
            if (ex is LicensesNotObtainedException) {
                throw ex
            } else
                throw LicensesNotObtainedException("couldn't obtain license $licenseType", ex, null)
        }
    }

    /**
     * Retrieve serial license string from API
     *
     * @param licenseType   LicenseType for which to retrieve the license key
     * @return String representing a serial license
     */
    @Throws(NoNetworkException::class)
    private suspend fun getSerialLicenseFromAPI(licenseType: LicenseType): String? {
        val request = GetLicensesRequest(listOf(licenseType))
        return apiService.getLicenses(request).licenses.firstOrNull()
            ?.let { license ->
                if (license.type == licenseType && license.value != null) {
                    logInfo("Obtained serial license from API")
                    return@let license.value
                }
                return null
            }
    }

    private suspend fun emulatorCleanUp() {
        require(isEmulator.value)
        LicenseType.values().forEach { licenseType ->
            try {
                apiService.releaseLicenses(ReleaseLicensesRequest(listOf(licenseType)))
            } catch (ex: Exception) {
                yield()
                ex.rethrowIfFatal()
                logWarn("release license $licenseType failed (probably nothing to release)", ex)
            }
        }
    }

    suspend fun getLicensesOrThrow(fromUserInput: Boolean = false, licenseTypes: List<LicenseType>) {
        logInfo("getLicensesOrThrow $fromUserInput $licenseTypes")
        fun createError(cause: Throwable?, licenseObtainedStatus: LicenseObtainedStatus?) =
            LicensesNotObtainedException("Couldn't obtain licenses: $licenseTypes", cause, licenseObtainedStatus)

        val licenseStatus = try {
            getLicenses(swallowErrors = false, fromUserInput = fromUserInput, licenseTypes = licenseTypes)
        } catch (ex: Throwable) {
            if (ex is LicensesNotObtainedException)
                throw ex
            else
                throw createError(ex, null)
        }
        if (licenseStatus != LicenseObtainedStatus.OBTAINED)
            throw createError(null, licenseStatus)
    }


    private suspend fun releaseLicenseApiSilently(licenseType: LicenseType) {
        try {
            apiService.releaseLicenses(ReleaseLicensesRequest(listOf(licenseType)))
        } catch (ex: WebCallException) {
            when (ex.code) {
                404 -> {
                    logWarn("No license of type $licenseType assigned to this device")
                    //No-op
                }
                else -> throw ex
            }
        }
    }


    private suspend fun releaseLicensesIfNeeded(licenseTypes: List<LicenseType>) {
        val licsToRelease = licenseTypes.filter { shouldReleaseLicense(it) }
        licsToRelease.forEach { lic ->
            val errorMetadata = SyncErrorMetadata.License(lic, SyncErrorMetadata.License.Action.RELEASE_LICENSE_CALL)
            try {
                releaseLicenseApiSilently(lic)
                setShouldReleaseLicense(lic, false)
                syncLogger.clearSyncError(errorMetadata)
            } catch (ex: Exception) {
                yield()
                ex.rethrowIfFatal()
                logError("release licenses fail $lic", ex)
                syncLogger.logSyncError(errorMetadata, ex)
            }
        }
    }

    /**
     * Checks if the license is already activated, and if not, obtains it.
     *
     * @param licenseTypes   LicenseTypes to be activated
     * @param swallowErrors if true, [LicensesNotObtainedException] will be swallowed but subsequent [licenseTypes] will still not be obtained
     * @return True if successfully obtained license, false if not.
     */
    suspend fun getLicenses(swallowErrors: Boolean = false, fromUserInput: Boolean = false, licenseTypes: List<LicenseType>): LicenseObtainedStatus =
        withContext(dispatchers.io) {
            mutex.withLock {
                if (isEmulator.value) {
                    logWarn("licenses not supported on emulators!!! releasing licenses...")
                    emulatorCleanUp()
                    return@withLock LicenseObtainedStatus.NOT_OBTAINED
                }
                releaseLicensesIfNeeded(licenseTypes.toList())
                var result = LicenseObtainedStatus.OBTAINED
                for (licenseType in licenseTypes) {
                    result = try {
                        if (result.isObtained)
                            result and obtainLicense(licenseType, fromUserInput)
                        else
                            result
                    } catch (ex: LicensesNotObtainedException) {
                        yield()
                        ex.rethrowIfFatal()
                        logError("obtain license fail $licenseType", ex)
                        if (!swallowErrors)
                            throw ex
                        else
                            LicenseObtainedStatus.NOT_OBTAINED
                    }
                }
                return@withContext result
            }
        }

    /*
     * Full stacktrace looks like this:
     * +   at NLicenseGenerateDeactivationIdA
     * --- End of external call stack ---
     * --- End of native stack trace ---
     * External error occurred
     * ExternalError: -1
	 * at com.neurotec.lang.RuntimeErrorCreator.create(RuntimeErrorCreator.java:54)
	 * at com.neurotec.lang.ChainedErrorCreator.create(ChainedErrorCreator.java:22)
	 * at com.neurotec.lang.NError.get(NError.java:174)
	 * at com.neurotec.lang.NError.get(NError.java:191)
	 * at com.neurotec.lang.NError.getLast(NError.java:205)
	 * at com.neurotec.lang.NResult.checkUnchecked(NResult.java:235)
	 * at com.neurotec.licensing.NLicense.deactivateOnline(NLicense.java:528)
	 * at com.neurotec.licensing.NLicense.deactivateOnline(NLicense.java:512)
     */
    private fun isFaultyDeactivation(ex: Exception) = ex.findMessage(messagePart = "-1")

    private suspend fun deactivateLicense(activatedLicense: ActivatedLicense, wasFaultyDeactivation: Boolean) {
        val lic = activatedLicense.licenseType
        val syncErrorMetadata = SyncErrorMetadata.License(lic, SyncErrorMetadata.License.Action.DEACTIVATE_LICENSE)
        try {
            logInfo("deactivateLicenses: Deactivating product license: ${lic.type}")
            NLicense.deactivateOnline(activatedLicense.activatedLicense)
        } catch (ex: Exception) {
            yield()
            ex.rethrowIfFatal()
            network.requireFastInternet()
            logError("failed to deactivateLicenses: $lic wasFaultyDeactivation=$wasFaultyDeactivation", ex)
            if (!wasFaultyDeactivation && isFaultyDeactivation(ex)) {
                logWarn("isFaultyDeactivation: $lic, going to try activation again then deactivating.")
                val newActivatedLicense = try {
                    NLicense.release(lic.type)
                    val serialLicense = getLicenseFromApiOrThrow(lic)
                    activateLicense(lic, serialLicense)
                } catch (ex2: Exception) {
                    logError("failed to retry activate license: $lic", ex2)
                    syncLogger.logSyncError(syncErrorMetadata, ex)
                    throw ex2
                }
                return deactivateLicense(newActivatedLicense, true)
            }
            syncLogger.logSyncError(syncErrorMetadata, ex)
            throw ex
        }
        setShouldReleaseLicense(lic, true)
        removeStoredLicense(lic).also {
            syncLogger.clearSyncError(syncErrorMetadata)
        }
        logInfo("NLicense.release $lic")
        NLicense.release(lic.type)

    }

    /**
     * Deactivate all serial licenses and remove them from the app storage
     */
    @Throws(NoNetworkException::class)
    suspend fun deactivateLicenses() = withContext(dispatchers.io) {
        network.requireFastInternet()
        var success = true
        mutex.withLock {
            val deactivatedLicenses = mutableListOf<LicenseType>()
            for (lic in LicenseType.values()) {
                val storedLicense = getStoredLicense(lic)
                if (storedLicense != null) {
                    try {
                        deactivateLicense(storedLicense, wasFaultyDeactivation = false)
                        deactivatedLicenses += lic
                    } catch (_: Exception) {
                        success = false
                    }
                } else {
                    logInfo("deactivateLicenses: no license stored for $lic")
                }
            }
            releaseLicensesIfNeeded(deactivatedLicenses)
        }
        if (!success) {
            error("Some licenses couldn't be deactivated")
        }
    }

    /**
     * Obtain license from a passed string
     *
     * @return Boolean indicating whether the license activation was successful.
     */
    @Throws(IOException::class)
    suspend fun obtainFromString(activatedLicense: ActivatedLicense): LicenseObtainedStatus = withContext(dispatchers.io) {
        val licenseType = activatedLicense.licenseType
        logInfo("obtainFromString: $licenseType")
        val obtainLicenseSyncErrorMetadata = SyncErrorMetadata.License(activatedLicense.licenseType, SyncErrorMetadata.License.Action.OBTAIN_ACTIVATED_LICENSE)
        try {
            NLicense.add(activatedLicense.activatedLicense)
        } catch (e: IOException) {
            yield()
            e.rethrowIfFatal()
            syncLogger.logSyncError(obtainLicenseSyncErrorMetadata, Exception("NLicense.add error", e))
            throw e
        }

        val isObtained = try {
            obtainProducts(licenseType.type)
        } catch (ex: Exception) {
            yield()
            ex.rethrowIfFatal()
            syncLogger.logSyncError(obtainLicenseSyncErrorMetadata, Exception("obtainProducts error", ex))
            throw ex
        }
        if (isObtained) {
            obtainedLicenseLogger.logObtainedLicense(activatedLicense)
            syncLogger.clearSyncError(obtainLicenseSyncErrorMetadata)
            // we don't care anymore about deactivation sync errors if obtain is successful
            syncLogger.clearSyncError(SyncErrorMetadata.License(licenseType, SyncErrorMetadata.License.Action.DEACTIVATE_LICENSE))
            LicenseObtainedStatus.OBTAINED
        } else {
            val isObtainable = obtainedLicenseLogger.isObtainable(activatedLicense)
            if (!isObtainable) {
                logWarn("NLicense.obtain is false for $licenseType but was already obtained previously during this session so app must force close")
                throw LicensesNotObtainedException(
                    "NLicense.obtain is false for $licenseType but was already obtained previously during this session so app must force close",
                    licenseObtainedStatus = LicenseObtainedStatus.OBTAINABLE_AFTER_FORCE_CLOSE
                )
            } else {
                syncLogger.logSyncError(obtainLicenseSyncErrorMetadata, Exception("NLicense.obtain is false for $licenseType"))
                LicenseObtainedStatus.NOT_OBTAINED
            }

        }
    }

    /**
     * Activates licenses for the requested components from available NLicense
     *
     * @param product    Strings denoting the Neurotechnology product license to activate
     * @return Boolean indicating whether the license activation was successful.
     */
    @Throws(IOException::class)
    suspend fun obtainProducts(product: String): Boolean = withContext(dispatchers.io) {
        val available: Boolean = NLicense.obtain("/local", LICENSE_SERVER_PORT, product)
        logInfo("Obtaining '$product' license ${if (available) "succeeded" else "failed"}.")
        available
    }

    /**
     * Return a list of all stored saved activated license types.
     * This only returns the type, not the actual license.
     *
     * @return List of LicenseType objects that are stored in EncryptedSharedPreferences
     */
    fun getActivatedLicenseTypes(): List<LicenseType> {
        val licenseList = mutableListOf<LicenseType>()
        for (lic in LicenseType.values()) {
            val storedLicense = getStoredLicense(lic)
            if (storedLicense != null) {
                licenseList.add(lic)
            }
        }
        return licenseList
    }

    private fun prefsKey(licenseType: LicenseType): String {
        return "PREF_${licenseType.name}_LICENSE"
    }

    private fun prefsKeyShouldRelease(licenseType: LicenseType): String {
        return "PREF_SHOULD_RELEASE_${licenseType.name}_LICENSE"
    }

    private fun getStoredLicense(licenseType: LicenseType): ActivatedLicense? {
        return secretSharedPreferences.getString(prefsKey(licenseType))
            ?.takeIf { it.isNotEmpty() }?.let { serialLicense -> ActivatedLicense(licenseType = licenseType, activatedLicense = serialLicense) }
    }

    private fun setShouldReleaseLicense(licenseType: LicenseType, shouldRelease: Boolean) {
        prefs.getBoolean(prefsKeyShouldRelease(licenseType)).set(shouldRelease)
    }

    private fun shouldReleaseLicense(licenseType: LicenseType): Boolean {
        return prefs.getBoolean(prefsKeyShouldRelease(licenseType)).get()
    }

    private suspend fun removeStoredLicense(licenseType: LicenseType) {
        secretSharedPreferences.remove(prefsKey(licenseType))
    }

    private suspend fun saveStoredLicense(licenseType: LicenseType, license: String) {
        secretSharedPreferences.putString(prefsKey(licenseType), license)
    }

}