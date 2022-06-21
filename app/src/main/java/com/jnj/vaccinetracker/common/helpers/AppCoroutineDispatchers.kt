package com.jnj.vaccinetracker.common.helpers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

data class AppCoroutineDispatchers(
    val io: CoroutineDispatcher,
    val computation: CoroutineDispatcher,
    val main: CoroutineDispatcher,
    val mainImmediate: CoroutineDispatcher,
) {
    companion object {
        val DEFAULT = AppCoroutineDispatchers(io = Dispatchers.IO, computation = Dispatchers.Default, main = Dispatchers.Main,
            mainImmediate = Dispatchers.Main.immediate)

        fun fromSingleDispatcher(dispatcher: CoroutineDispatcher) =
            AppCoroutineDispatchers(io = dispatcher, computation = dispatcher, main = dispatcher, mainImmediate = dispatcher)
    }
}