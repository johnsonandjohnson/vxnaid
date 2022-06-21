package com.jnj.vaccinetracker.sync.p2p.common.models

import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.md5Hash
import com.jnj.vaccinetracker.common.helpers.toTemp
import com.jnj.vaccinetracker.sync.p2p.common.util.DatabaseFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.CoroutineContext

sealed class DbFile {
    abstract val file: File

    val name: String get() = file.name
    fun exists() = file.exists()

    data class Src(
        override val file: File,
    ) : DbFile() {
        fun toCopy() = Copy(File("$file.copy"))
    }

    data class Copy(
        override val file: File,
    ) : DbFile() {
        fun delete(databaseFolder: DatabaseFolder): Boolean = databaseFolder.deleteDatabase(file.name)
    }
}

suspend fun DbFile.createSnapshot(): DbFileSnapshot {
    return DbFileSnapshot(this, file.lastModified(), file.md5Hash())
}

suspend fun DbFile.Src.saveCopy(
    coroutineContext: CoroutineContext = Dispatchers.IO,
): DbFile.Copy {
    val src = this
    val copyTarget = src.toCopy()
    withContext(coroutineContext) {
        logInfo("saveCopy ${src.name} to ${copyTarget.name}")
        val tempTarget = copyTarget.file.toTemp()
        tempTarget.delete()
        src.file.copyTo(tempTarget)
        tempTarget.renameTo(copyTarget.file)
    }
    return copyTarget
}