package com.jnj.vaccinetracker.sync.p2p.domain.entities

import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.di.ResourcesWrapper

sealed class ClientProgress : P2pProgress {
    object Idle : ClientProgress()
    object AwaitingDatabaseIdle : ClientProgress()
    object LoggingIn : ClientProgress()
    object DownloadingSecrets : ClientProgress()
    object ImportingSecrets : ClientProgress()
    data class DownloadingDatabase(override val progress: Int) : ClientProgress(), BaseDeterminateProgress
    object ImportingDatabase : ClientProgress()
    data class DownloadingTemplates(override val progress: Int, val isDraft: Boolean) : ClientProgress(), BaseDeterminateProgress
    data class DownloadingImages(override val progress: Int, val isDraft: Boolean) : ClientProgress(), BaseDeterminateProgress
    object TransferCompletedSuccessfully : ClientProgress()

    fun isCompletedSuccessfully() = this is TransferCompletedSuccessfully

    override fun isIdle() = this is Idle

    fun displayName(resourcesWrapper: ResourcesWrapper): String {
        return when (this) {
            is DownloadingDatabase -> resourcesWrapper.getString(R.string.p2p_client_progress_downloading_database)
            is DownloadingImages -> {
                val resId = if (isDraft) R.string.p2p_client_progress_downloading_draft_images
                else R.string.p2p_client_progress_downloading_images
                resourcesWrapper.getString(resId)
            }
            DownloadingSecrets -> resourcesWrapper.getString(R.string.p2p_client_progress_downloading_secrets)
            is DownloadingTemplates -> {
                val resId = if (isDraft) R.string.p2p_client_progress_downloading_draft_templates
                else R.string.p2p_client_progress_downloading_templates
                resourcesWrapper.getString(resId)
            }
            Idle -> ""
            ImportingDatabase -> resourcesWrapper.getString(R.string.p2p_client_progress_importing_database)
            ImportingSecrets -> resourcesWrapper.getString(R.string.p2p_client_progress_importing_secrets)
            LoggingIn -> resourcesWrapper.getString(R.string.p2p_client_progress_logging_in)
            TransferCompletedSuccessfully -> resourcesWrapper.getString(R.string.p2p_client_progress_completed_success)
            AwaitingDatabaseIdle -> resourcesWrapper.getString(R.string.p2p_client_progress_await_database_idle)
        }
    }

    fun toErrorLocalized(resourcesWrapper: ResourcesWrapper): String {
        val displayName = displayName(resourcesWrapper)
        return if (displayName.isNotEmpty()) {
            resourcesWrapper.getString(R.string.p2p_client_transfer_error, displayName)
        } else {
            resourcesWrapper.getString(R.string.general_label_error)
        }
    }

    fun isNotInProgress() = isIdle() || isCompletedSuccessfully()
}