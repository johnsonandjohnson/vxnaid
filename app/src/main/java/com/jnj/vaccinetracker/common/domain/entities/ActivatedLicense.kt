package com.jnj.vaccinetracker.common.domain.entities

import com.jnj.vaccinetracker.common.data.models.LicenseType

data class ActivatedLicense(val licenseType: LicenseType, val activatedLicense: String)