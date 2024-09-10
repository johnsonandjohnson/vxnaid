package com.jnj.vaccinetracker.visit.zscore

import android.view.View
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.domain.entities.Gender
import com.jnj.vaccinetracker.visit.adapters.OtherSubstanceItemAdapter

sealed class HardcodedZScore(
   protected var conceptName: String,
   protected val gender: Gender,
   protected val birthDateText: String,
) {
   protected var weight: String? = null
   protected var height: String? = null
   protected var muac: String? = null
   protected var isOedema: String? = null

   companion object {
      fun fromConceptName(name: String, gender: Gender, birthDateText: String): HardcodedZScore {
         return when (name) {
            Constants.CONCEPT_NAME_WEIGHT_FOR_AGE_Z_SCORE -> HardcodedWeightForAgeZScore(name, gender, birthDateText)
            Constants.CONCEPT_NAME_HEIGHT_FOR_AGE_Z_SCORE -> HardcodedHeightForAgeZScore(name, gender, birthDateText)
            Constants.CONCEPT_NAME_MUACA_Z_SCORE -> HardcodedMuacZScore(name, gender, birthDateText)
            Constants.CONCEPT_NAME_WEIGHT_FOR_HEIGHT_Z_SCORE -> HardcodedWeightForHeightZScore(name, gender, birthDateText)
            else -> error("Unknown concept name!")
         }
      }
   }

   fun setArguments(items: MutableMap<String, String>?) {
      items?.let {
         if (it.containsKey(Constants.CONCEPT_NAME_WEIGHT_KG)) {
            weight = it[Constants.CONCEPT_NAME_WEIGHT_KG]
         }
         if (it.containsKey(Constants.CONCEPT_NAME_HEIGHT_FOR_AGE_Z_SCORE)) {
            height = it[Constants.CONCEPT_NAME_HEIGHT_FOR_AGE_Z_SCORE]
         }
         if (it.containsKey(Constants.CONCEPT_NAME_MUACA_Z_SCORE)) {
            muac = it[Constants.CONCEPT_NAME_MUACA_Z_SCORE]
         }
         if (it.containsKey(Constants.CONCEPT_NAME_IS_OEDEMA_Z_SCORE)) {
            isOedema = it[Constants.CONCEPT_NAME_IS_OEDEMA_Z_SCORE]
         }
      }
   }

   abstract fun getValue(): String?

   abstract fun isEmpty(): Boolean
   abstract fun onEmpty(): () -> Unit
   abstract fun onNotEmpty(): () -> Unit
   abstract fun setupView(view: View, listener: OtherSubstanceItemAdapter.AddSubstanceValueListener)
}