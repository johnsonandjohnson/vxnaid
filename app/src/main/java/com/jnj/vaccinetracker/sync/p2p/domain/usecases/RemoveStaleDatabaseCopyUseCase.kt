package com.jnj.vaccinetracker.sync.p2p.domain.usecases

import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.helpers.days
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.ui.plus
import com.jnj.vaccinetracker.sync.p2p.common.util.DatabaseFolder
import com.jnj.vaccinetracker.sync.p2p.common.util.VmpDatabaseFiles
import javax.inject.Inject

class RemoveStaleDatabaseCopyUseCase @Inject constructor(
    private val vmpDatabaseFiles: VmpDatabaseFiles,
    private val databaseFolder: DatabaseFolder
) {

    companion object {
        private val STALE_THRESHOLD = 7.days
    }

    /**
     * remove database after it's [STALE_THRESHOLD] days old or right away if [deviceNameChanged] is **true**
     */
    fun removeStaleDatabaseCopy(deviceNameChanged: Boolean) {
        logInfo("removeStaleDatabaseCopy: deviceNameChanged=$deviceNameChanged")
        val copyFile = vmpDatabaseFiles.getDbFile().toCopy()
        if (copyFile.exists()) {
            val copyDate = DateEntity(copyFile.file.lastModified())
            val currentDate = dateNow()
            val isStaleDate = copyDate + STALE_THRESHOLD < currentDate
            if (isStaleDate || deviceNameChanged) {
                logInfo("removing stale database")
                copyFile.delete(databaseFolder)
            }
        } else {
            logInfo("database copy not available")
        }
    }
}