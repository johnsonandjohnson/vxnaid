package com.jnj.vaccinetracker.sync.p2p.common.models

data class DbFileSnapshot(
    val dbFile: DbFile,
    val dateModified: Long,
    val hash: String,
) {
    fun isOutdated() = dateModified != dbFile.file.lastModified()
}