package com.jnj.vaccinetracker.common.domain.entities

typealias OnProgressPercentChanged = (progressPercent: Int) -> Unit

/**
 * @return percent (0..100)
 */
fun combineProgressPercent(vararg progressPercentArray: Progress): Int {
    val maxWeight = progressPercentArray.map { it.weight }.sum()
    return progressPercentArray
        .map { progress ->
            val weightPercent = progress.weight.toFloat() / maxWeight.toFloat()
            (progress.progressPercent.percent * weightPercent).toInt()
        }.sum()
}

data class Progress(val progressPercent: Int, val weight: Int) {
    init {
        require(weight > 0) { "weight must be greater than zero" }
    }

    override fun toString(): String {
        return "$progressPercent:$weight"
    }
}

fun Int.progress(weight: Int = 1) = Progress(this, weight)

val Int.percent get() = coerceAtMost(100).coerceAtLeast(0)