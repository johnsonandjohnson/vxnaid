package com.jnj.vaccinetracker.common.domain.usecases

import com.jnj.vaccinetracker.common.data.biometrics.BiometricClientFactory
import com.jnj.vaccinetracker.common.data.helpers.runTasks
import com.jnj.vaccinetracker.common.data.managers.LicenseManager
import com.jnj.vaccinetracker.common.data.models.LicenseType
import com.jnj.vaccinetracker.common.domain.entities.BiometricsFileMatch
import com.jnj.vaccinetracker.common.domain.entities.BiometricsTemplateBytes
import com.jnj.vaccinetracker.common.domain.entities.OnProgressPercentChanged
import com.jnj.vaccinetracker.common.domain.entities.ParticipantBiometricsTemplateFileBase
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.debugLabel
import com.jnj.vaccinetracker.common.helpers.logWarn
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BiometricsMatcherUseCase @Inject constructor(
    private val dispatchers: AppCoroutineDispatchers,
    private val biometricClientFactory: BiometricClientFactory,
    private val licenseManager: LicenseManager,
) {
    companion object {
        private const val BATCH_SIZE = 2500
    }

    private suspend fun matchBatch(templatesToEnroll: List<ParticipantBiometricsTemplateFileBase>, biometricsTemplate: BiometricsTemplateBytes): List<BiometricsFileMatch> =
        withContext(dispatchers.computation) {
            biometricClientFactory.create().use { client ->
                templatesToEnroll.forEach { t -> client.enrollSilently(t) }
                val results = client.match(biometricsTemplate)
                val templateMap = templatesToEnroll.associateBy { it.participantUuid }
                results.map {
                    val template = requireNotNull(templateMap[it.id]) { "Biometric match has unknown id" }
                    BiometricsFileMatch(template, it.matchingScore)
                }
            }
        }

    suspend fun match(
        templatesToEnroll: List<ParticipantBiometricsTemplateFileBase>,
        biometricsTemplate: BiometricsTemplateBytes,
        progressPercentChanged: OnProgressPercentChanged = {},
    ): List<BiometricsFileMatch> {
        if (templatesToEnroll.isEmpty()) {
            logWarn("match: templatesToEnroll is empty")
            return emptyList()
        }
        licenseManager.getLicensesOrThrow(licenseTypes = listOf(LicenseType.IRIS_CLIENT, LicenseType.IRIS_MATCHING))
        val dataSet = templatesToEnroll.chunked(BATCH_SIZE)
        return runTasks(dataSet, debugLabel = debugLabel(), coroutineContext = dispatchers.computation, progressUpdate = { _, _, _, progress, max ->
            progressPercentChanged(progress / max)
        }) { templates ->
            matchBatch(templates, biometricsTemplate)
        }.flatten()
    }
}