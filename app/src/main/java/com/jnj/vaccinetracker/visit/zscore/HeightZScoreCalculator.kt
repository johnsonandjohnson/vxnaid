package com.jnj.vaccinetracker.visit.zscore

import com.jnj.vaccinetracker.common.domain.entities.Gender

class HeightZScoreCalculator(
        private val height: Int?, gender: Gender, birtDayText: String,
): ZScoreCalculator(gender, birtDayText) {
    companion object {
        const val NORMAL = "Normal"
        const val STUNTING = "Stunting"
    }

    override fun calculateZScoreAndRating(): ZScoreAndRating? {
        val zScore = this.zScore ?: return null
        val rating = when {
            zScore < -2 -> STUNTING
            else -> NORMAL
        }
        return ZScoreAndRating(zScore, rating)
    }

    override fun calculateZScore(): Double? {
        // Placeholder calculation (random value for demonstration)
        // Perform calculations based on height, gender, and age to calculate Z-score
        // Replace the placeholder calculation with actual logic using reference data or models
        if (height == null) return null
        return (Math.random() * 8) - 4 // Example Z-score between -4 and +4
    }
}
