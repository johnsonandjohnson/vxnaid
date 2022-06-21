package com.jnj.vaccinetracker.common.domain.usecases


import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.sync.data.models.DeviceNameRequest
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.p2p.data.datasources.DatabaseImportedDateDataSource
import com.jnj.vaccinetracker.sync.p2p.domain.usecases.RemoveStaleDatabaseCopyUseCase
import javax.inject.Inject


class ChangeDeviceNameUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    private val userRepository: UserRepository,
    private val removeStaleDatabaseCopyUseCase: RemoveStaleDatabaseCopyUseCase,
    private val databaseImportedDateDataSource: DatabaseImportedDateDataSource,
) {

    suspend fun changeDeviceName(siteUuid: String) {
        logInfo("ChangeDeviceNameUseCase called with site Uuid $siteUuid")
        if (siteUuid != userRepository.getDeviceNameSiteUuid()) {
            logInfo("Clearing device name")
            userRepository.clearDeviceName()
            val name = api.getDeviceName(DeviceNameRequest(siteUuid)).deviceName
            logInfo("Retrieved new device name: $name")
            userRepository.setNewDeviceName(name, siteUuid)
            onDeviceNameChanged()
        }
    }

    private fun onDeviceNameChanged() {
        // remove stale database copy
        removeStaleDatabaseCopyUseCase.removeStaleDatabaseCopy(deviceNameChanged = true)
        // Imported database becomes invalid
        databaseImportedDateDataSource.clearDatabaseImportedDate()
    }
}