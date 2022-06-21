package com.jnj.vaccinetracker.update

import android.annotation.SuppressLint
import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import com.jnj.vaccinetracker.BuildConfig
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.helpers.AndroidFiles
import com.jnj.vaccinetracker.common.data.managers.UpdateManager
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.domain.entities.VaccineTrackerVersion
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File
import javax.inject.Inject


class UpdateViewModel @Inject constructor(
    private val resourcesWrapper: ResourcesWrapper,
    private val androidFiles: AndroidFiles,
    private val app: Application,
    override val dispatchers: AppCoroutineDispatchers,
    private val updateManager: UpdateManager,
    private val networkConnectivity: NetworkConnectivity,
    private val syncLogger: SyncLogger,
) : ViewModelBase() {

    val startedDownload = mutableLiveBoolean()
    val downloadAppProgress = mutableLiveInt()
    val downloadSize = mutableLiveData<String>()
    val currentVersion = BuildConfig.VERSION_NAME
    val newVersion = mutableLiveData("")
    val errorMessage = mutableLiveData<String>()

    private lateinit var serverLatestVersion: VaccineTrackerVersion
    var downloadId: Long = 0
    var destContentUri: Uri = Uri.EMPTY

    val downloadCompletedEvent = eventFlow<Unit>()
    val finishedDownload = mutableLiveBoolean()

    init {
        initVersion()
        observeNetwork()
    }

    private fun observeNetwork() {
        val errorNoNetwork = resourcesWrapper.getString(R.string.update_error_no_network)

        networkConnectivity.observeNetworkConnectivity().onEach { isConnected ->
            if (!isConnected) {
                errorMessage.set(errorNoNetwork)
            } else {
                if (errorMessage.get() == errorNoNetwork) {
                    errorMessage.set(null)
                }
            }
        }.launchIn(scope)
    }

    private fun initVersion() {
        scope.launch {
            try {
                serverLatestVersion = updateManager.getLatestVersion()
                val sb = StringBuilder()
                sb.append(serverLatestVersion.version / 10000)
                sb.append(".")
                sb.append((serverLatestVersion.version / 100) % 100)
                sb.append(".")
                sb.append(serverLatestVersion.version % 100)
                newVersion.set(sb.toString())
            } catch (ex: Throwable) {
                yield()
                ex.rethrowIfFatal()
                errorMessage.set(resourcesWrapper.getString(R.string.update_error_version_check))
                logError("Error fetching latest app version: ", ex)
            }
        }
    }

    fun update() {
        scope.launch {
            try {
                downloadFromUrl(serverLatestVersion.url, resourcesWrapper.getString(R.string.app_name) + "_" + serverLatestVersion.version + ".apk")
            } catch (ex: Throwable) {
                yield()
                ex.rethrowIfFatal()
                errorMessage.set(resourcesWrapper.getString(R.string.update_error_downloading))
                logError("Error downloading updated application: ", ex)
            }
        }
    }

    /**
     * Download the file from the given URL and save in the external files directory under the given filename
     * Emits onto the downloadCompleted eventFlow upon completion.
     *
     * @param url       URL from which to download the file
     * @param fileName  Filename to store the downloaded file as
     */
    @SuppressLint("Range")
    private suspend fun downloadFromUrl(url: String, fileName: String) = withContext(dispatchers.io) {
        val filePath = File(androidFiles.externalFiles, fileName)
        if (filePath.exists()) filePath.delete()

        val request = DownloadManager.Request(Uri.parse(url))
        request.setDescription(resourcesWrapper.getString(R.string.update_version))
        request.setTitle(resourcesWrapper.getString(R.string.app_name))
        val destFileUri = Uri.parse("file://$filePath")
        destContentUri = androidFiles.getUriForFile(filePath)
        request.setDestinationUri(destFileUri)

        // get download service and enqueue file
        val manager = app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        var downloading = true
        finishedDownload.set(false)
        startedDownload.set(true)

        downloadId = manager.enqueue(request)
        logInfo("Application download started")

        while (downloading) {
            if (!isActive)
                break
            val q = DownloadManager.Query()
            q.setFilterById(downloadId)
            manager.query(q).use { cursor ->
                cursor.moveToFirst()
                val bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                    downloading = false
                }
                val bytesTotal: Int = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                downloadSize.set("%.1f".format(bytesTotal.toDouble() / 1048576) + "MB")
                if (bytesTotal != 0) {
                    val dlprogress = (bytesDownloaded * 100L / bytesTotal).toInt()
                    downloadAppProgress.set(dlprogress)
                    logDebug("progress: $dlprogress")
                    // Small delay to slow down loop and let interface draw progress
                    delay(100L)
                }
            }
        }
        logInfo("awaiting sync not in progress")
        // We don't want to app process to be killed while sync is on going
        syncLogger.awaitSyncNotInProgress()

        // Once download complete, emit event for the fragment to start activity to open the file
        logInfo("Application download complete")
        finishedDownload.set(true)
        downloadCompletedEvent.tryEmit(Unit)
    }

    fun onCancelClick() {
        if (downloadId > 0) {
            val dlManager = app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dlManager.remove(downloadId)
        }
    }
}