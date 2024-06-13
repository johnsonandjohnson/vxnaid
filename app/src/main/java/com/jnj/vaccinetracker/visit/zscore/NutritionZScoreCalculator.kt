package com.jnj.vaccinetracker.visit.zscore

import android.graphics.Color
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.domain.entities.Gender
import kotlin.random.Random

class NutritionZScoreCalculator(
        private val weight: Int?,
        private val height: Int?,
        private val isOedema: Boolean?,
        gender: Gender,
        birtDayText: String
): ZScoreCalculator(gender, birtDayText) {
   companion object {
      const val NORMAL = "Normal Nutrition Status" // Green
      const val MODERATE = "Moderate Acute Malnutrition" // Yellow
      const val SEVERE_WITH_OEDEMA = "Severe Acute Malnutrition with Oedema" // Red
      const val SEVERE_WITHOUT_OEDEMA = "Severe Acute Malnutrition without Oedema" // Red
   }
   override fun calculateZScoreAndRating(): ZScoreAndRating? {
      val zScore = this.zScore ?: return null
      val rating = when {
         (zScore <= -3) && isOedema == true -> SEVERE_WITH_OEDEMA
         zScore <= -3 -> SEVERE_WITHOUT_OEDEMA
         zScore < -2.0 -> MODERATE
         else -> NORMAL
      }
      return ZScoreAndRating(zScore, rating)
   }

   override fun calculateZScore(): Double? {
      // Placeholder calculation (random value for demonstration)
      // Perform calculations based on height, gender, and age to calculate Z-score
      // Replace the placeholder calculation with actual logic using reference data or models
      if (height == null || weight == null) return null
      if (weight < 30) return -3.0
      return Random.nextDouble(-2.9, 4.0)
   }

   fun isOedemaValue(): Boolean {
      val zScore = this.zScore ?: return false
      if (zScore <= -3) return true
      return false
   }

   fun getTextColorBasedOnZsCoreValue(): Int {
      val defaultColor = R.color.colorTextOnLight
      val zScore = this.zScore ?: return defaultColor
      val color = when {
         zScore <= -3 -> Color.RED
         zScore < -2.0 -> Color.parseColor("#FFAA00") //dark yellow
         else -> Color.GREEN
      }
      return color
   }
}