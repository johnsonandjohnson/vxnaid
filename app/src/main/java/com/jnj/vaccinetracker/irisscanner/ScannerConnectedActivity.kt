package com.jnj.vaccinetracker.irisscanner

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import androidx.activity.viewModels
import com.jnj.vaccinetracker.common.ui.BaseActivity

class ScannerConnectedActivity : BaseActivity() {

    private val viewModel: ScannerConnectedViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
        if (device != null) {
            viewModel.markScannerAsConnected()
        }
        finish()
    }

}