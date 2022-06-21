package com.jnj.vaccinetracker.sync.p2p.common

import com.jnj.vaccinetracker.common.data.helpers.Json
import com.jnj.vaccinetracker.common.exceptions.HostNotAvailableException
import com.jnj.vaccinetracker.common.exceptions.PortNotAvailableException
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.sync.p2p.common.models.CompatibleNsdDevice
import com.jnj.vaccinetracker.sync.p2p.common.models.dtos.messages.MessageBase
import com.jnj.vaccinetracker.sync.p2p.data.factories.AcceptedSenderFactory
import com.jnj.vaccinetracker.sync.p2p.domain.exceptions.SendMessageException
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import kotlin.reflect.KClass

class Messenger @Inject constructor(
    private val dispatchers: AppCoroutineDispatchers,
    private val acceptedSenderFactory: AcceptedSenderFactory,
    private val json: Json,
) {

    private val tag = this

    /**
     * Send a message to the desired device who it's connected in the group.
     *
     * @param device  The receiver of the message.
     * @param message The message to be sent.
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun <T : MessageBase> sendMessage(
        device: CompatibleNsdDevice,
        message: T,
        messageClass: KClass<out T>,
    ) {
        logInfo("sendMessage to $device with class ${messageClass.simpleName}")
        withContext(dispatchers.io) {
            requireNotNull(message.headers) { "headers must not be null" }
            // Set the actual device to the message
            try {
                acceptedSenderFactory.createSocket()
                    .let { socket ->
                        val messageJson = json.stringify(message, messageClass.java)
                        tag.logInfo("Sending data: $message")
                        socket.send(messageJson, device)
                    }
            } catch (e: IOException) {
                tag.logError("Error creating client socket", e)
                throw SendMessageException(e)
            } catch (e: HostNotAvailableException) {
                tag.logError("could not send message because host not specified in $device")
                throw SendMessageException(e)
            } catch (e: PortNotAvailableException) {
                tag.logError("could not send message because port not specified in $device")
                throw SendMessageException(e)
            }
        }
    }
}