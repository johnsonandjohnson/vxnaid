package com.jnj.vaccinetracker.sync.p2p.data.factories

import com.jnj.vaccinetracker.sync.p2p.data.receiver.AcceptedSender
import java.net.Socket
import javax.inject.Inject
import javax.net.SocketFactory

class AcceptedSenderFactory @Inject constructor() {

    private val socketFactory: SocketFactory by lazy {
        SocketFactory.getDefault()
    }

    fun createSocket(raw: Socket? = null): AcceptedSender {
        return AcceptedSender(raw ?: socketFactory.createSocket())
    }
}