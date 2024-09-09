package com.jnj.vaccinetracker.visit.zscore

import android.view.View
import com.jnj.vaccinetracker.common.domain.entities.BirthDate
import com.jnj.vaccinetracker.common.domain.entities.Gender

object HardcodedWeightForAgeZScore: HardcodedOtherSubstanceConfig() {
   override fun getInput(): String {
      TODO("Not yet implemented")
   }

   override fun getValue(input: String, gender: Gender?, birthDateText: String?): String {
      return WeightZScoreCalculator(
         weight = input, gender=gender!!, birthDayText = birthDateText!!
      ).calculateZScore().toString()
   }

   override fun setupView(view: View) {
      TODO("Not yet implemented")
   }
}