package com.jnj.vaccinetracker.barcode

import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.domain.entities.Address
import com.jnj.vaccinetracker.common.domain.entities.Gender
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.logVerbose
import com.jnj.vaccinetracker.common.validators.ParticipantIdValidator
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

class ScanBarcodeViewModel @Inject constructor(
    private val participantIdValidator: ParticipantIdValidator,
    override val dispatchers: AppCoroutineDispatchers,
    private val resourcesWrapper: ResourcesWrapper,
) : ViewModelBase() {

    val scannedBarcode = mutableLiveData<String>()
    val flashOn = mutableLiveBoolean()
    val barcodeValid = mutableLiveBoolean()
    val barcodeErrorMessage = mutableLiveData<String>()
    private var validateIdJob: Job? = null


    fun toggleFlash() {
        flashOn.set(!flashOn.value)
    }


    fun onBarcodeScanned(result: String) {
        logVerbose("Got barcode: {}", result)

        val barcode = formatBarcode(result) ?: return
        if (barcode.isNotEmpty()) {
            validateIdJob?.cancel()
            validateIdJob = scope.launch {
                validateId(barcode)
            }
        }
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

    private fun resetErrorMessage() {
        barcodeErrorMessage.set(null)
    }

}
