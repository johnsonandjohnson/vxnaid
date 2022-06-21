package com.jnj.vaccinetracker.sync.p2p.common.models.dtos.messages

import com.jnj.vaccinetracker.common.helpers.uuid
import com.squareup.moshi.JsonClass
import java.util.*

sealed class MessageHeaders {
    abstract val messageId: String
    abstract val deviceName: String
    abstract val deviceIp: String?
    abstract val devicePort: Int
    abstract val dateSend: Date
    abstract val osApiLevel: Int
}

@JsonClass(generateAdapter = true)
data class ClientMessageHeaders(
    override val messageId: String = uuid(),
    val sessionToken: String?,
    override val deviceName: String,
    override val deviceIp: String? = null,
    override val devicePort: Int,
    override val dateSend: Date = Date(),
    override val osApiLevel: Int,
) : MessageHeaders()

@JsonClass(generateAdapter = true)
data class ServerMessageHeaders(
    override val messageId: String = uuid(),
    val requestId: String?,
    override val deviceName: String,
    override val deviceIp: String? = null,
    override val devicePort: Int,
    override val dateSend: Date = Date(),
    override val osApiLevel: Int,
) : MessageHeaders()