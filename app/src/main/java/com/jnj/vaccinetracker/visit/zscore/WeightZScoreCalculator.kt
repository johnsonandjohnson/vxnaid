package com.jnj.vaccinetracker.visit.zscore

import com.jnj.vaccinetracker.common.domain.entities.Gender

data class ZScoreAndRating(val zScore: Double, val rating: String) {
    override fun toString(): String {
        return "$rating (${String.format("%.2f", zScore)}SD)"
    }
}

class WeightZScoreCalculator(
        private val weight: Int?,
        private val gender: Gender,
        private val birtDayText: String
) {
    companion object {
        const val SEVERELY_UNDERWEIGHT = "Severely Underweight"
        const val UNDERWEIGHT = "Underweight"
        const val NORMAL = "Normal"
        const val OVERWEIGHT = "Overweight"
        const val OBESE = "Obese"
    }

    fun calculateZScoreAndRating(): ZScoreAndRating? {
        val zScore = calculateZScore() ?: return null
        val rating = when {
            zScore < -3 -> SEVERELY_UNDERWEIGHT
            zScore in -3.0..-2.0 -> UNDERWEIGHT
            zScore in -2.0..2.0 -> NORMAL
            zScore in 2.0..3.0 -> OVERWEIGHT
            else -> OBESE
        }
        return ZScoreAndRating(zScore, rating)
    }

    private fun calculateZScore(): Double? {
        // Placeholder calculation (random value for demonstration)
        // Perform calculations based on weight, gender, and age to calculate Z-score
        // Replace the placeholder calculation with actual logic using reference data or models
        if (weight == null) return null
        return (Math.random() * 6) - 3 // Example Z-score between -3 and +3
    }
}
