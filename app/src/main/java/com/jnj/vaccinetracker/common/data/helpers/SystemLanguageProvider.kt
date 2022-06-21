package com.jnj.vaccinetracker.common.data.helpers

import android.content.Context
import android.os.Build
import javax.inject.Inject


class SystemLanguageProvider @Inject constructor(private val context: Context) {
    fun getSystemLanguage(): String = context.resources.configuration.run {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locales[0].language
        } else {
            @Suppress("DEPRECATION")
            locale.language
        }
    }
}