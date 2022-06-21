package com.jnj.vaccinetracker.sync.p2p.data

import com.jnj.vaccinetracker.common.data.helpers.Json
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.await
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.sync.p2p.common.models.ReceiverInfo
import com.jnj.vaccinetracker.sync.p2p.common.models.dtos.messages.ClientMessage
import com.jnj.vaccinetracker.sync.p2p.common.models.dtos.messages.MessageBase
import com.jnj.vaccinetracker.sync.p2p.common.models.dtos.messages.ServerMessage
import com.jnj.vaccinetracker.sync.p2p.data.factories.ReceiverFactory
import com.jnj.vaccinetracker.sync.p2p.data.receiver.AcceptedSender
import com.jnj.vaccinetracker.sync.p2p.data.receiver.SecureReceiver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import javax.inject.Inject

abstract class MessageReceiver<M : MessageBase> {

    private val job = SupervisorJob()
    private val dispatchers = AppCoroutineDispatchers.DEFAULT

    private val scope
        get() = CoroutineScope(dispatchers.mainImmediate + job)
    private val serverSocket = MutableStateFlow<SecureReceiver?>(null)
    val receiverInfo: Flow<ReceiverInfo>
        get() = serverSocket.filterNotNull().map { it.toReceiverInfo() }
    val messagesReceived = MutableSharedFlow<M>(extraBufferCapacity = 10)

    protected abstract fun parseJson(json: String): M

    protected abstract val receiverFactory: ReceiverFactory

    @Suppress("BlockingMethodInNonBlockingContext")

    suspend fun awaitFirstNextMessage(timeoutMillis: Long = 30_000): M {
        logInfo("awaiting first message")
        return withTimeout(timeoutMillis) {
            try {
                messagesReceived.await()
            } catch (ex: TimeoutCancellationException) {
                logError("awaitFirstNextMessage: TimeoutCancellationException")
                throw ex
            }
        }
    }

    /**
     * @param port A port number of 0 means that the port number is automatically allocated
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun start(port: Int) = withContext(dispatchers.io) {
        if (serverSocket.value == null) {
            serverSocket.value = receiverFactory.createReceiver(port).also { socket ->
                scope.launch(dispatchers.io) {
                    waitForMessages(socket)
                }
            }
        }
    }

    fun close() {
        job.cancel()
        try {
            serverSocket.value?.close()
            serverSocket.value = null
            logInfo("ServerSocket closed")
        } catch (e: IOException) {
            logError("Error closing the serverSocket")
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun CoroutineScope.handleSocket(socket: AcceptedSender) {
        launch {
            try {
                val dataReceived = socket.read()
                val ip = dataReceived.fromAddress.hostAddress!!
                val port = dataReceived.port
                logInfo("From IP: $ip with port $port")
                val message: M = parseJson(dataReceived.data)
                logInfo("Message received: $message")
                messagesReceived.tryEmit(addFromAddressToMessage(message, ip))
            } catch (ex: IOException) {
                logError("handleSocket failed", ex)
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun CoroutineScope.waitForMessages(serverSocket: SecureReceiver) {
        try {
            while (isActive) {
                logInfo("waiting for server messages at port ${serverSocket.localPort}")
                // creates new socket
                val socket = serverSocket.accept()
                handleSocket(socket)
            }
        } catch (ex: IOException) {
            logError("Error looping client ServerSocket: " + ex.message)
        }
    }

    protected abstract fun addFromAddressToMessage(message: M, ipAddress: String): M
}

class ServerMessageReceiver @Inject constructor(override val receiverFactory: ReceiverFactory, private val json: Json) :
    MessageReceiver<ServerMessage>() {

    override fun addFromAddressToMessage(
        message: ServerMessage,
        ipAddress: String,
    ): ServerMessage =
        message.copyWithHeaders(message.headers?.copy(deviceIp = ipAddress))


    override fun parseJson(json: String): ServerMessage {
        return this.json.parse(json)
    }
}

class ClientMessageReceiver @Inject constructor(override val receiverFactory: ReceiverFactory, private val json: Json) :
    MessageReceiver<ClientMessage>() {
    override fun parseJson(json: String): ClientMessage {
        return this.json.parse(json)
    }

    override fun addFromAddressToMessage(
        message: ClientMessage,
        ipAddress: String,
    ): ClientMessage =
        message.copyWithHeaders(message.headers?.copy(deviceIp = ipAddress))
}