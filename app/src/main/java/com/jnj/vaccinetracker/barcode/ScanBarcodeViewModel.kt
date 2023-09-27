package com.jnj.vaccinetracker.barcode

import android.util.Log
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.encryption.SharedPreference
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
    private val manufacturerRegexes = mutableLiveData<List<Manufacturer>>()
    private val participantArg = stateFlow<ParticipantSummaryUiModel?>(null)
    private var validateIdJob: Job? = null
    private val retryClickEvents = eventFlow<Unit>()
    val participant = mutableLiveData<ParticipantSummaryUiModel>()
    val manufacturerList = mutableLiveData<List<String>>()

    val loading = mutableLiveBoolean()
    val errorMessage = mutableLiveData<String>()

    private var manufacturersList: MutableList<Manufacturer> = mutableListOf<Manufacturer>()

    fun toggleFlash() {
        flashOn.set(!flashOn.value)
    }

    init {
        initState()
    }

    private suspend fun load(participantSummary: ParticipantSummaryUiModel) {
        try {
            val manufacturers = configurationManager.getVaccineManufacturers(participantSummary.vaccine.value)
            val config = configurationManager.getConfiguration()
            onManufacturersLoaded(manufacturers)
            onManufacturersDataLoaded(config.manufacturers)
            manufacturerRegexes.set(config.manufacturers)
            loading.set(false)
        } catch (ex: Throwable) {
            yield()
            ex.rethrowIfFatal()
            loading.set(false)
            errorMessage.set(resourcesWrapper.getString(R.string.general_label_error))
            logError("Failed to load visits for participant: ", ex)
        }


    }

    private fun initState() {
        loading.set(true)
        errorMessage.set(null)
        participantArg.filterNotNull().onEach { participant ->
            this.participant.value = participant
        }.combine(retryClickEvents.asFlow()) { participant, _ ->
            loading.set(true)
            errorMessage.set(null)
            load(participant)
        }.launchIn(scope)

        retryClickEvents.tryEmit(Unit)

    }

    fun setArguments(participant: ParticipantSummaryUiModel) {
        this.participantArg.value = participant
    }


    fun onBarcodeScanned(result: String,flag:String,manufacturers: List<Manufacturer>) {
        logVerbose("Got barcode: {}", result)

        val barcode = formatBarcode(result) ?: return
        if (barcode.isNotEmpty()) {
            validateIdJob?.cancel()
            manufacturersList.clear()
            manufacturersList.addAll(manufacturers)
            validateIdJob = scope.launch {
                if(flag.equals(ScanBarcodeActivity.PARTICIPANT)) {
                    validateId(barcode)
                }else{
                    validateIdManufracturer(barcode)
                }
            }
        }
    }


    /**
     * Triggered when manufacturers are loaded from the manager.
     * Will set the manufacturer dropdown list and default if there is only one option.
     *
     * @param manufacturers     List of strings representing the manufacturers possible for this regimen
     */
    private fun onManufacturersLoaded(manufacturers: List<String>) {
        this.manufacturerList.set(manufacturers)

        // If only one manufacturer possible, default to this value.
        if (manufacturers.size == 1) {
           // this.selectedManufacturer.set(manufacturers[0])
        }
    }


    private fun onManufacturersDataLoaded(manufacturers: List<Manufacturer>) {
        this.manufacturersList.addAll(manufacturers)

        // If only one manufacturer possible, default to this value.
        if (manufacturers.size == 1) {
            // this.selectedManufacturer.set(manufacturers[0])
        }
    }

    fun getManufactuerList():kotlin.collections.List<Manufacturer>{
        return manufacturersList
    }



    private suspend fun validateId(
        barcode: String,
    ): Unit {
        resetErrorMessage()
        if (barcode.isNullOrEmpty() || !participantIdValidator.validate(barcode)) {
            scannedBarcode.set(barcode)
            barcodeValid.set(false)
            barcodeErrorMessage.set(resourcesWrapper.getString(R.string.participant_flow_participant_id_scanned))
        } else {
            scannedBarcode.set(barcode)
            barcodeValid.set(true)
        }
    }

    private fun validateIdManufracturer(
        barcode: String,
    ): Unit {
        resetErrorMessage()
        if (barcode.isNullOrEmpty() || !matchBarcodeManufacturer(barcode) ) {
            scannedBarcode.set(barcode)
            barcodeValid.set(false)
            barcodeErrorMessage.set(resourcesWrapper.getString(R.string.participant_flow_participant_id_scanned))
        } else {
            scannedBarcode.set(barcode)
            barcodeValid.set(true)
        }
    }

    /**
     * Check if the barcode matches any of the regex strings for the manufacturers
     * As soon as a match is found, this manufacturer will be selected
     * If no matches are found, a validation message is shown on the manufacturer dropdown
     *
     * @param barcode           Scanned barcode string
     * @param resourcesWrapper  ResourcesWrapper to resolve resource string for validation message
     */
    fun matchBarcodeManufacturer(barcode: CharSequence) :Boolean{

        var isValidate=false
        for( manufract in manufacturersList){
            if (manufract.barcodeRegex.toRegex().containsMatchIn(barcode)) {
                //setSelectedManufacturer(it.name)
                isValidate=true
                break
            }else{
                isValidate=false
            }
        }

        return isValidate

        /*if (selectedManufacturer.get().isNullOrEmpty()) {
            manufacturerValidationMessage.set(resourcesWrapper.getString(R.string.visit_dosing_error_no_manufacturer))
        }*/
    }

    private fun resetErrorMessage() {
        barcodeErrorMessage.set(null)
    }

}
