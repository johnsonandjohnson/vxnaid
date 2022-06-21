package com.jnj.vaccinetracker.sync.domain.usecases.error

import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.helpers.appVersion
import com.jnj.vaccinetracker.sync.data.models.SyncErrorsDeviceInfo
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import javax.inject.Inject

class BuildSyncErrorDeviceInfoUseCase @Inject constructor(
    private val syncSettingsRepository: SyncSettingsRepository,
    private val userRepository: UserRepository,
    private val buildDeviceHardwareUseCase: BuildDeviceHardwareUseCase,
) {


    fun build(): SyncErrorsDeviceInfo {
        return SyncErrorsDeviceInfo(
            deviceId = userRepository.getDeviceGuid(),
            siteUuid = syncSettingsRepository.getSiteUuid(),
            backendUrl = syncSettingsRepository.getBackendUrl(),
            appVersion = appVersion,
            deviceHardware = buildDeviceHardwareUseCase.build(),
            deviceName = userRepository.getDeviceName()
        )
    }
}