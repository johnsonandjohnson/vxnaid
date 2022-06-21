package com.jnj.vaccinetracker.sync.p2p.data

import com.jnj.vaccinetracker.sync.p2p.common.models.ReceiverInfo
import com.jnj.vaccinetracker.sync.p2p.data.receiver.SecureReceiver

fun SecureReceiver.toReceiverInfo() = ReceiverInfo(port = localPort)