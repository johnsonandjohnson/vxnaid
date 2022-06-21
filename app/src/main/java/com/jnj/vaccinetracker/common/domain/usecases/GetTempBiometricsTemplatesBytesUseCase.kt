package com.jnj.vaccinetracker.common.domain.usecases

import com.jnj.vaccinetracker.common.data.helpers.AndroidFiles
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.models.IrisPosition
import com.jnj.vaccinetracker.common.domain.entities.BiometricsTemplateBytes
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.readBytesAsync
import java.io.File
import javax.inject.Inject

class GetTempBiometricsTemplatesBytesUseCase @Inject constructor(
    private val androidFiles: AndroidFiles,
    private val dispatchers: AppCoroutineDispatchers,
) {

    /**
     * Generates the iris template portion for the participant matching API call or the registration call.
     *
     * @param irisScans Map of IrisPositions enums to boolean, set to true if the iris was scanned and submitted for that position.
     * @return template bytes
     */
    suspend fun getBiometricsTemplate(irisScans: Map<IrisPosition, Boolean>): BiometricsTemplateBytes? {

        // Check if any irises were scanned
        // = if in any booleans in the irisScans map values are set to true
        if (irisScans.isNullOrEmpty() || !irisScans.values.any { it }) return null
        // Check if the NETemplate exists
        val templateFile = File(androidFiles.cacheDir, Constants.IRIS_TEMPLATE_NAME)
        if (!templateFile.exists()) return null

        return BiometricsTemplateBytes(templateFile.readBytesAsync(dispatchers.io))
    }
}