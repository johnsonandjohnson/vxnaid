package com.jnj.vaccinetracker.common.data.models

/**
 * @author druelens
 * @version 1
 */
enum class LicenseType(val type: String, val primaryComponent: String) {
    IRIS_CLIENT("SingleComputerLicense:IrisClient", "Biometrics.IrisExtraction"),
    IRIS_MATCHING("SingleComputerLicense:IrisMatcher", "Biometrics.IrisMatching"),
}