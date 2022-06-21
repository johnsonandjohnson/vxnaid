package com.jnj.vaccinetracker.sync.p2p.data.helpers

import com.jnj.vaccinetracker.sync.p2p.domain.entities.ClientProgress
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientProgressProvider @Inject constructor() {
    val clientProgress = MutableStateFlow<ClientProgress>(ClientProgress.Idle)
}