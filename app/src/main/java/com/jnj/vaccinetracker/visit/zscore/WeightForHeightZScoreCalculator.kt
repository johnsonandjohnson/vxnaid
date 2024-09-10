package com.jnj.vaccinetracker.visit.zscore

import android.graphics.Color
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.domain.entities.Gender
import kotlin.random.Random

class WeightForHeightZScoreCalculator(
        private val weight: String?,
        private val height: String?,
        private val isOedema: String?,
        gender: Gender,
        birthDayText: String
): ZScoreCalculator(gender, birthDayText) {
   companion object {
      const val NORMAL = "Normal Nutrition Status" // Green
      const val MODERATE = "Moderate Acute Malnutrition" // Yellow
      const val SEVERE_WITH_OEDEMA = "Severe Acute Malnutrition with Oedema" // Red
      const val SEVERE_WITHOUT_OEDEMA = "Severe Acute Malnutrition without Oedema" // Red
   }
   override fun calculateZScoreAndRating(): ZScoreAndRating? {
      val zScore = this.zScore ?: return null
      val rating = when {
         (zScore <= -3) && isOedema.toBoolean() -> SEVERE_WITH_OEDEMA
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
      if (height.isNullOrEmpty() || weight.isNullOrEmpty()) return null
      if (weight.toDouble() < 30) return -3.0
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