package com.jnj.vaccinetracker.setup.screens.p2p.transfer.helpers

import com.jnj.vaccinetracker.common.helpers.seconds
import com.jnj.vaccinetracker.sync.p2p.domain.entities.P2pProgress
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

private val DELAY = 1.seconds

fun <P : P2pProgress> Flow<P>.throttleProgress(): Flow<P> {
    // if the type of progress has changed then wait a moment before we proceed so the user can see each progress type
    var lastP: P? = null
    return onEach { p ->
        if (p::class != lastP?.let { it::class }) {
            if (lastP?.isIdle() == false)
                delay(DELAY)
            lastP = p
        }
    }
}