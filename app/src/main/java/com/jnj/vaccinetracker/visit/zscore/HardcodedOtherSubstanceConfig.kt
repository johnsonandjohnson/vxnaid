package com.jnj.vaccinetracker.visit.zscore

import android.view.View
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.domain.entities.Gender

sealed class HardcodedOtherSubstanceConfig {
   companion object {
      fun fromConceptName(name: String): HardcodedOtherSubstanceConfig {
         return when (name) {
            Constants.CONCEPT_NAME_WEIGHT_FOR_AGE_Z_SCORE -> HardcodedWeightForAgeZScore
            else -> error("Unknown concept name!")
         }
      }
   }

   abstract fun getInput(): String
   abstract fun getValue(input: String, gender: Gender?, birthDateText: String?): String
   abstract fun setupView(view: View)
}