package com.jnj.vaccinetracker.timetracker.data.timetrackers.base

import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.tfcporciuncula.flow.Preference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

abstract class TimeTrackerBase(private val dispatchers: AppCoroutineDispatchers) {
    private val job = SupervisorJob()

    private val runningState = MutableStateFlow<Boolean>(false)

    private val scope
        get() = CoroutineScope(dispatchers.mainImmediate + job)

    protected abstract val timePref: Preference<Long>

    private fun DateEntity.elapsed() = dateNow().time - this.time

    var timeToBeSaved: DateEntity? = null

    init {
        initState()
    }

    private fun saveTimeQueued() {
        timeToBeSaved?.let {
            scope.launch(dispatchers.io) {
                saveElapsed(it.elapsed())
            }
            timeToBeSaved = null
        }
    }

    private fun saveElapsed(elapsed: Long) {
        val current = timePref.get()
        timePref.set(current + elapsed)
    }

    fun initState() {
        scope.launch(dispatchers.io) {
            while (true) {
                saveTimeQueued()
                if (runningState.value)
                    timeToBeSaved = dateNow()
                delay(1000)
            }
        }
    }

    @Synchronized
    fun startRecording() {
        if (!runningState.value) {
            saveTimeQueued()
            timeToBeSaved = dateNow()
            runningState.value = true
        }
    }

    @Synchronized
    fun pauseRecording() {
        if (runningState.value) {
            runningState.value = false
            saveTimeQueued()
        }
    }

    fun isRecording() = runningState.value

    fun getRecordedTime() = timePref.get()

    fun clear() = timePref.delete()

    /**
     * in ms
     */
    fun observeRecordedTime(): Flow<Long> = timePref.asFlow()
}