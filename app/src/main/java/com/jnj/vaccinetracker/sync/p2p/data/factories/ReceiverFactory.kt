package com.jnj.vaccinetracker.sync.p2p.data.factories

import com.jnj.vaccinetracker.sync.p2p.data.receiver.SecureReceiver
import javax.inject.Inject


class ReceiverFactory @Inject constructor(private val acceptedSenderFactory: AcceptedSenderFactory) {

    fun createReceiver(port: Int) = SecureReceiver(port, acceptedSenderFactory)
}