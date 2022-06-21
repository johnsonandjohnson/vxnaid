package com.jnj.vaccinetracker.setup.screens.p2p.transfer.client

import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.setup.screens.p2p.transfer.base.SetupP2pDeviceTransferViewModelBase
import com.jnj.vaccinetracker.sync.p2p.domain.entities.ClientProgress
import com.jnj.vaccinetracker.sync.p2p.domain.services.P2pClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

class SetupP2pDeviceClientTransferViewModel @Inject constructor(
    dispatchers: AppCoroutineDispatchers,
    private val client: P2pClient, override val userRepository: UserRepository, override val resourcesWrapper: ResourcesWrapper,
) : SetupP2pDeviceTransferViewModelBase(dispatchers) {

    val progress: StateFlow<ClientProgress> get() = client.clientProgress

    val serviceDevice
        get() = client.serviceDevice

    val isDownloading = MutableStateFlow(false)


    init {
        initState()
    }

    override fun initState() {
        super.initState()
        client.errorMessage
            .onEach { errorMessage.value = it }
            .launchIn(scope)
        client.startNsd()
    }

    fun startDownload() {
        if (isDownloading.value) {
            logInfo("already started download")
            return
        }

        scope.launch {
            if (serviceDevice.value == null) {
                logInfo("error cannot start download because not connected")
                return@launch
            }
            isDownloading.value = true
            try {
                client.importDataFromServer()
            } catch (ex: Exception) {
                logError("error occurred during importDataFromServer", ex)
            } finally {
                isDownloading.value = false
            }
        }

    }

    fun reconnect() {
        client.reconnect()
    }

    override fun onCleared() {
        super.onCleared()
        client.dispose()
    }
}