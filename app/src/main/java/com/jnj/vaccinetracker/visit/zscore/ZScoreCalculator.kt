package com.jnj.vaccinetracker.visit.zscore

import com.jnj.vaccinetracker.common.domain.entities.Gender

data class ZScoreAndRating(val zScore: Double, val rating: String) {
    override fun toString(): String {
        return "$rating (${String.format("%.2f", zScore)}SD)"
    }
}

abstract class ZScoreCalculator (
        private val gender: Gender,
        private val birtDayText: String
){

    abstract fun calculateZScoreAndRating(): ZScoreAndRating?
    abstract fun calculateZScore(): Double?
}