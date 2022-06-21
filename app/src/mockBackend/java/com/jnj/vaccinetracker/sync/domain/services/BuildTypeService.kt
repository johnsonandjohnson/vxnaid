package com.jnj.vaccinetracker.sync.domain.services

import com.jnj.vaccinetracker.timetracker.domain.services.TimeTrackerService
import javax.inject.Inject

/**
 * custom logic by build type
 */
class BuildTypeService @Inject constructor(private val timeTrackerService: TimeTrackerService)