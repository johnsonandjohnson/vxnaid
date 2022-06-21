package com.jnj.vaccinetracker.common.ui

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

interface UiFlowExt {

    fun <T> Flow<T>.launchIn(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycleScope.launchWhenStarted {
            collect { }
        }
    }
}
