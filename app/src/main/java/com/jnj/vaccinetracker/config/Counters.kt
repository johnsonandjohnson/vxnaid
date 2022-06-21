package com.jnj.vaccinetracker.config

import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource

object Counters {

    object DownstreamSync {
        val DELAY_SERVER_POLL = 10.minutes
        val DELAY_FAILED_RECORD_DOWNLOAD = 1.hours

        val SHOULD_OPTIMIZE_DATE_OFFSET = 1.days
    }

    object UpstreamSync {
        val MAX_SKIP_DURATION = 1.hours
        val RETRY_DELAY = 5.seconds
        const val UPLOAD_RETRY_COUNT = 3
        val OBSERVE_DRAFT_TABLES_DELAY = 250.milliseconds
        val OBSERVE_ERRORS_DEBOUNCE = 1000.milliseconds
    }

    object FailedUploadBiometricTemplateSync {
        val TIME_SINCE_LAST_UPLOAD_ATTEMPT = 1.hours
        val DELAY = TIME_SINCE_LAST_UPLOAD_ATTEMPT
    }

    object SyncWebCall {
        const val SYNC_API_RETRY_COUNT = 1
        val RETRY_DELAY = 3.seconds
    }

    object InternetConnectivity {
        val POLL_INTERNET_DELAY = 1.seconds
    }

    object ServerHealthMeter {
        val HEALTH_CALL_DELAY = 5.seconds
    }

    object ActiveUsersSync {
        val DELAY = 15.minutes
    }

    object LicenseSync {
        val DELAY_NO_LICENSE = 20.minutes
    }

    object MasterDataSync {
        /**
         * how often will we call [VaccineTrackerSyncApiDataSource.getMasterDataUpdates]
         */
        val DELAY = 10.minutes

        /**
         * when a master data file has never been downloaded before
         * and there's an error during the call to download and store it,
         * how long should we wait until we try again?
         */
        val FIRST_INIT_ERROR_RETRY_DELAY = 1.minutes
    }

    object SyncCompletedDateReporting {
        val DELAY = 5.minutes
    }

    object UpstreamSyncErrorSync {
        val RETRY_DELAY = 15.minutes

        /**
         * How long we will wait before uploading errors
         */
        val OBSERVE_ERRORS_DEBOUNCE = 5.seconds
    }

    object UserExpirySync {
        val DELAY = 1.minutes
    }

    object ChangeDeviceName {
        const val RETRY_COUNT = 3
        val SHORT_RETRY_DELAY = 5.seconds
        val LONG_RETRY_DELAY = 10.minutes
    }

    object SyncState {
        val DELAY = 5.seconds
        val DEBOUNCE_PROGRESS_CHANGE = 1.seconds
        val SYNC_COMPLETE_DURATION = 5.seconds
    }
}