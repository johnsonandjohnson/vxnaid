package com.jnj.vaccinetracker.common.di

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.IntegerRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat

/**
 * @author maartenvangiel
 * @version 1
 */
interface ResourcesWrapper {

    fun getString(@StringRes resId: Int): String

    fun getString(@StringRes resId: Int, vararg arguments: Any): String

    fun getInt(@IntegerRes resId: Int): Int

    fun getColor(@ColorRes resId: Int): Int
}

class AppResources constructor(private val context: Context) : ResourcesWrapper {
    override fun getString(resId: Int): String {
        return context.getString(resId)
    }

    override fun getString(resId: Int, vararg arguments: Any): String {
        return context.getString(resId, *arguments)
    }

    override fun getInt(resId: Int): Int {
        return context.resources.getInteger(resId)
    }

    override fun getColor(resId: Int): Int {
        return ContextCompat.getColor(context, resId)
    }
}

