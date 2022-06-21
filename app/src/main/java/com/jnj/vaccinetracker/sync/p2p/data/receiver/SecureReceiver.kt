package com.jnj.vaccinetracker.sync.p2p.data.receiver

import com.jnj.vaccinetracker.sync.p2p.data.factories.AcceptedSenderFactory
import java.io.Closeable
import javax.net.ServerSocketFactory

class SecureReceiver(
    private val port: Int,
    private val acceptedSenderFactory: AcceptedSenderFactory,
) : Closeable {

    val localPort get() = serverSocket.localPort

    private val serverSocketFactory: ServerSocketFactory by lazy {
        ServerSocketFactory.getDefault()
    }

    private val serverSocket by lazy {
        serverSocketFactory.createServerSocket(port)
    }

    fun accept(): AcceptedSender {
        return acceptedSenderFactory.createSocket(serverSocket.accept())
    }

    override fun close() = serverSocket.close()
}