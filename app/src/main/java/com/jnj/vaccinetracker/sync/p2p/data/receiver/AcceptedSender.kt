package com.jnj.vaccinetracker.sync.p2p.data.receiver

import com.jnj.vaccinetracker.common.exceptions.HostNotAvailableException
import com.jnj.vaccinetracker.common.exceptions.PortNotAvailableException
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.sync.p2p.common.models.CompatibleNsdDevice
import com.jnj.vaccinetracker.sync.p2p.common.models.ReceivedData
import java.net.InetSocketAddress
import java.net.Socket

class AcceptedSender(
    private val raw: Socket,
) {
    companion object {
        private const val CONNECT_TIMEOUT = 2000
    }

    fun read(): ReceivedData {
        raw.use { socket ->
            val dataReceived: String = socket.inputStream.bufferedReader().use { it.readText() }
            logInfo("received data ports: ${socket.port} ${socket.localPort}")
            return ReceivedData(dataReceived, socket.inetAddress, socket.port)
        }
    }

    private fun CompatibleNsdDevice.toInetSocketAddress(): InetSocketAddress {
        if (host == null)
            throw HostNotAvailableException()
        if (port == null)
            throw PortNotAvailableException()
        return InetSocketAddress(
            host,
            port.port
        )
    }


    fun send(text: String, toDevice: CompatibleNsdDevice) {
        raw.use { socket ->
            socket.bind(null)
            val hostAddress = toDevice.toInetSocketAddress()
            socket.connect(hostAddress, CONNECT_TIMEOUT)
            socket.getOutputStream().bufferedWriter().use { out ->
                out.write(text)
            }
        }

    }
}