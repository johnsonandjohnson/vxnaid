package com.jnj.vaccinetracker.common.data.helpers.license

import com.jnj.vaccinetracker.common.domain.entities.ActivatedLicense
import com.jnj.vaccinetracker.common.helpers.logInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
/**
 * must be called each time NLicense.obtain function returns **true**
 */
class ObtainedLicenseLogger @Inject constructor() {

    private val licenseLogs = linkedSetOf<ActivatedLicense>()

    fun logObtainedLicense(activatedLicense: ActivatedLicense) {
        logInfo("logObtainedLicense: ${activatedLicense.licenseType}")
        licenseLogs.add(activatedLicense)
    }

    /**
     * The license cannot be obtained if another license was already obtained since startup of the app.
     * @return true NLicense.obtain function can return true for [activatedLicense] in current session, otherwise false
     */
    fun isObtainable(activatedLicense: ActivatedLicense): Boolean {
        return if (licenseLogs.contains(activatedLicense)) {
            true
        } else {
            val loggedLicense = licenseLogs.find { it.licenseType == activatedLicense.licenseType }
            loggedLicense == null
        }
    }
}