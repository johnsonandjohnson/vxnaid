package com.jnj.vaccinetracker.sync.domain.usecases.error

import android.os.Build
import com.jnj.vaccinetracker.sync.data.models.DeviceHardware
import javax.inject.Inject

class BuildDeviceHardwareUseCase @Inject constructor() {

    fun build(): DeviceHardware {
        return DeviceHardware(
            androidVersion = Build.VERSION.RELEASE,
            model = Build.MODEL,
            device = Build.DEVICE,
            product = Build.PRODUCT,
        )
    }
}