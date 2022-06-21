package com.jnj.vaccinetracker.irisscanner

import androidx.lifecycle.ViewModel
import com.jnj.vaccinetracker.common.data.managers.IrisScannerConnectionManager
import javax.inject.Inject

class ScannerConnectedViewModel @Inject constructor(private val irisScannerConnectionManager: IrisScannerConnectionManager) :
    ViewModel() {

    fun markScannerAsConnected() {
        irisScannerConnectionManager.onDeviceConnected()
    }

}