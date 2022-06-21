package com.jnj.vaccinetracker.sync.p2p.domain.services

import android.os.Build
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.database.ParticipantRoomDatabase
import com.jnj.vaccinetracker.common.data.database.repositories.DraftParticipantBiometricsTemplateRepository
import com.jnj.vaccinetracker.common.data.database.repositories.DraftParticipantImageRepository
import com.jnj.vaccinetracker.common.data.database.repositories.ParticipantBiometricsTemplateRepository
import com.jnj.vaccinetracker.common.data.database.repositories.ParticipantImageRepository
import com.jnj.vaccinetracker.common.data.database.repositories.base.ParticipantDataFileRepositoryCommon
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.data.encryption.PasswordHasher
import com.jnj.vaccinetracker.common.data.files.ParticipantDataFileIO
import com.jnj.vaccinetracker.common.data.helpers.*
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.domain.entities.*
import com.jnj.vaccinetracker.common.domain.usecases.masterdata.GetSitesUseCase
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.common.ui.plus
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.jnj.vaccinetracker.sync.data.repositories.SyncUserCredentialsRepository
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import com.jnj.vaccinetracker.sync.p2p.common.Messenger
import com.jnj.vaccinetracker.sync.p2p.common.models.CompatibleNsdDevice
import com.jnj.vaccinetracker.sync.p2p.common.models.LoginStatus
import com.jnj.vaccinetracker.sync.p2p.common.models.NsdDeviceEvent
import com.jnj.vaccinetracker.sync.p2p.common.models.dtos.messages.ClientMessage
import com.jnj.vaccinetracker.sync.p2p.common.models.dtos.messages.ClientMessageHeaders
import com.jnj.vaccinetracker.sync.p2p.common.models.dtos.messages.ServerMessage
import com.jnj.vaccinetracker.sync.p2p.common.util.VmpDatabaseFiles
import com.jnj.vaccinetracker.sync.p2p.data.ServerMessageReceiver
import com.jnj.vaccinetracker.sync.p2p.data.datasources.DatabaseImportedDateDataSource
import com.jnj.vaccinetracker.sync.p2p.data.helpers.ClientProgressProvider
import com.jnj.vaccinetracker.sync.p2p.data.helpers.calcProgress
import com.jnj.vaccinetracker.sync.p2p.domain.entities.ClientProgress
import com.jnj.vaccinetracker.sync.p2p.domain.exceptions.SendMessageException
import com.jnj.vaccinetracker.sync.p2p.domain.usecases.ApplySecretsUseCase
import com.jnj.vaccinetracker.sync.p2p.domain.usecases.IsDatabaseValidUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okio.buffer
import okio.sink
import java.io.File
import javax.inject.Inject

class P2pClient @Inject constructor(
    private val dispatchers: AppCoroutineDispatchers,
    private val messageReceiver: ServerMessageReceiver,
    private val messenger: Messenger,
    private val passwordHasher: PasswordHasher,
    private val md5HashGenerator: Md5HashGenerator,
    private val databaseFiles: VmpDatabaseFiles,
    private val nsdService: NsdService,
    private val userRepository: UserRepository,
    private val database: ParticipantRoomDatabase,
    private val participantBiometricsTemplateRepository: ParticipantBiometricsTemplateRepository,
    private val draftParticipantBiometricsTemplateRepository: DraftParticipantBiometricsTemplateRepository,
    private val imageRepository: ParticipantImageRepository,
    private val draftImageRepository: DraftParticipantImageRepository,
    private val androidFiles: AndroidFiles,
    private val base64: Base64,
    private val participantDataFileIO: ParticipantDataFileIO,
    private val applySecretsUseCase: ApplySecretsUseCase,
    private val isDatabaseValidUseCase: IsDatabaseValidUseCase,
    private val syncUserCredentialsRepository: SyncUserCredentialsRepository,
    private val nsdConnectionState: NsdConnectionState,
    private val syncSettingsRepository: SyncSettingsRepository,
    private val resourcesWrapper: ResourcesWrapper,
    private val getSitesUseCase: GetSitesUseCase,
    private val syncLogger: SyncLogger,
    private val databaseImportedDateDataSource: DatabaseImportedDateDataSource,
    private val reactiveNetworkConnectivity: ReactiveNetworkConnectivity,
    clientProgressProvider: ClientProgressProvider
) {
    var dataReceivedListener: (ServerMessage) -> Unit = {}
    val serviceDevice = MutableStateFlow<CompatibleNsdDevice?>(null)

    var sessionToken = MutableStateFlow<String?>(null)

    val loginStatus: Flow<LoginStatus> get() = sessionToken.map { if (it != null) LoginStatus.AUTHENTICATED else LoginStatus.UNAUTHENTICATED }

    val errorMessage = MutableStateFlow<String?>(null)

    val clientProgress = clientProgressProvider.clientProgress

    private val job = SupervisorJob()

    private val scope
        get() = CoroutineScope(dispatchers.mainImmediate + job)

    companion object {
        private const val DATA_FILE_BATCH_SIZE = 200
        private val DATABASE_IMPORT_DATE_THRESHOLD = 30.days
    }

    init {
        initState()
    }

    private fun resetProgress() {
        clientProgress.value = ClientProgress.Idle
    }

    private suspend fun login(): ServerMessage.LoginReply {
        try {
            val userCredentials = syncUserCredentialsRepository.getSyncUserCredentials()
            clientProgress.value = ClientProgress.LoggingIn
            val hash = passwordHasher.hash(userCredentials.password)
            sendMessageToServer(
                ClientMessage.Login(
                    username = userCredentials.username,
                    passwordHash = hash,
                    siteUuid = syncSettingsRepository.getSiteUuidOrThrow()
                ), createHeaders()
            )
            val serverMessage = messageReceiver.awaitFirstNextMessage()
            if (serverMessage is ServerMessage.LoginReply) {
                onLoginReply(serverMessage)
                return serverMessage
            } else {
                error("unknown message: $serverMessage")
            }
        } catch (ex: Exception) {
            onImportDataFromServerException(ex)
            throw ImportDataFromServerException()
        }
    }

    private class ImportDataFromServerException : Exception()

    private fun shouldImportDatabase(): Boolean {
        val lastImportDate = databaseImportedDateDataSource.getDatabaseImportedDate() ?: return true
        return lastImportDate + DATABASE_IMPORT_DATE_THRESHOLD < dateNow()
    }

    private suspend fun awaitSyncComplete() {
        clientProgress.value = ClientProgress.AwaitingDatabaseIdle
        syncLogger.awaitSyncPageProgressNotDownloading()
        syncLogger.awaitFailedBiometricsTemplateUploadNotInProgress()
    }

    suspend fun importDataFromServer() {
        errorMessage.value = null
        var success = false
        try {
            // login with sync admin
            val loginReply = login()
            when (loginReply.status) {
                ServerMessage.LoginReply.Status.AUTHENTICATED -> {
                    // Make sure we are not syncing
                    awaitSyncComplete()
                    // Then download secrets
                    doRequestSecrets()
                    if (shouldImportDatabase()) {
                        // Then Download the database binary file in chunked and import that downloaded file
                        doRequestDatabase()
                    }
                    // Now the database is complete, loop through the data files and the missing files
                    downloadMissingDataFiles()
                    success = true
                }
                ServerMessage.LoginReply.Status.UNAUTHENTICATED -> {
                    errorMessage.value = resourcesWrapper.getString(R.string.p2p_client_unauthenticated_error)
                }
                ServerMessage.LoginReply.Status.SITE_MISMATCH -> {
                    val sites = getSitesUseCase.getMasterData()
                    val requiredSite = sites.findByUuidOrThrow(loginReply.requiredSiteUuid)
                    val currentSite = sites.findByUuidOrThrow(syncSettingsRepository.getSiteUuidOrThrow())
                    errorMessage.value = resourcesWrapper.getString(R.string.p2p_client_site_mismatch_error, requiredSite.name, currentSite.name)
                }
            }.let { }
        } catch (ex: ImportDataFromServerException) {
            //no-op
        } finally {
            if (success) {
                clientProgress.value = ClientProgress.TransferCompletedSuccessfully
            } else {
                resetProgress()
            }
        }
    }

    private fun onNsdDeviceEvent(nsdDeviceEvent: NsdDeviceEvent) {
        nsdDeviceEvent.run {
            when (this) {
                is NsdDeviceEvent.Lost -> {
                    if (nsdDevice.deviceName == serviceDevice.value?.deviceName) {
                        logInfo("server connection lost")
                        sessionToken.value = null
                        serviceDevice.value = null
                    }
                    Unit
                }
                is NsdDeviceEvent.Resolved -> {
                    serviceDevice.value = nsdDevice
                }
            }.let {}
        }
    }

    private fun onLoginReply(loginReply: ServerMessage.LoginReply) {
        if (loginReply.sessionToken != null) {
            this.sessionToken.value = loginReply.sessionToken
        }
    }


    private fun initState() {
        scope.launch {
            messageReceiver.start(0)
        }
        messageReceiver.messagesReceived
            .filterNotNull().onEach { msg ->
                scope.launch {
                    onMessageReceived(msg)
                }
            }.launchIn(scope)
        nsdService.nsdDeviceEvents.onEach {
            onNsdDeviceEvent(it)
        }.launchIn(scope)
        reactiveNetworkConnectivity.observeNetworkConnectivity().onEach {
            renderNetworkErrorMessage()
        }.launchIn(scope)
    }

    private fun renderNetworkErrorMessage() {
        val wifiError = resourcesWrapper.getString(R.string.p2p_wifi_not_available_error)
        if (!isNetworkWifiAvailable()) {
            errorMessage.value = wifiError
        } else if (errorMessage.value == wifiError) {
            errorMessage.value = null
        }
    }

    fun startNsd() {
        nsdService.discoverServices()
        nsdConnectionState.onNsdConnected()
    }

    private val externalDbFolder by lazy {
        File(androidFiles.externalFiles, "database").also { it.mkdirs() }
    }

    private suspend fun ServerMessage.DatabaseFileReply.contentBytes(): ByteArray = base64.decode(content)

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun ServerMessage.DatabaseFileReply.writeAppendToDisk(
        file: File,
        append: Boolean,
    ) {
        withContext(dispatchers.io) {
            val bytes = contentBytes()
            logInfo("writeAppendToDisk:${file.name} ${bytes.size}b")
            file.sink(append = append).buffer().use { sink ->
                sink.write(bytes)
                sink.flush()
            }
        }
    }

    private suspend fun File.md5() = md5HashGenerator.md5(this)

    private suspend fun WaitForDatabaseReplyResult.handleResult() {
        return when (this) {
            WaitForDatabaseReplyResult.Completed -> {
                logInfo("WaitForDatabaseReplyResult Completed")
            }
            WaitForDatabaseReplyResult.ShouldWaitForMore -> {
                logInfo("WaitForDatabaseReplyResult ShouldWaitForMore")
                waitForDatabaseReplyAndSave().handleResult()
            }
        }
    }

    private sealed class WaitForDatabaseReplyResult {
        object ShouldWaitForMore : WaitForDatabaseReplyResult()
        object Completed : WaitForDatabaseReplyResult()
    }

    class ServerOutDiskSpaceException : Exception()


    /**
     * if we do regular recursion, old responses will stay in memory and we will run into OOM error
     * @return [WaitForDatabaseReplyResult] to indicate if if this method should be called again.
     * Call [WaitForDatabaseReplyResult.handleResult] to automatically wait for more replies until completed
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun waitForDatabaseReplyAndSave(): WaitForDatabaseReplyResult {
        val target = downloadedDbFile
        val targetTemp = target.toTemp()
        return when (val response = messageReceiver.awaitFirstNextMessage()) {
            is ServerMessage.DatabaseFileReply -> {
                val append = if (!response.isAppend) {
                    targetTemp.delete()
                    false
                } else {
                    true
                }
                response.writeAppendToDisk(targetTemp, append)
                val fileLength = targetTemp.length()
                logInfo("wrote database part, sending ack to server: $fileLength")
                val p = calcProgress(fileLength, response.bytesMax)
                clientProgress.value = ClientProgress.DownloadingDatabase(progress = p)
                sendMessageToServer(
                    ClientMessage.DatabaseFileRequest(
                        currentFileLength = fileLength
                    ), createHeaders()
                )
                WaitForDatabaseReplyResult.ShouldWaitForMore
            }
            is ServerMessage.DatabaseFileTransferCompletedReply -> {
                clientProgress.value = ClientProgress.ImportingDatabase
                target.delete()
                targetTemp.renameTo(target)
                targetTemp.delete()
                val isSuccessfullyDownloaded = response.hash == downloadedDbFile.md5()
                if (isSuccessfullyDownloaded) {
                    importDownloadedDatabase()
                } else {
                    logError("Some database files were not successfully downloaded")
                }
                WaitForDatabaseReplyResult.Completed
            }
            is ServerMessage.DatabaseFileErrorReply -> {
                val errorMessage = "database file error reply: ${response.reason}"
                logError(errorMessage)
                when (response.reason) {
                    ServerMessage.DatabaseFileErrorReply.Reason.IOError,
                    ServerMessage.DatabaseFileErrorReply.Reason.Timeout -> {
                        //try again
                        sendMessageToServer(
                            ClientMessage.DatabaseFileRequest(targetTemp.length()), createHeaders()
                        )
                        WaitForDatabaseReplyResult.ShouldWaitForMore
                    }
                    ServerMessage.DatabaseFileErrorReply.Reason.OutOfDiskSpace -> {
                        throw ServerOutDiskSpaceException()
                    }
                    ServerMessage.DatabaseFileErrorReply.Reason.Unknown -> error(errorMessage)
                }
            }
            else -> error("unexpected response: $response")
        }
    }

    private val downloadedDbFile by lazy {
        File(externalDbFolder, databaseFiles.getDbFile().name)
    }

    private suspend fun importDownloadedDatabase() = withContext(dispatchers.io) {
        logInfo("importDownloadedDatabase")
        require(downloadedDbFile.exists()) { "error can't import because downloaded db file doesn't exist" }
        val dbFile = databaseFiles.getDbFile()
        val targetFile = dbFile.file
        val tempTargetFile = targetFile.toTemp()
        tempTargetFile.delete()
        downloadedDbFile.copyTo(tempTargetFile)
        downloadedDbFile.delete()
        if (isDatabaseValidUseCase.isDatabaseValid(tempTargetFile.name)) {
            logInfo("download database is valid, swapping passphrase and db file")
            database.closeShorty {
                require(databaseFiles.delete()) { "dbFile couldn't be deleted" }
                require(tempTargetFile.renameTo(dbFile.file)) { "TempFile couldn't be renamed to db file" }
            }
            onDatabaseImported()
        } else {
            error("downloaded database is not valid! ")
        }
    }

    private fun onDatabaseImported() {
        databaseImportedDateDataSource.setDatabaseImportedDate(dateNow())
    }

    private fun calcDownloadedDatabaseFileLength(): Long {
        val tempDbFile = downloadedDbFile.toTemp()
        return tempDbFile.length()
    }

    private fun isNetworkWifiAvailable() = reactiveNetworkConnectivity.connectivity.isConnectedWifi()

    private fun createLocalizedImportDataServerExceptionMessage(ex: Exception): String {
        return when {
            !isNetworkWifiAvailable() -> resourcesWrapper.getString(R.string.p2p_wifi_not_available_error)
            androidFiles.isOutOfDiskSpace() -> resourcesWrapper.getString(R.string.p2p_client_this_device_out_of_space_error)
            else -> when (ex) {
                is ServerOutDiskSpaceException -> resourcesWrapper.getString(R.string.p2p_client_server_out_of_space_error)
                is SendMessageException -> resourcesWrapper.getString(R.string.p2p_could_not_send_message_error)
                is TimeoutCancellationException -> resourcesWrapper.getString(R.string.p2p_transfer_timeout_error)
                else -> clientProgress.value.toErrorLocalized(resourcesWrapper)
            }
        }
    }

    private suspend fun onImportDataFromServerException(ex: Exception) {
        yield()
        ex.rethrowIfFatal()
        when (ex) {
            is SendMessageException, is TimeoutCancellationException -> {
                nsdService.rediscoverServices()
            }
        }
        val localizedErrorMessage = createLocalizedImportDataServerExceptionMessage(ex)
        logError("error importing data from server during progress: ${clientProgress.value}, localizedMessage: $localizedErrorMessage", ex)
        errorMessage.value = localizedErrorMessage
        throw ImportDataFromServerException()
    }

    private suspend fun doRequestSecrets() {
        logInfo("doRequestSecrets")
        try {
            clientProgress.value = ClientProgress.DownloadingSecrets
            sendMessageToServer(ClientMessage.SecretsRequest(), createHeaders())
            when (val response = messageReceiver.awaitFirstNextMessage()) {
                is ServerMessage.SecretsReply -> {
                    clientProgress.value = ClientProgress.ImportingSecrets
                    val headers = response.headersOrThrow()
                    applySecretsUseCase.applySecrets(
                        databasePassphraseEncrypted = response.databasePassphrase,
                        dataFileSecretEncrypted = response.dataFileSecret, headers.osApiLevel
                    )
                }
                else -> error("unexpected reply: $response")
            }.let {}
        } catch (ex: Exception) {
            onImportDataFromServerException(ex)
        }
    }

    private suspend fun doRequestDatabase() {
        logInfo("doRequestDatabase")
        try {
            val currentFileLength = calcDownloadedDatabaseFileLength()
            clientProgress.value = ClientProgress.DownloadingDatabase(0)
            sendMessageToServer(
                ClientMessage.DatabaseFileRequest(currentFileLength = currentFileLength),
                createHeaders()
            )
            waitForDatabaseReplyAndSave().handleResult()
        } catch (ex: Exception) {
            onImportDataFromServerException(ex)
        }
    }

    private suspend fun ParticipantDataFileWithContent.save() {
        val contentBytes = base64.decode(content)
        participantDataFileIO.writeParticipantDataFile(file, contentBytes, overwrite = true, isEncrypted = true)
    }

    private suspend fun onDataFilesReceived(filesRequested: List<ParticipantDataFile>, filesReceived: List<ParticipantDataFileWithContent>) {
        logInfo("onDataFilesReceived")
        if (filesReceived.size != filesRequested.size) {
            // if some files are not returned, then it means they could not be read
            logError("response.files.size ${filesReceived.size} not equal to files.size ${filesRequested.size}")
        }
        runTasks(filesReceived, debugLabel = "saveDataFiles") {
            it.save()
        }
    }

    private suspend fun downloadTemplateFiles(files: List<ParticipantBiometricsTemplateFileBase>) {
        sendMessageToServer(
            ClientMessage.ParticipantTemplateFilesRequest(
                files = files
            ), createHeaders()
        )
        val response = messageReceiver.awaitFirstNextMessage() as ServerMessage.ParticipantTemplateFilesReply
        onDataFilesReceived(files, response.files)
    }

    private suspend fun downloadImageFiles(files: List<ParticipantImageFileBase>) {
        sendMessageToServer(
            ClientMessage.ParticipantImageFilesRequest(
                files = files
            ), createHeaders()
        )
        val response = messageReceiver.awaitFirstNextMessage() as ServerMessage.ParticipantImageFilesReply
        onDataFilesReceived(files, response.files)
    }

    private suspend fun <T : ParticipantDataFile> ParticipantDataFileRepositoryCommon<T>.onEachChunked(block: suspend (List<T>, progress: Int) -> Unit) {
        val max = count()
        var totalProcessed = 0L
        forEachAll { files ->
            files.chunked(DATA_FILE_BATCH_SIZE).onEach { chunkedItems ->
                val p = calcProgress(totalProcessed, max)
                totalProcessed += chunkedItems.size
                block(chunkedItems, p)
            }
        }
    }

    private suspend fun downloadMissingTemplates() {
        logInfo("downloadMissingTemplates")
        listOf(participantBiometricsTemplateRepository, draftParticipantBiometricsTemplateRepository).forEach { repo ->
            val isDraft = repo is DraftParticipantBiometricsTemplateRepository
            clientProgress.value = ClientProgress.DownloadingTemplates(0, isDraft = isDraft)
            try {
                repo.onEachChunked { templates, progress ->
                    val missingTemplates = templates.filter { !it.exists(androidFiles) }
                    if (missingTemplates.isNotEmpty())
                        downloadTemplateFiles(missingTemplates)
                    clientProgress.value = ClientProgress.DownloadingTemplates(progress, isDraft = isDraft)
                }
            } catch (ex: Exception) {
                onImportDataFromServerException(ex)
            }
        }

    }

    private suspend fun downloadMissingImages() {
        logInfo("downloadMissingImages")
        listOf(imageRepository, draftImageRepository).forEach { repo ->
            val isDraft = repo is DraftParticipantImageRepository
            clientProgress.value = ClientProgress.DownloadingImages(0, isDraft = isDraft)
            try {
                repo.onEachChunked { images, progress ->
                    val missingImages = images.filter { !it.exists(androidFiles) }
                    if (missingImages.isNotEmpty())
                        downloadImageFiles(missingImages)
                    clientProgress.value = ClientProgress.DownloadingImages(progress, isDraft = isDraft)
                }
            } catch (ex: Exception) {
                onImportDataFromServerException(ex)
            }
        }

    }

    private suspend fun downloadMissingDataFiles() = withContext(dispatchers.io) {
        downloadMissingTemplates()
        downloadMissingImages()
    }

    private suspend fun createHeaders(): ClientMessageHeaders {
        val receiverInfo = messageReceiver.receiverInfo.await()
        return ClientMessageHeaders(
            deviceName = userRepository.getDeviceNameOrThrow(),
            sessionToken = sessionToken.value,
            devicePort = receiverInfo.port,
            osApiLevel = Build.VERSION.SDK_INT
        )
    }

    /**
     * Send a message to the service device.
     *
     * @param message The message to be sent.
     */
    private suspend fun sendMessageToServer(message: ClientMessage, headers: ClientMessageHeaders) {
        logInfo("sendMessageToServer: $message")
        val device = requireNotNull(serviceDevice.value) { "serviceDevice must not be null" }
        sendMessage(device, message, headers)
    }

    /**
     * Send a message to the desired device who it's connected in the group.
     *
     * @param device  The receiver of the message.
     * @param message The message to be sent.
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun sendMessage(
        device: CompatibleNsdDevice,
        message: ClientMessage,
        headers: ClientMessageHeaders,
    ) {
        messenger.sendMessage(
            device,
            message.copyWithHeaders(headers),
            ClientMessage::class,
        )
    }

    fun dispose() {
        job.cancel()
        messageReceiver.close()
        stopNsd()
        nsdConnectionState.onNsdDisconnected()
        resetProgress()
    }

    fun stopNsd() {
        serviceDevice.value = null
        nsdService.disconnect()
    }

    fun reconnect() {
        nsdService.rediscoverServices()
    }

    private fun onMessageReceived(message: ServerMessage) {
        logInfo("onMessageReceived: $message")
        errorMessage.value = null
        dataReceivedListener(message)
        when (message) {
            is ServerMessage.LoginReply -> {
                onLoginReply(message)
            }
            is ServerMessage.AuthenticationRequiredReply -> {
                logError("Error, authentication required!")
                // assume session token is invalid
                sessionToken.value = null
                errorMessage.value = resourcesWrapper.getString(R.string.p2p_client_authentication_error)
            }
            is ServerMessage.ParticipantTemplateFilesReply,
            is ServerMessage.DatabaseFileReply,
            is ServerMessage.DatabaseFileTransferCompletedReply,
            is ServerMessage.DatabaseFileErrorReply,
            is ServerMessage.SecretsReply,
            is ServerMessage.ParticipantImageFilesReply -> logInfo(
                "handled somewhere else: ${message::class.simpleName}"
            )
        }.let { }

    }
}