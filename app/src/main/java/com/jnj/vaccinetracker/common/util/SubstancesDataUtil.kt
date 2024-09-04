package com.jnj.vaccinetracker.common.util

import android.os.Build
import androidx.annotation.RequiresApi
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.domain.entities.Substance
import com.jnj.vaccinetracker.common.domain.entities.SubstancesConfig
import com.jnj.vaccinetracker.common.domain.entities.SubstancesGroupConfig
import com.jnj.vaccinetracker.common.domain.entities.VisitDetail
import com.jnj.vaccinetracker.visit.model.OtherSubstanceDataModel
import com.jnj.vaccinetracker.visit.model.SubstanceDataModel
import com.soywiz.klock.DateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

class SubstancesDataUtil {

    companion object {

        @RequiresApi(Build.VERSION_CODES.O)
        suspend fun getSubstancesDataForCurrentVisit(
            participantBirthDate: String,
            participantVisits: List<VisitDetail>,
            configurationManager: ConfigurationManager
        ): List<SubstanceDataModel> {
            val substancesGroupConfig = configurationManager.getSubstancesGroupConfig()
            val childAgeInWeeks = getWeeksBetweenDateAndToday(participantBirthDate)
            val substancesConfig = configurationManager.getSubstancesConfig()
            val substanceDataModelList = mutableListOf<SubstanceDataModel>()
            substancesConfig.forEach { substance ->
                val minWeekNumber = substance.weeksAfterBirth - substance.weeksAfterBirthLowWindow
                val maxWeekNumber = substance.weeksAfterBirth + substance.weeksAfterBirthUpWindow
                if (childAgeInWeeks in minWeekNumber..maxWeekNumber &&
                    !isSubstanceAlreadyApplied(participantVisits, substance.conceptName)
                ) {
                    substanceDataModelList.add(
                        getSingleSubstanceData(
                            substance,
                            substancesGroupConfig,
                            participantVisits,
                            substancesConfig
                        )
                    )
                }
            }

            val resultListWithoutDuplicates = substanceDataModelList.distinctBy { it.conceptName }
            val filteredResultList = applyVaccinesCatchUpSchedule(
                resultListWithoutDuplicates,
                childAgeInWeeks,
                participantVisits,
                substancesGroupConfig
            ).toMutableList()

            return filteredResultList.filter { it.conceptName != "" }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun getVisitTypeForCurrentVisit(
            participantBirthDate: String,
        ): String {
            val childAgeInWeeks = getWeeksBetweenDateAndToday(participantBirthDate)
            val visitType = getVisitTypeFromChildAgeInWeeks(childAgeInWeeks)
            return visitType
        }

        @RequiresApi(Build.VERSION_CODES.O)
        suspend fun getSubstancesDataForVisitType(
            visitType: String,
            configurationManager: ConfigurationManager
        ): List<SubstanceDataModel> {
            val substancesGroupConfig = configurationManager.getSubstancesGroupConfig()
            val visitTypeInWeeks = getWeeksBetweenDateAndTodayFromVisitType(visitType)
            val substancesConfig = configurationManager.getSubstancesConfig()
            val substanceDataModelList = mutableListOf<SubstanceDataModel>()
            substancesConfig.forEach { substance ->
                val minWeekNumber = substance.weeksAfterBirth - substance.weeksAfterBirthLowWindow
                val maxWeekNumber = substance.weeksAfterBirth + substance.weeksAfterBirthUpWindow
                if (visitTypeInWeeks in minWeekNumber..maxWeekNumber) {
                    substanceDataModelList.add(
                        getSingleSubstanceData(
                            substance,
                            substancesGroupConfig,
                            null,
                            substancesConfig
                        )
                    )
                }
            }
            return substanceDataModelList.filter { it.conceptName != "" }.distinctBy { it.conceptName }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun isTimeIntervalMaintained(
            previousDoseConceptName: String?,
            participantVisits: List<VisitDetail>,
            minimumWeeksNumberAfterPreviousDose: Int?
        ): Boolean {

            if (previousDoseConceptName == null || minimumWeeksNumberAfterPreviousDose == null) {
                return true
            }

            val vaccineDates = participantVisits.flatMap { visitDetail ->
                visitDetail.observations.filter { (key, _) ->
                    key == previousDoseConceptName + " ${Constants.DATE_STR}"
                }.mapNotNull {
                    DateUtil.convertStringToDate(it.value.value, DateFormat.FORMAT_DATE.toString())
                }
            }

            val mostRecentVaccineDate = vaccineDates.maxOrNull() ?: return false
            val weeksSinceVaccine = ChronoUnit.WEEKS.between(
                mostRecentVaccineDate.toInstant().atZone(ZoneId.of(Constants.UTC_TIME_ZONE_NAME))
                    .toLocalDateTime(),
                LocalDateTime.now()
            )

            return weeksSinceVaccine >= minimumWeeksNumberAfterPreviousDose
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun applyVaccinesCatchUpSchedule(
            substances: List<SubstanceDataModel>,
            childAgeInWeeks: Int,
            participantVisits: List<VisitDetail>,
            substancesGroupConfig: SubstancesGroupConfig
        ): List<SubstanceDataModel> {
            return substances.filter { substance ->
                val previousDoseConceptName = findPreviousDoseName(substance, substancesGroupConfig)
                (substance.maximumAgeInWeeks == null || childAgeInWeeks <= substance.maximumAgeInWeeks) &&
                        isTimeIntervalMaintained(
                            previousDoseConceptName,
                            participantVisits,
                            substance.minimumWeeksNumberAfterPreviousDose
                        )
            }
        }

        suspend fun getAllSubstances(
            configurationManager: ConfigurationManager
        ): List<SubstanceDataModel> {
            val substancesConfig = configurationManager.getSubstancesConfig()
            val substanceDataModelList = mutableListOf<SubstanceDataModel>()
            substancesConfig.forEach { substance ->
                substanceDataModelList.add(
                    SubstanceDataModel(
                        substance.conceptName,
                        substance.label,
                        substance.category,
                        substance.routeOfAdministration,
                        substance.group,
                        substance.maximumAgeInWeeks,
                        substance.minimumWeeksNumberAfterPreviousDose
                    )
                )
            }

            return substanceDataModelList
        }

        @RequiresApi(Build.VERSION_CODES.O)
        suspend fun getOtherSubstancesDataForCurrentVisit(
            participantBirthDate: String,
            configurationManager: ConfigurationManager
        ): List<OtherSubstanceDataModel> {
            val otherSubstancesConfig = configurationManager.getOtherSubstancesConfig()
            val childAgeInWeeks = getWeeksBetweenDateAndToday(participantBirthDate)
            val otherSubstancesDataModelList = mutableListOf<OtherSubstanceDataModel>()
            otherSubstancesConfig.forEach { otherSubstance ->
                val minWeekNumber =
                    otherSubstance.weeksAfterBirth - otherSubstance.weeksAfterBirthLowWindow
                val maxWeekNumber =
                    otherSubstance.weeksAfterBirth + otherSubstance.weeksAfterBirthUpWindow
                if (childAgeInWeeks in minWeekNumber..maxWeekNumber) {
                    otherSubstancesDataModelList.add(
                        OtherSubstanceDataModel(
                            otherSubstance.conceptName,
                            otherSubstance.label,
                            otherSubstance.category,
                            otherSubstance.inputType,
                            otherSubstance.options
                        )
                    )
                }
            }

            return otherSubstancesDataModelList
        }

        @RequiresApi(Build.VERSION_CODES.O)
        suspend fun getOtherSubstancesDataForVisitType(
            visitType: String,
            configurationManager: ConfigurationManager
        ): List<OtherSubstanceDataModel> {
            val otherSubstancesConfig = configurationManager.getOtherSubstancesConfig()
            val visitTypeInWeeks = getWeeksBetweenDateAndTodayFromVisitType(visitType)
            val otherSubstancesDataModelList = mutableListOf<OtherSubstanceDataModel>()
            otherSubstancesConfig.forEach { otherSubstance ->
                val minWeekNumber =
                    otherSubstance.weeksAfterBirth - otherSubstance.weeksAfterBirthLowWindow
                val maxWeekNumber =
                    otherSubstance.weeksAfterBirth + otherSubstance.weeksAfterBirthUpWindow
                if (visitTypeInWeeks in minWeekNumber..maxWeekNumber) {
                    otherSubstancesDataModelList.add(
                        OtherSubstanceDataModel(
                            otherSubstance.conceptName,
                            otherSubstance.label,
                            otherSubstance.category,
                            otherSubstance.inputType,
                            otherSubstance.options
                        )
                    )
                }
            }

            return otherSubstancesDataModelList
        }

        private fun getSingleSubstanceData(
            substance: Substance,
            substancesGroupConfig: SubstancesGroupConfig,
            participantVisits: List<VisitDetail>?,
            substancesConfig: SubstancesConfig
        ): SubstanceDataModel {
            val group =
                substancesGroupConfig.find { substanceGroup -> substanceGroup.substanceName == substance.group }
            val earlierDoses =
                group?.options?.takeWhile { it != substance.conceptName }?.toMutableList()
                    ?: mutableListOf()
            if (group?.options?.contains(substance.conceptName) == true) {
                earlierDoses.add(substance.conceptName)
            }
            var substanceToBeAdministered = substance.conceptName
            for (item in earlierDoses) {
                if (participantVisits != null && isSubstanceAlreadyApplied(
                        participantVisits,
                        item
                    )
                ) {
                    substanceToBeAdministered = ""
                } else {
                    substanceToBeAdministered = item
                    break
                }
            }

            val substanceToBeAdministeredObject =
                substancesConfig.find { it.conceptName == substanceToBeAdministered }

            return SubstanceDataModel(
                substanceToBeAdministeredObject?.conceptName ?: "",
                substanceToBeAdministeredObject?.label ?: "",
                substanceToBeAdministeredObject?.category ?: "",
                substanceToBeAdministeredObject?.routeOfAdministration ?: "",
                substanceToBeAdministeredObject?.group ?: "",
                substanceToBeAdministeredObject?.maximumAgeInWeeks,
                substanceToBeAdministeredObject?.minimumWeeksNumberAfterPreviousDose
            )
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun getWeeksBetweenDateAndToday(dateString: String): Int {
            val formatter = DateTimeFormatter.ofPattern(DateFormat.FORMAT_DATE.toString())
            val startDate = LocalDate.parse(dateString, formatter)
            val endDate = LocalDate.now()
            val daysBetween = ChronoUnit.DAYS.between(startDate, endDate).toDouble()

            return ceil(daysBetween / 7).toInt()
        }

        private fun getWeeksBetweenDateAndTodayFromVisitType(visitType: String): Int {
            return when (visitType) {
                Constants.VISIT_TYPE_AT_BIRTH -> 1
                Constants.VISIT_TYPE_SIX_WEEKS -> 6
                Constants.VISIT_TYPE_TEN_WEEKS -> 10
                Constants.VISIT_TYPE_FOURTEEN_WEEKS -> 14
                Constants.VISIT_TYPE_NINE_MONTHS -> 36
                Constants.VISIT_TYPE_EIGHTEEN_MONTHS -> 72
                Constants.VISIT_TYPE_TWO_YEARS -> 96
                else -> 1
            }
        }

        private fun getVisitTypeFromChildAgeInWeeks(ageInWeeks: Int): String {
            return when {
                ageInWeeks in 0..4 -> Constants.VISIT_TYPE_AT_BIRTH       // 0-4 weeks: Birth visit
                ageInWeeks in 5..8 -> Constants.VISIT_TYPE_SIX_WEEKS      // 5-8 weeks: Six weeks visit
                ageInWeeks in 9..12 -> Constants.VISIT_TYPE_TEN_WEEKS     // 9-12 weeks: Ten weeks visit
                ageInWeeks in 13..34 -> Constants.VISIT_TYPE_FOURTEEN_WEEKS // 13-34 weeks: Fourteen weeks visit
                ageInWeeks in 35..70 -> Constants.VISIT_TYPE_NINE_MONTHS  // 35-70 weeks: Nine months visit
                ageInWeeks in 71..94 -> Constants.VISIT_TYPE_EIGHTEEN_MONTHS // 71-94 weeks: Eighteen months visit
                ageInWeeks >= 95 -> Constants.VISIT_TYPE_TWO_YEARS        // 95+ weeks: Two years visit
                else -> Constants.VISIT_TYPE_AT_BIRTH                    // Default to Birth visit
            }
        }

        private fun isSubstanceAlreadyApplied(
            visits: List<VisitDetail>,
            substanceName: String
        ): Boolean {
            return visits.any { visit -> substanceName + " ${Constants.DATE_STR}" in visit.observations }
        }

        private fun findPreviousDoseName(
            substance: SubstanceDataModel,
            substancesGroupConfig: SubstancesGroupConfig
        ): String? {
            val group = substancesGroupConfig.find { it.substanceName == substance.group }
            val index = group?.options?.indexOf(substance.conceptName)

            val previousVaccineConceptName = if (index!! > 0) {
                group.options[index - 1]
            } else {
                null
            }

            return previousVaccineConceptName
        }
    }
}