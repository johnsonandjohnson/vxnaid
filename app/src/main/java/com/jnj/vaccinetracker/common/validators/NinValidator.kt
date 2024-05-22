package com.jnj.vaccinetracker.common.validators

import com.jnj.vaccinetracker.common.domain.usecases.masterdata.GetConfigurationUseCase
import com.jnj.vaccinetracker.common.helpers.logWarn
import javax.inject.Inject

class NinValidator @Inject constructor(
    private val getConfigurationUseCase: GetConfigurationUseCase,
    private val ninUtil: NinUtil
) {

    companion object {
        /**
         * NIN format: "CM" followed by 12 alphanumeric characters.
         */
        private val ninRegex = "^CM[0-9A-Za-z]{12}\$".toRegex()
    }

    fun validate(nin: String): Boolean {
        // Check if NIN matches the specific format
        if (!ninRegex.matches(nin)) {
            logWarn("$nin does not match the required format: $ninRegex")
            return false
        }

        // Perform additional validation if necessary
        return try {
            true
        } catch (e: Exception) {
            logWarn("NIN is invalid: ${e.message}")
            false
        }
    }
}

class NinUtil @Inject constructor() {
    fun isValidNumber(nin: String): Boolean {
        return !nin.last().isDigit()
    }
}
