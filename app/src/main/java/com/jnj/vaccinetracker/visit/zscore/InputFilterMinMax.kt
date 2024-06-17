package com.jnj.vaccinetracker.visit.zscore

import android.text.InputFilter
import android.text.Spanned
import android.util.Log

class InputFilterMinMax(private val min: Int, private val max: Int) : InputFilter {

    override fun filter(
            source: CharSequence?,
            start: Int,
            end: Int,
            dest: Spanned?,
            dstart: Int,
            dend: Int
    ): CharSequence? {
        try {
            val input = (dest.toString() + source.toString()).toInt()
            if (isInRange(min, max, input)) {
                return null
            }
        } catch (nfe: NumberFormatException) {
            Log.e("InputFilterMinMax", "Number format exception: ${nfe.message}")
        }
        return ""
    }

    private fun isInRange(a: Int, b: Int, c: Int): Boolean {
        return if (b > a) c in a..b else c in b..a
    }
}
