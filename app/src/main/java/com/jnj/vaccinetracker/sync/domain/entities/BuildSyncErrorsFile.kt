package com.jnj.vaccinetracker.sync.domain.entities

sealed class BuildSyncErrorsFile {
    data class FromOne(val metadata: SyncErrorMetadata) : BuildSyncErrorsFile()
    object FromAll : BuildSyncErrorsFile()
}