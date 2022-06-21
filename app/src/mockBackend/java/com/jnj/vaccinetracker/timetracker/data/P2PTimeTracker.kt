package com.jnj.vaccinetracker.timetracker.data

import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.timetracker.data.timetrackers.base.TimeTrackerBase
import com.tfcporciuncula.flow.FlowSharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class P2PTimeTracker @Inject constructor(prefs: FlowSharedPreferences, dispatchers: AppCoroutineDispatchers) : TimeTrackerBase(dispatchers) {

    override val timePref = prefs.getLong("P2P_TIME")
}