package com.jnj.vaccinetracker.common.helpers

import android.content.Intent
import android.net.Uri


fun createSyncErrorsShareIntent(uri: Uri) = createShareIntent(uri, "application/json", "Vaccine Tracker Sync Errors")
fun createLogFileShareIntent(uri: Uri) = createShareIntent(uri, "text/plain", "vmp_log.log")
fun createShareIntent(uri: Uri, type: String, subject: String): Intent {
    val sharingIntent = Intent(Intent.ACTION_SEND)
    sharingIntent.type = type
    sharingIntent.putExtra(Intent.EXTRA_STREAM, uri)
    sharingIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
    return Intent.createChooser(sharingIntent, null)
}