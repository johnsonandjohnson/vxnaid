package com.jnj.vaccinetracker.sync.p2p.domain.entities

import java.util.*

sealed class ServerProgress : P2pProgress {
    object Idle : ServerProgress()
    object Authenticating : ServerProgress()
    object UploadingSecrets : ServerProgress()
    data class UploadingDatabase(override val progress: Int) : ServerProgress(), BaseDeterminateProgress
    data class UploadingTemplates(val amount: Int, val date: Date = Date()) : ServerProgress()
    data class UploadingImages(val amount: Int, val date: Date = Date()) : ServerProgress()

    override fun isIdle() = this is Idle

}