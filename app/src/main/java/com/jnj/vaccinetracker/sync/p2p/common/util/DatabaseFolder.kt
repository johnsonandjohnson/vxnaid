package com.jnj.vaccinetracker.sync.p2p.common.util

import android.content.Context
import com.jnj.vaccinetracker.common.helpers.logInfo
import java.io.File
import javax.inject.Inject

class DatabaseFolder @Inject constructor(private val context: Context) {

    fun getFile(fileName: String): File = context.getDatabasePath(fileName)
    fun deleteDatabase(fileName: String): Boolean {
        val success = context.deleteDatabase(fileName)
        logInfo("context.deleteDatabase $fileName -> $success")
        return !getFile(fileName).exists()
    }
}