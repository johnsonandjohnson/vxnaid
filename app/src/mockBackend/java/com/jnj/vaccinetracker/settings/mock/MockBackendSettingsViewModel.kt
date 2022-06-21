package com.jnj.vaccinetracker.settings.mock

import android.net.Uri
import com.jnj.vaccinetracker.common.data.helpers.AndroidFiles
import com.jnj.vaccinetracker.common.domain.entities.SizeUnit
import com.jnj.vaccinetracker.common.domain.entities.SyncEntityType
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.config.MockAppSettings
import com.jnj.vaccinetracker.fake.data.network.FakeEngineSettings
import com.jnj.vaccinetracker.settings.domain.usecasse.GenerateFatParticipantUseCase
import com.jnj.vaccinetracker.settings.domain.usecasse.GenerateTemplatesUseCase
import com.jnj.vaccinetracker.sync.domain.helpers.ForceSyncObserver
import com.jnj.vaccinetracker.sync.p2p.domain.usecases.GetDatabaseSizeUseCase
import com.jnj.vaccinetracker.timetracker.data.P2PTimeTracker
import com.jnj.vaccinetracker.timetracker.data.TimeLeapBuilder
import com.jnj.vaccinetracker.timetracker.data.TimeTrackerReportFileProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import com.jnj.vaccinetracker.config.mockAppSettings as mockSettings

class MockBackendSettingsViewModel @Inject constructor(
    override val dispatchers: AppCoroutineDispatchers,
    private val fakeEngineSettings: FakeEngineSettings,
    private val forceSyncObserver: ForceSyncObserver,
    private val timeLeapBuilder: TimeLeapBuilder,
    private val filesDirProvider: AndroidFiles,
    private val timeTrackerReportFileProvider: TimeTrackerReportFileProvider,
    private val generateFatParticipantUseCase: GenerateFatParticipantUseCase,
    private val generateTemplatesUseCase: GenerateTemplatesUseCase,
    private val p2PTimeTracker: P2PTimeTracker,
    private val getDatabaseSizeUseCase: GetDatabaseSizeUseCase
) : ViewModelBase() {

    companion object {
        private val Long.hours get() = TimeUnit.MILLISECONDS.toHours(this)
        private val Long.seconds get() = TimeUnit.MILLISECONDS.toSeconds(this)
        private val Long.minutes get() = TimeUnit.MILLISECONDS.toMinutes(this)
        fun formatTime(timeMs: Long): String {
            val h = timeMs.hours
            val m = (timeMs - TimeUnit.HOURS.toMillis(h)).minutes
            val s = (timeMs - TimeUnit.MINUTES.toMillis(m) - TimeUnit.HOURS.toMillis(h)).seconds
            return "${h}h${m}m${s}s"
        }

    }

    val timeTaken = mutableLiveData<String?>()
    val participantCount = mutableLiveData<Long>()
    val visitCount = mutableLiveData<Long>()
    val imageCount = mutableLiveData<Long>()
    val templateCount = mutableLiveData<Long>()
    val shareTimeTrackerReportEvent = eventFlow<ShareTimeTrackerReport>()
    val mockAppSettings: MockAppSettings = mockSettings
    fun observeParticipantGenerationCount() = fakeEngineSettings.observeTargetParticipantCount()
    val isGeneratingTemplates = mutableLiveBoolean()
    val isGeneratingFatParticipants = mutableLiveBoolean()
    val generateFatParticipantsAmount = mutableLiveData<String?>()
    val generateTemplatesAmount = mutableLiveData<String?>()
    val p2PTransferTime = mutableLiveData<String?>()
    val databaseSize = mutableLiveData<String?>()

    init {
        scope.launch {
            while (true) {
                val leap = timeLeapBuilder.buildTimeLeap()
                timeTaken.value = formatTime(leap.timeStamp)
                participantCount.value = leap.getCount(SyncEntityType.PARTICIPANT)
                visitCount.value = leap.getCount(SyncEntityType.VISIT)
                templateCount.value = leap.getCount(SyncEntityType.BIOMETRICS_TEMPLATE)
                imageCount.value = leap.getCount(SyncEntityType.IMAGE)
                databaseSize.value = getDatabaseSizeUseCase.getDatabaseSize().let { "${bytesToMb(it)} MB" }
                delay(2500)
            }
        }

        p2PTimeTracker.observeRecordedTime().onEach {
            p2PTransferTime.value = formatTime(it)
        }.launchIn(scope)
    }

    private fun bytesToMb(bytes: Long): String = SizeUnit.B.toMB(bytes.toDouble()).toFloat().let { "%.2f".format(it) }

    fun onParticipantGenerationCountChanged(participantGenerationCount: Int) {
        if (fakeEngineSettings.getTargetParticipantCount() != participantGenerationCount) {
            fakeEngineSettings.setTargetParticipantCount(participantGenerationCount)
            scope.launch {
                kotlin.runCatching {
                    //trigger re sync
                    forceSyncObserver.forceSync()
                }
            }
        } else {
            logInfo("onParticipantGenerationCountChanged unchanged")
        }
    }

    fun onShareTimeTrackerReportClick() {
        val file = timeTrackerReportFileProvider.provideTimeTrackerReportFile()
        if (!file.exists()) {
            logError("no time tracker report exists")
            return
        }
        val uri = filesDirProvider.getUriForFile(file)
        shareTimeTrackerReportEvent.tryEmit(ShareTimeTrackerReport(file.name, uri))
    }

    fun onGenerateTemplatesClick() {
        scope.launch {
            isGeneratingTemplates.value = true
            try {
                generateTemplates(generateTemplatesAmount.value?.toIntOrNull() ?: 0)
            } finally {
                isGeneratingTemplates.value = false
            }
        }
    }

    fun onTemplateAmountChanged(count: CharSequence?) {
        logInfo("onTemplateAmountChanged: $count")
        generateTemplatesAmount.value = count?.toString()
    }

    fun onFatParticipantAmountChanged(count: CharSequence?) {
        logInfo("onFatParticipantAmountChanged: $count")
        generateFatParticipantsAmount.value = count?.toString()
    }


    private suspend fun generateTemplates(count: Int) {
        logInfo("generateTemplates: $count")
        repeat(count) {
            generateTemplatesUseCase.generate()
        }
    }

    private suspend fun generateFatParticipants(count: Int) {
        logInfo("generateFatParticipants: $count")
        repeat(count) {
            generateFatParticipantUseCase.generate()
        }
    }

    fun onGenerateFatParticipantsClick() {
        scope.launch {
            isGeneratingFatParticipants.value = true
            try {
                generateFatParticipants(generateFatParticipantsAmount.value?.toIntOrNull() ?: 0)
            } finally {
                isGeneratingFatParticipants.value = false
            }
        }
    }

}

data class ShareTimeTrackerReport(val fileName: String, val uri: Uri)