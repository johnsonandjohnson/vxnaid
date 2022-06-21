package com.jnj.vaccinetracker.setup.screens.p2p.transfer.base

import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

abstract class SetupP2pDeviceTransferViewModelBase(override val dispatchers: AppCoroutineDispatchers) : ViewModelBase() {

    protected abstract val userRepository: UserRepository
    protected abstract val resourcesWrapper: ResourcesWrapper
    val deviceName = mutableLiveData<String?>()
    val errorMessage = mutableLiveData<String?>()

    protected open fun initState() {
        userRepository.observeDeviceName().onEach {
            deviceName.value = it
        }.launchIn(scope)
    }
}