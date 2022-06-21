package com.jnj.vaccinetracker.common.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.isFatalException
import com.jnj.vaccinetracker.common.helpers.logInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class ViewModelBase : ViewModel() {

    private val job = SupervisorJob()

    protected abstract val dispatchers: AppCoroutineDispatchers

    protected val scope
        get() = CoroutineScope(dispatchers.mainImmediate + job)

    override fun onCleared() {
        logInfo("onCleared")
        super.onCleared()
        job.cancel()
    }

    protected inline fun <reified T> stateFlow(value: T) = MutableStateFlow(value)
    protected inline fun <reified T> eventFlow() = EventChannel<T>()
    protected inline fun <reified T> mutableLiveData(value: T? = null) = MutableLiveDataUnique(value, dispatchers, scope)
    protected fun mutableLiveInt(value: Int = 0) = MutableLiveDataUniqueNonNull(value, dispatchers, scope)
    protected fun mutableLiveBoolean(value: Boolean = false) = MutableLiveDataUniqueNonNull(value, dispatchers, scope)

}

class EventChannel<E> {
    //WARNING: only one subscriber possible
    private val channel = Channel<E>(Channel.UNLIMITED)

    fun asFlow() = channel.receiveAsFlow()
    fun tryEmit(event: E) {
        channel.offer(event)
    }
}

fun <T> Flow<T>.catchNonFatal(action: suspend FlowCollector<T>.(throwable: Throwable) -> Unit) = catch { throwable ->
    if (throwable.isFatalException()) {
        throw throwable
    }
    action(throwable)
}

class MutableLiveDataValueContainer<T>(
    private val initialValue: T,
    private val nullable: Boolean,
) {
    private class Wrapper<T>(val value: T?) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Wrapper<*>

            if (value !== other.value) return false

            return true
        }

        override fun hashCode(): Int {
            return value?.hashCode() ?: 0
        }
    }

    private val state = MutableStateFlow(Wrapper(if (nullable) initialValue else initialValue!!))

    var value: T?
        get() = state.value.value
        set(value) {
            state.value = if (nullable) Wrapper(value) else Wrapper(value ?: initialValue)
        }

    fun setValue(newValue: T?, dispatchers: AppCoroutineDispatchers, scope: CoroutineScope, superSetValue: (T?) -> Unit) {
        val oldValue = value
        value = newValue
        if (value !== oldValue) {
            scope.launch(dispatchers.mainImmediate) {
                superSetValue(value)
            }
        }
    }
}


class MutableLiveDataUnique<T>(
    initialValue: T? = null,
    private val dispatchers: AppCoroutineDispatchers,
    private val scope: CoroutineScope,
) : MutableLiveData<T?>(initialValue) {
    private val valueContainer = MutableLiveDataValueContainer(initialValue, true)
    override fun setValue(value: T?) = valueContainer.setValue(value, dispatchers, scope) { super.setValue(it) }
    override fun getValue(): T? {
        return valueContainer.value
    }

    fun set(v: T?) {
        this.value = v
    }

    fun get() = value
}

class MutableLiveDataUniqueNonNull<T : Any>(
    initialValue: T,
    private val dispatchers: AppCoroutineDispatchers,
    private val scope: CoroutineScope,
) : MutableLiveData<T>(initialValue) {
    private val valueContainer = MutableLiveDataValueContainer(initialValue, false)
    override fun setValue(value: T?) = valueContainer.setValue(value, dispatchers, scope) { super.setValue(it) }
    override fun getValue(): T {
        return valueContainer.value!!
    }

    fun set(v: T?) {
        setValue(v)
    }

    fun get() = value
}

