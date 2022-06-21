package com.jnj.vaccinetracker.common.data.managers

import com.jnj.vaccinetracker.BuildConfig
import com.jnj.vaccinetracker.common.domain.entities.VaccineTrackerVersion
import com.jnj.vaccinetracker.common.domain.usecases.GetLatestVersionUseCase
import com.jnj.vaccinetracker.common.helpers.*
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton


/**
 * @author druelens
 * @version 1
 */
@Singleton
class UpdateManager @Inject constructor(
    private val getLatestVersionUseCase: GetLatestVersionUseCase,
    private val dispatchers: AppCoroutineDispatchers,
    private val versionUpdateObserver: VersionUpdateObserver,
) {

    private val job = SupervisorJob()
    private val scope
        get() = CoroutineScope(dispatchers.main + job)

    private var latestVersionCache: VaccineTrackerVersion? = null


    fun checkLatestVersion() {
        scope.launch {
            try {
                clearLatestVersionCache()
                if (!isLatestVersion()) {
                    logInfo("New app version available")
                    versionUpdateObserver.notifyUpdateAvailable()
                }
            } catch (ex: Exception) {
                yield()
                ex.rethrowIfFatal()
                logError("something went wrong when fetching latest version", ex)
            }
        }
    }

    suspend fun getLatestVersion(): VaccineTrackerVersion {
        latestVersionCache?.let { return it }

        return getLatestVersionUseCase.getLatestVersion()
            .also { latestVersionCache = it }
    }

    suspend fun isLatestVersion(): Boolean {
        return BuildConfig.VERSION_CODE >= getLatestVersion().version
    }

    fun clearLatestVersionCache() {
        latestVersionCache = null
    }
}