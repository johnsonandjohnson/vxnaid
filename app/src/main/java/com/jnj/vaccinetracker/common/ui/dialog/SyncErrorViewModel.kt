package com.jnj.vaccinetracker.common.ui.dialog

import android.net.Uri
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.helpers.AndroidFiles
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import com.jnj.vaccinetracker.common.ui.format
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.config.appSettings
import com.jnj.vaccinetracker.sync.data.models.SyncDate
import com.jnj.vaccinetracker.sync.domain.entities.BuildSyncErrorsFile
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorMetadata
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorOverview
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorOverviewDisplay
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import com.jnj.vaccinetracker.sync.domain.usecases.error.BuildSyncErrorsFileUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.error.DeleteAllSyncErrorsUseCase
import com.jnj.vaccinetracker.sync.domain.usecases.error.FindAllSyncErrorMetadataUnresolvedUseCase
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.inject.Inject

class SyncErrorViewModel @Inject constructor(
    override val dispatchers: AppCoroutineDispatchers,
    private val buildSyncErrorsFileUseCase: BuildSyncErrorsFileUseCase,
    private val androidFiles: AndroidFiles,
    private val findAllSyncErrorMetadataUnresolvedUseCase: FindAllSyncErrorMetadataUnresolvedUseCase,
    private val deleteAllSyncErrorsUseCase: DeleteAllSyncErrorsUseCase,
    private val syncLogger: SyncLogger,
) : ViewModelBase() {

    val loading = mutableLiveBoolean(false)
    val lastSyncDate = mutableLiveData<LastSyncDate?>()
    val errorsFileToShare = eventFlow<ErrorsFileToShareUiModel>()
    val deleteAllErrorsCompletedEvent = eventFlow<Boolean>()

    val showDeleteAllSyncErrorsButton = appSettings.showDeleteSyncErrorsButton
    val items = mutableLiveData<List<SyncErrorOverviewUiModel>>()

    val showShareButton = appSettings.showShareSyncErrorsButton

    init {
        initState()
    }

    private fun initState() {
        loadErrors()
        loadSyncDate()
    }

    private fun loadSyncDate() = scope.launch {
        lastSyncDate.value = syncLogger.getSyncCompletedDate()?.let { LastSyncDate(it) }
    }

    private fun SyncErrorOverview.isBiometricUploadError() = metadata is SyncErrorMetadata.UploadBiometricsTemplate

    private fun List<SyncErrorOverview>.toUiModels(): List<SyncErrorOverviewUiModel> {
        val regularErrors = filter { !it.isBiometricUploadError() }.map { SyncErrorOverviewUiModel.Regular(it) }
        val biometricError: SyncErrorOverviewUiModel.UploadBiometric? =
            filter { it.isBiometricUploadError() }.takeIf { it.isNotEmpty() }
                ?.let { SyncErrorOverviewUiModel.UploadBiometric(it) }
        return regularErrors + listOfNotNull(biometricError)
    }

    private fun loadErrors() = scope.launch {
        loading.value = true
        try {
            items.value = findAllSyncErrorMetadataUnresolvedUseCase.findAllSyncErrorMetadataUnresolved().toUiModels()
        } finally {
            loading.value = false
        }
    }

    fun onShareClick() = scope.launch {
        if (!loading.value) {
            loading.value = true
            try {
                val file = buildSyncErrorsFileUseCase.buildSyncErrorsFile(BuildSyncErrorsFile.FromAll)
                val uri = androidFiles.getUriForFile(file)
                errorsFileToShare.tryEmit(ErrorsFileToShareUiModel.Success(uri))
            } catch (ex: Exception) {
                yield()
                ex.rethrowIfFatal()
                logError("failed to build sync errors file", ex)
                errorsFileToShare.tryEmit(ErrorsFileToShareUiModel.Failure)
            } finally {
                loading.value = false
            }
        }
    }

    fun onDeleteAllClick() = scope.launch {
        if (!loading.value) {
            loading.value = true
            try {
                deleteAllSyncErrorsUseCase.deleteAllSyncErrors()
                deleteAllErrorsCompletedEvent.tryEmit(true)
            } catch (ex: Exception) {
                yield()
                ex.rethrowIfFatal()
                logError("error during delete all sync errors", ex)
                deleteAllErrorsCompletedEvent.tryEmit(false)
            } finally {
                loading.value = false
            }
        }
    }
}

sealed class SyncErrorOverviewUiModel : SyncErrorOverviewDisplay() {
    data class Regular(val syncError: SyncErrorOverview) : SyncErrorOverviewUiModel() {
        override val displayDate: String
            get() = syncError.displayDate

        override fun displayErrorMessage(context: ResourcesWrapper): String {
            return syncError.displayErrorMessage(context)
        }
    }

    data class UploadBiometric(val syncErrors: List<SyncErrorOverview>) : SyncErrorOverviewUiModel() {
        init {
            require(syncErrors.isNotEmpty()) { "syncErrors must not be empty" }
            require(syncErrors.all { it.metadata is SyncErrorMetadata.UploadBiometricsTemplate }) { "syncErrors must all be of type UploadBiometricsTemplate" }
        }

        override val displayDate: String
            get() = syncErrors.maxByOrNull { it.dateCreated }!!.displayDate

        override fun displayErrorMessage(context: ResourcesWrapper): String {
            return context.getString(R.string.upload_biometrics_templates_error, syncErrors.size)
        }
    }
}

sealed class ErrorsFileToShareUiModel {
    data class Success(val uri: Uri) : ErrorsFileToShareUiModel()
    object Failure : ErrorsFileToShareUiModel()
}

data class LastSyncDate(val syncDate: SyncDate) {

    val displayDate get() = syncDate.format()
}