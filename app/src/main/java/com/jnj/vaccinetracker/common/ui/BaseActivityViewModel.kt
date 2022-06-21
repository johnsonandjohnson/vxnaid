package com.jnj.vaccinetracker.common.ui

import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.SessionExpiryObserver
import com.jnj.vaccinetracker.common.helpers.SessionRefreshObserver
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.sync.domain.entities.SyncState
import com.jnj.vaccinetracker.sync.domain.helpers.SyncStateObserver
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

class BaseActivityViewModel @Inject constructor(
    private val userRepository: UserRepository,
    override val dispatchers: AppCoroutineDispatchers,
    private val sessionExpiryObserver: SessionExpiryObserver,
    private val sessionRefreshObserver: SessionRefreshObserver,
    private val syncStateObserver: SyncStateObserver,
) : ViewModelBase() {

    val syncState = stateFlow<SyncState>(SyncState.Idle)
    val shouldShowSessionRefreshDialog = stateFlow(false)
    val openLoginScreenEvent = eventFlow<Unit>()
    var loggedOutExplicit: Boolean = false

    init {
        initState()
    }


    private fun initState() {
        sessionExpiryObserver.sessionExpiredEvents.onEach {
            calcShouldShowSessionRefreshDialog(false)
        }.launchIn(scope)

        sessionRefreshObserver.sessionRefreshEvents.onEach { hasSession ->
            calcShouldShowSessionRefreshDialog(hasSession)
        }.launchIn(scope)

        syncStateObserver.observeSyncState().onEach { state ->
            syncState.emit(state)
        }.launchIn(scope)
    }

    private fun calcShouldShowSessionRefreshDialog(hasSession: Boolean) {
        shouldShowSessionRefreshDialog.value = !hasSession && !loggedOutExplicit
    }


    fun logOut() = scope.launch {
        loggedOutExplicit = true
        userRepository.logOut()
        openLoginScreenEvent.tryEmit(Unit)
    }
}
