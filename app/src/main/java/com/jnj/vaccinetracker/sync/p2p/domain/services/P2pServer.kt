package com.jnj.vaccinetracker.sync.p2p.domain.services

import android.os.Build
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.database.ParticipantRoomDatabase
import com.jnj.vaccinetracker.common.data.helpers.AndroidFiles
import com.jnj.vaccinetracker.common.data.helpers.Base64
import com.jnj.vaccinetracker.common.data.helpers.NsdConnectionState
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.domain.entities.ImageFileWithContent
import com.jnj.vaccinetracker.common.domain.entities.TemplateFileWithContent
import com.jnj.vaccinetracker.common.domain.entities.readContent
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.jnj.vaccinetracker.sync.p2p.common.Messenger
import com.jnj.vaccinetracker.sync.p2p.common.models.*
import com.jnj.vaccinetracker.sync.p2p.common.models.CompatibleNsdDevice.Companion.toNsdDevice
import com.jnj.vaccinetracker.sync.p2p.common.models.dtos.messages.ClientMessage
import com.jnj.vaccinetracker.sync.p2p.common.models.dtos.messages.ServerMessage
import com.jnj.vaccinetracker.sync.p2p.common.models.dtos.messages.ServerMessage.*
import com.jnj.vaccinetracker.sync.p2p.common.models.dtos.messages.ServerMessage.LoginReply.Companion.toLoginReplyStatus
import com.jnj.vaccinetracker.sync.p2p.common.models.dtos.messages.ServerMessageHeaders
import com.jnj.vaccinetracker.sync.p2p.common.util.DatabaseFolder
import com.jnj.vaccinetracker.sync.p2p.common.util.VmpDatabaseFiles
import com.jnj.vaccinetracker.sync.p2p.data.ClientMessageReceiver
import com.jnj.vaccinetracker.sync.p2p.data.factories.reply.SecretsReplyFactory
import com.jnj.vaccinetracker.sync.p2p.data.helpers.ServerProgressProvider
import com.jnj.vaccinetracker.sync.p2p.data.helpers.calcProgress
import com.jnj.vaccinetracker.sync.p2p.domain.entities.ServerProgress
import com.jnj.vaccinetracker.sync.p2p.domain.exceptions.SendMessageException
import com.jnj.vaccinetracker.sync.p2p.domain.usecases.IsDatabaseValidUseCase
import com.jnj.vaccinetracker.sync.p2p.domain.usecases.PrepareDatabaseForExportUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.IOException
import javax.inject.Inject


class P2pServer @Inject constructor(
    private val dispatchers: AppCoroutineDispatchers,
    private val messageReceiver: ClientMessageReceiver,
    private val messenger: Messenger,
    private val loginService: P2pLoginService,
    private val database: ParticipantRoomDatabase,
    private val databaseFiles: VmpDatabaseFiles,
    private val nsdService: NsdService,
    private val userRepository: UserRepository,
    private val androidFiles: AndroidFiles,
    private val base64: Base64,
    private val secretsReplyFactory: SecretsReplyFactory,
    private val nsdConnectionState: NsdConnectionState,
    private val syncSettingsRepository: SyncSettingsRepository,
    private val prepareDatabaseForExportUseCase: PrepareDatabaseForExportUseCase,
    private val reactiveNetworkConnectivity: ReactiveNetworkConnectivity,
    private val resourcesWrapper: ResourcesWrapper,
    private val isDatabaseValidUseCase: IsDatabaseValidUseCase,
    private val databaseFolder: DatabaseFolder,
    serverProgressProvider: ServerProgressProvider
) {

    var dataReceivedListener: (ClientMessage) -> Unit = {}
    private val job = SupervisorJob()
    private val scope
        get() = CoroutineScope(dispatchers.mainImmediate + job)

    private val isSendingDatabase = MutableStateFlow(false)

    val serverProgress = serverProgressProvider.serverProgress
    val errorMessage = MutableStateFlow<String?>(null)
    val nsdSession get() = loginService.session

    init {
        initState()
    }

    private fun initState() {
        scope.launch {
            messageReceiver.start(0)
        }
        messageReceiver.messagesReceived.filterNotNull().onEach { msg ->
            scope.launch {
                onMessageReceived(msg)
            }

        }.launchIn(scope)

        reactiveNetworkConnectivity.observeNetworkConnectivity().onEach {
            renderNetworkErrorMessage()
        }.launchIn(scope)
    }

    private fun renderNetworkErrorMessage() {
        if (!isNetworkWifiAvailable()) {
            errorMessage.value = resourcesWrapper.getString(R.string.p2p_wifi_not_available_error)
        } else {
            errorMessage.value = null
        }
    }

    private fun resetProgress() {
        serverProgress.value = ServerProgress.Idle
    }

    suspend fun registerService() {
        logInfo("registerService")
        val receiverInfo = messageReceiver.receiverInfo.await()
        nsdService.registerService(receiverInfo)
        nsdConnectionState.onNsdConnected()
    }

    fun dispose() {
        job.cancel()
        stopNsd()
        messageReceiver.close()
        loginService.clear()
        nsdConnectionState.onNsdDisconnected()
        resetProgress()
    }

    fun stopNsd() {
        nsdService.disconnect()
    }

    private suspend fun onError(ex: Exception) {
        yield()
        ex.rethrowIfFatal()
        logError("onError", ex)
        when {
            !isNetworkWifiAvailable() -> {
                errorMessage.value = resourcesWrapper.getString(R.string.p2p_wifi_not_available_error)
            }
            else -> {
                errorMessage.value = when (ex) {
                    is SendMessageException -> resourcesWrapper.getString(R.string.p2p_could_not_send_message_error)
                    is TimeoutCancellationException -> resourcesWrapper.getString(R.string.p2p_transfer_timeout_error)
                    else -> resourcesWrapper.getString(R.string.general_label_error)
                }
            }
        }
    }

    private fun isNetworkWifiAvailable() = reactiveNetworkConnectivity.connectivity.isConnectedWifi()

    private suspend fun createHeaders(requestId: String? = null): ServerMessageHeaders {
        val receiverInfo = messageReceiver.receiverInfo.await()
        return ServerMessageHeaders(
            requestId = requestId,
            deviceName = userRepository.getDeviceNameOrThrow(),
            devicePort = receiverInfo.port,
            osApiLevel = Build.VERSION.SDK_INT
        )
    }

    private suspend fun sendMessage(
        device: CompatibleNsdDevice,
        message: ServerMessage,
        headers: ServerMessageHeaders,
    ) {
        messenger.sendMessage(
            device,
            message.copyWithHeaders(headers),
            ServerMessage::class,
        )
    }


    private suspend fun requireAuthentication(message: ClientMessage) {
        val messageType = message::class.simpleName
        fun createError(errorMessage: String) =
            UnauthenticatedException("$errorMessage | messageType = $messageType")
        try {

            val headers =
                message.headers ?: throw createError("headers must not be null")
            val nsdDevice = headers.toNsdDevice()
            val session = loginService.getSession(nsdDevice)
                ?: throw createError("session not available for $nsdDevice")
            val sessionTokenMatches = session.sessionToken == headers.sessionToken
            if (sessionTokenMatches) {
                logInfo("${headers.deviceName} is authenticated for message $messageType")
            } else {
                throw createError("session token doesn't match [${session.sessionToken}, ${headers.sessionToken}]")
            }
        } catch (ex: UnauthenticatedException) {
            message.replyWith(AuthenticationRequiredReply())
            throw ex
        }
    }

    private suspend fun ClientMessage.replyWith(serverMessage: ServerMessage) {
        val headers = headersOrThrow()
        val device = headers.toNsdDevice()
        sendMessage(device, serverMessage, createHeaders(requestId = headers.messageId))
    }

    class UnauthenticatedException(override val message: String) : Exception()

    private suspend fun sendLoginReply(message: ClientMessage.Login) {
        serverProgress.value = ServerProgress.Authenticating
        try {
            val headers = message.headersOrThrow()
            val client = headers.toNsdDevice()
            val siteUuid = syncSettingsRepository.getSiteUuidOrThrow()
            if (message.siteUuid != siteUuid) {
                message.replyWith(LoginReply(LoginReply.Status.SITE_MISMATCH, siteUuid, null))
            } else {
                val result =
                    loginService.login(message.username, message.passwordHash, client)
                val reply = LoginReply(
                    status = result.loginStatus.toLoginReplyStatus(),
                    sessionToken = result.token,
                    requiredSiteUuid = siteUuid
                )
                message.replyWith(reply)
            }
        } finally {
            resetProgress()
        }
    }

    private suspend fun sendTemplatesReply(
        message: ClientMessage.ParticipantTemplateFilesRequest,
    ) = withContext(dispatchers.io) {
        requireAuthentication(message)
        try {
            logInfo("sendTemplatesReply ${message.files.size}")
            serverProgress.value = ServerProgress.UploadingTemplates(amount = message.files.size)
            val files = message.files.mapNotNull { file ->
                file.readContent(androidFiles)?.let { contentBytes ->
                    TemplateFileWithContent(
                        file,
                        base64.encode(contentBytes)
                    )
                }
            }
            message.replyWith(ParticipantTemplateFilesReply(files))
        } finally {
            resetProgress()
        }
    }

    private suspend fun sendImagesReply(
        message: ClientMessage.ParticipantImageFilesRequest,
    ) = withContext(dispatchers.io) {
        requireAuthentication(message)
        try {
            logInfo("sendImagesReply ${message.files.size}")
            serverProgress.value = ServerProgress.UploadingImages(amount = message.files.size)
            val files = message.files.mapNotNull { file ->
                file.readContent(androidFiles)?.let { contentBytes ->
                    ImageFileWithContent(
                        file,
                        base64.encode(contentBytes)
                    )
                }
            }
            message.replyWith(ParticipantImageFilesReply(files))
        } finally {
            resetProgress()
        }
    }

    private suspend fun getExistingDbFileCopy(): DbFileSnapshot? {
        val dbFileSrc = databaseFiles.getDbFile()
        val dbFileCopy = dbFileSrc.toCopy()
        return if (dbFileCopy.exists()) {
            if (isDatabaseValidUseCase.isDatabaseValid(dbFileCopy.name)) {
                dbFileCopy.createSnapshot()
            } else {
                logError("getExistingDbFileCopy exists but the database is not valid")
                dbFileCopy.delete(databaseFolder)
                null
            }
        } else {
            null
        }
    }

    private suspend fun makeDbFileSnapshot(): DbFileSnapshot {
        logInfo("makeDbFileSnapshot")
        val dbFileSrc = databaseFiles.getDbFile()
        val dbFileSrcSnapshot = dbFileSrc.createSnapshot()
        val dbFileCopy = dbFileSrc.saveCopy()
        return if (dbFileSrcSnapshot.isOutdated()) {
            logInfo("db file src has changed since creating snapshot and copy, retrying")
            dbFileCopy.delete(databaseFolder)
            makeDbFileSnapshot()
        } else {
            logInfo("db file src transferred to copy location, creating snapshot")
            val dbFileCopySnapshot = dbFileCopy.createSnapshot()
            if (dbFileSrcSnapshot.hash != dbFileCopySnapshot.hash) {
                dbFileCopy.delete(databaseFolder)
                logInfo("db file src hash is not matching with db file copy hash, retrying")
                makeDbFileSnapshot()
            } else {
                logInfo("created correct db file copy snapshot")
                prepareDatabaseForExportUseCase.prepareDatabaseForExport(dbFileCopySnapshot.dbFile.name)
                dbFileCopySnapshot
            }
        }
    }

    class UnexpectedProgressException : Exception()


    private suspend fun sendDatabaseReplies(
        message: ClientMessage.DatabaseFileRequest,
    ) {
        serverProgress.value = ServerProgress.UploadingDatabase(0)
        isSendingDatabase.value = true
        var currentMessage: ClientMessage.DatabaseFileRequest = message
        try {
            logInfo("sendDatabaseReplies")
            requireAuthentication(message)
            val skip: Long
            val existing = getExistingDbFileCopy()
            val dbFileSnapshot = if (existing != null) {
                logInfo("use existing copy")
                skip = message.currentFileLength
                existing
            } else {
                // reopening the database will flush 'write ahead logging' meaning we only have to transfer one file
                database.reopen()
                skip = 0
                makeDbFileSnapshot()
            }
            val dbFile = dbFileSnapshot.dbFile
            require(dbFile is DbFile.Copy) { "must transfer copy" }
            val copy = dbFile.file
            require(copy.exists()) { "error copy file does not exists" }
            copy.forEachBlockAsync(
                blockSize = DATABASE_FILE_TRANSFER_BLOCK_SIZE.toInt(),
                skip = skip,
            ) { buffer, bytesRead, progress, max, isFirstBytes ->
                val p = calcProgress(progress, max)
                serverProgress.value = ServerProgress.UploadingDatabase(p)
                val isAppend = !isFirstBytes
                logInfo("sending database part with size ${buffer.size}b while bytesRead=${bytesRead}b")
                val contentBase64 = base64.encode(buffer)
                val serverReply = DatabaseFileReply(
                    content = contentBase64,
                    bytesProgress = progress,
                    bytesMax = max,
                    isAppend = isAppend
                )
                currentMessage.replyWith(serverReply)
                // wait until client has received it
                when (val clientReply = messageReceiver.awaitFirstNextMessage()) {
                    is ClientMessage.DatabaseFileRequest -> {
                        requireAuthentication(clientReply)
                        currentMessage = clientReply
                        if (clientReply.currentFileLength != progress) {
                            logError("unexpected progress received (${clientReply.currentFileLength}) while expected $progress, retrying sendDatabaseReplies")
                            throw UnexpectedProgressException()
                        }
                    }
                    else -> {
                        error("unexpected response: $clientReply")
                    }
                }
                true
            }
            val serverReply =
                DatabaseFileTransferCompletedReply(
                    hash = dbFileSnapshot.hash,
                )
            currentMessage.replyWith(serverReply)
        } catch (e: UnexpectedProgressException) {
            return sendDatabaseReplies(currentMessage)
        } finally {
            isSendingDatabase.value = false
        }
    }

    private suspend fun replyWithSecrets(message: ClientMessage) {
        requireAuthentication(message)
        try {
            serverProgress.value = ServerProgress.UploadingSecrets
            val headers = message.headersOrThrow()
            message.replyWith(secretsReplyFactory.createSecretsReply(headers.osApiLevel))
        } finally {
            resetProgress()
        }
    }

    private fun createDatabaseErrorReason(e: Exception): DatabaseFileErrorReply.Reason {
        return when {
            androidFiles.isOutOfDiskSpace() -> DatabaseFileErrorReply.Reason.OutOfDiskSpace
            else -> when (e) {
                is IOException -> DatabaseFileErrorReply.Reason.IOError
                is TimeoutCancellationException -> DatabaseFileErrorReply.Reason.Timeout
                else -> DatabaseFileErrorReply.Reason.Unknown
            }
        }
    }

    private suspend fun onDatabaseFileRequest(message: ClientMessage.DatabaseFileRequest) {
        if (isSendingDatabase.value) {
            logInfo("already busy sending database")
            return
        }
        try {
            sendDatabaseReplies(message)
        } catch (e: Exception) {
            yield()
            e.rethrowIfFatal()
            logError("sendDatabaseReplies io error", e)
            val reason = createDatabaseErrorReason(e)
            if (reason == DatabaseFileErrorReply.Reason.OutOfDiskSpace) {
                logError("we are out of disk space so can't send database!")
            }
            message.replyWith(DatabaseFileErrorReply(reason))
        } finally {
            resetProgress()
        }
    }

    private suspend fun onMessageReceived(
        message: ClientMessage,
    ) {
        errorMessage.value = null
        logInfo("onMessageReceived: $message")
        dataReceivedListener(message)
        try {
            when (message) {
                is ClientMessage.DatabaseFileRequest -> {
                    onDatabaseFileRequest(message)
                }
                is ClientMessage.Login -> {
                    sendLoginReply(message)
                }
                is ClientMessage.ParticipantTemplateFilesRequest -> {
                    sendTemplatesReply(message)
                }
                is ClientMessage.ParticipantImageFilesRequest -> {
                    sendImagesReply(message)
                }
                is ClientMessage.SecretsRequest -> {
                    replyWithSecrets(message)
                }
            }.let { }

        } catch (ex: UnauthenticatedException) {
            logError("not authorized for message, $message", ex)
        } catch (ex: Exception) {
            onError(ex)
        }
    }

    companion object {
        private val DATABASE_FILE_TRANSFER_BLOCK_SIZE = 2.mb
    }
}