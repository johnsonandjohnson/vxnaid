package com.jnj.vaccinetracker.barcode

import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.domain.entities.Manufacturer
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.logVerbose
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import com.jnj.vaccinetracker.common.validators.ParticipantIdValidator
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.inject.Inject

class ScanBarcodeViewModel @Inject constructor(
    private val participantIdValidator: ParticipantIdValidator,
    override val dispatchers: AppCoroutineDispatchers,
    private val resourcesWrapper: ResourcesWrapper,
    private val configurationManager: ConfigurationManager,
) : ViewModelBase() {

    val scannedBarcode = mutableLiveData<String>()
    val flashOn = mutableLiveBoolean()
    val barcodeValid = mutableLiveBoolean()
    val barcodeErrorMessage = mutableLiveData<String>()
    private val participantArg = stateFlow<ParticipantSummaryUiModel?>(null)
    private var validateIdJob: Job? = null
    private val retryClickEvents = eventFlow<Unit>()
    val participant = mutableLiveData<ParticipantSummaryUiModel>()
    val manufacturerNames = mutableLiveData<List<String>>()
    val selectedManufacturerName = mutableLiveData<String>()

    val loading = mutableLiveBoolean()
    val errorMessage = mutableLiveData<String>()

    private var manufacturers: MutableList<Manufacturer> = mutableListOf()

    fun toggleFlash() {
        flashOn.set(!flashOn.value)
    }

    init {
        initState()
    }

    private suspend fun load() {
        try {
            val config = configurationManager.getConfiguration()
            onManufacturersLoaded(config.manufacturers)
            loading.set(false)
        } catch (ex: Throwable) {
            yield()
            ex.rethrowIfFatal()
            loading.set(false)
            errorMessage.set(resourcesWrapper.getString(R.string.general_label_error))
            logError("Failed to load configuration: ", ex)
        }
    }

    private fun initState() {
        loading.set(true)
        errorMessage.set(null)
        participantArg.filterNotNull()
            .onEach { participant ->
                this.participant.value = participant
            }
            .combine(retryClickEvents.asFlow()) { _, _ ->
                loading.set(true)
                errorMessage.set(null)
                load()
            }
            .launchIn(scope)

        retryClickEvents.tryEmit(Unit)
    }

    fun setArguments(participant: ParticipantSummaryUiModel) {
        this.participantArg.value = participant
    }

    fun onBarcodeScanned(result: String, flag: String, manufacturers: List<Manufacturer>) {
        logVerbose("Got barcode: $result")

        val barcode = formatBarcode(result) ?: return
        if (barcode.isNotEmpty()) {
            validateIdJob?.cancel()
            this.manufacturers.clear()
            this.manufacturers.addAll(manufacturers)
            validateIdJob = scope.launch {
                when (flag) {
                    ScanBarcodeActivity.PARTICIPANT -> validateParticipantId(barcode)
                    else -> validateManufacturerId(barcode)
                }
            }
        }
    }

    private fun onManufacturersLoaded(manufacturers: List<Manufacturer>) {
        val productNameList = manufacturers.map { it.name }
        this.manufacturerNames.set(productNameList)
        this.manufacturers.addAll(manufacturers)
    }

    fun getManufacturers(): List<Manufacturer> {
        return manufacturers
    }

    private suspend fun validateParticipantId(barcode: String) {
        resetErrorMessage()
        if (barcode.isEmpty() || !participantIdValidator.validate(barcode)) {
            updateBarcodeState(isValid = false, barcode)
            barcodeErrorMessage.set(resourcesWrapper.getString(R.string.participant_flow_participant_id_scanned))
        } else {
            updateBarcodeState(isValid = true, barcode)
        }
    }

    private fun validateManufacturerId(barcode: String) {
        resetErrorMessage()
        if (barcode.isEmpty() || !matchBarcodeManufacturer(barcode)) {
            updateBarcodeState(isValid = false, barcode)
            barcodeErrorMessage.set(resourcesWrapper.getString(R.string.participant_flow_participant_id_scanned))
        } else {
            updateBarcodeState(isValid = true, barcode)
        }
    }

    fun matchBarcodeManufacturer(barcode: CharSequence): Boolean {
        manufacturers.forEach { manufacturer ->
            if (manufacturer.barcodeRegex.toRegex().containsMatchIn(barcode)) {
                selectedManufacturerName.value = manufacturer.name
                return true
            }
        }
        return false
    }

    private fun updateBarcodeState(isValid: Boolean, barcode: String) {
        scannedBarcode.set(barcode)
        barcodeValid.set(isValid)
    }

    private fun resetErrorMessage() {
        barcodeErrorMessage.set(null)
    }
}
