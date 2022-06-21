package com.jnj.vaccinetracker.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import com.jnj.vaccinetracker.VaccineTrackerApplication
import com.jnj.vaccinetracker.common.data.helpers.AndroidFiles
import com.jnj.vaccinetracker.common.data.managers.LicenseManager
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.LogFileProvider
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.config.appSettings
import com.jnj.vaccinetracker.sync.domain.helpers.ForceSyncObserver
import kotlinx.coroutines.launch
import javax.inject.Inject

class SettingsViewModel @Inject constructor(
    val userRepository: UserRepository,
    override val dispatchers: AppCoroutineDispatchers,
    private val licenseManager: LicenseManager,
    private val resourcesWrapper: ResourcesWrapper,
    private val application: VaccineTrackerApplication,
    private val forceSyncObserver: ForceSyncObserver,
    private val androidFiles: AndroidFiles,
    private val logFileProvider: LogFileProvider,
) : ViewModelBase() {

    val deviceId = mutableLiveData<String>()

    val rerunSetupWizardEvent = eventFlow<Unit>()
    val shareLogsEvents = eventFlow<Uri>()

    val showSyncNowButton = appSettings.showSyncNowButton
    val showShareLogsButton = appSettings.showShareLogsButton
    val showClearLogsButton = appSettings.showClearLogsButton


    private var isSharing = false

    init {
        initState()
    }

    private fun initState() {
        deviceId.set(userRepository.getDeviceGuid())
    }

    fun syncNow() {
        forceSyncObserver.forceSync()
    }

    fun onClearLogsClick() {
        logFileProvider.deleteAll()
    }

    fun onShareLogsClick() {
        if (isSharing) {
            return
        }
        scope.launch {
            isSharing = true
            try {
                val file = logFileProvider.createJoinedLogFile()
                if (!file.exists()) {
                    logError("no log file exists")
                    return@launch
                }
                val uri = androidFiles.getUriForFile(file)
                shareLogsEvents.tryEmit(uri)
            } finally {
                isSharing = false
            }
        }
    }


    fun copyDeviceIdToClipboard() {
        val clipboard = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("VMPdeviceId", deviceId.get())
        clipboard.setPrimaryClip(clip)
    }

    fun rerunSetupWizard() {
        rerunSetupWizardEvent.tryEmit(Unit)
    }
}
