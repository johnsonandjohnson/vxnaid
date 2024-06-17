package com.jnj.vaccinetracker.visit.zscore

import android.graphics.Color
import com.jnj.vaccinetracker.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import com.jnj.vaccinetracker.common.domain.entities.Gender

class MuacZScoreCalculator(
        private val muac: Int?, gender: Gender, birtDayText: String,
) : ZScoreCalculator(gender, birtDayText) {
   companion object {
      const val NORMAL_NUTRITION_STATUS = "Normal Nutrition Status" // green
      const val MODERATE_NUTRITION_STATUS = "Moderate Acute Malnutrition" // yellow
      const val SEVERE_NUTRITION_STATUS = "Severe Acute Malnutrition" // red

      fun shouldCalculateMuacZScore(birthDayString: String): Boolean {
         val currentDate = Calendar.getInstance().time

         val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
         val parsedBirthDate = dateFormat.parse(birthDayString)

         val diffMonths = parsedBirthDate?.let { calculateMonthDifference(it, currentDate) }

         if (diffMonths != null) {
            return diffMonths >= 6
         }
         return false
      }

      private fun calculateMonthDifference(startDate: Date, endDate: Date): Int {
         val startCalendar = Calendar.getInstance().apply { time = startDate }
         val endCalendar = Calendar.getInstance().apply { time = endDate }

         var diffYears = endCalendar.get(Calendar.YEAR) - startCalendar.get(Calendar.YEAR)
         if (endCalendar.get(Calendar.MONTH) < startCalendar.get(Calendar.MONTH) ||
                 (endCalendar.get(Calendar.MONTH) == startCalendar.get(Calendar.MONTH) &&
                         endCalendar.get(Calendar.DAY_OF_MONTH) < startCalendar.get(Calendar.DAY_OF_MONTH))
         ) {
            diffYears--
         }
         return diffYears * 12 + endCalendar.get(Calendar.MONTH) - startCalendar.get(Calendar.MONTH)
      }
   }

   override fun calculateZScoreAndRating(): ZScoreAndRating? {
      val zScore = this.zScore ?: return null
      // exact values were not provided
      val rating = when {
         zScore < -1 -> SEVERE_NUTRITION_STATUS
         zScore in -1.0..2.0 -> MODERATE_NUTRITION_STATUS
         else -> NORMAL_NUTRITION_STATUS
      }
      return ZScoreAndRating(zScore, rating)
   }

   override fun calculateZScore(): Double? {
      // Placeholder calculation (random value for demonstration)
      // Perform calculations based on weight, gender, and age to calculate Z-score
      // Replace the placeholder calculation with actual logic using reference data or models
      if (muac == null) return null
      if (!shouldCalculateMuacZScore(birtDayText)) return null
      return (Math.random() * 8) - 4 // Example Z-score between -4 and +4
   }

   fun getTextColorBasedOnZsCoreValue(): Int {
      val defaultColor = R.color.colorTextOnLight
      val zScore = this.zScore ?: return defaultColor
      val color = when {
         zScore <= -1 -> Color.RED
         zScore < 2.0 -> Color.parseColor("#FFAA00") //dark yellow
         else -> Color.GREEN
      }
      return color
   }
}
