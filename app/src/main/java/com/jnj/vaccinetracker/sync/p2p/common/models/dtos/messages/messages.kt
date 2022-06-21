package com.jnj.vaccinetracker.sync.p2p.common.models.dtos.messages

import com.jnj.vaccinetracker.common.domain.entities.ImageFileWithContent
import com.jnj.vaccinetracker.common.domain.entities.ParticipantBiometricsTemplateFileBase
import com.jnj.vaccinetracker.common.domain.entities.ParticipantImageFileBase
import com.jnj.vaccinetracker.common.domain.entities.TemplateFileWithContent
import com.jnj.vaccinetracker.sync.p2p.common.models.LoginStatus
import com.jnj.vaccinetracker.sync.p2p.common.models.dtos.EncryptedSecret
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel

sealed class MessageBase {
    abstract val headers: MessageHeaders?
}

/**
 * Message from client to server
 */
@JsonClass(generateAdapter = true, generator = "sealed:type")
sealed class ClientMessage : MessageBase() {
    abstract override val headers: ClientMessageHeaders?
    fun headersOrThrow() = requireNotNull(headers) { "headers must not be null" }
    abstract fun copyWithHeaders(headers: ClientMessageHeaders?): ClientMessage

    @JsonClass(generateAdapter = true)
    @TypeLabel("login")
    data class Login(
        val username: String,
        val passwordHash: String,
        val siteUuid: String,
        override val headers: ClientMessageHeaders? = null,
    ) : ClientMessage() {
        override fun copyWithHeaders(headers: ClientMessageHeaders?) = copy(headers = headers)
        override fun toString(): String {
            return "Login: $username"
        }
    }

    @JsonClass(generateAdapter = true)
    @TypeLabel("secrets_request")
    data class SecretsRequest(
        override val headers: ClientMessageHeaders? = null,
    ) : ClientMessage() {
        override fun copyWithHeaders(headers: ClientMessageHeaders?) = copy(headers = headers)
        override fun toString(): String {
            return "SecretsRequest"
        }
    }

    @JsonClass(generateAdapter = true)
    @TypeLabel("participant_template_files_request")
    data class ParticipantTemplateFilesRequest(
        val files: List<ParticipantBiometricsTemplateFileBase>,
        override val headers: ClientMessageHeaders? = null,
    ) : ClientMessage() {

        override fun copyWithHeaders(headers: ClientMessageHeaders?) = copy(headers = headers)
        override fun toString(): String {
            return "ParticipantTemplateFilesRequest: ${files.size}"
        }
    }

    @JsonClass(generateAdapter = true)
    @TypeLabel("participant_image_files_request")
    data class ParticipantImageFilesRequest(
        val files: List<ParticipantImageFileBase>,
        override val headers: ClientMessageHeaders? = null,
    ) : ClientMessage() {

        override fun copyWithHeaders(headers: ClientMessageHeaders?) = copy(headers = headers)
        override fun toString(): String {
            return "ParticipantImageFilesRequest: ${files.size}"
        }
    }

    @JsonClass(generateAdapter = true)
    @TypeLabel("database_file_request")
    data class DatabaseFileRequest(
        val currentFileLength: Long,
        override val headers: ClientMessageHeaders? = null,
    ) : ClientMessage() {
        override fun copyWithHeaders(headers: ClientMessageHeaders?) = copy(headers = headers)
        override fun toString(): String {
            return "DatabaseFileRequest: $currentFileLength"
        }
    }
}

/**
 * Message from server to client
 */
@JsonClass(generateAdapter = true, generator = "sealed:type")
sealed class ServerMessage : MessageBase() {
    abstract override val headers: ServerMessageHeaders?
    fun headersOrThrow() = requireNotNull(headers) { "headers must not be null" }
    abstract fun copyWithHeaders(headers: ServerMessageHeaders?): ServerMessage

    @JsonClass(generateAdapter = true)
    @TypeLabel("login_reply")
    data class LoginReply(
        val status: Status,
        val requiredSiteUuid: String,
        val sessionToken: String?,
        override val headers: ServerMessageHeaders? = null,
    ) : ServerMessage() {
        enum class Status {
            AUTHENTICATED, UNAUTHENTICATED, SITE_MISMATCH;
        }

        override fun copyWithHeaders(headers: ServerMessageHeaders?) = copy(headers = headers)

        override fun toString(): String {
            return "Login reply: $status"
        }

        companion object {
            fun LoginStatus.toLoginReplyStatus() = when (this) {
                LoginStatus.AUTHENTICATED -> Status.AUTHENTICATED
                LoginStatus.UNAUTHENTICATED -> Status.UNAUTHENTICATED
            }
        }
    }

    @JsonClass(generateAdapter = true)
    @TypeLabel("secrets_reply")
    data class SecretsReply(
        val dataFileSecret: EncryptedSecret,
        val databasePassphrase: EncryptedSecret,
        override val headers: ServerMessageHeaders? = null,
    ) : ServerMessage() {
        override fun copyWithHeaders(headers: ServerMessageHeaders?) = copy(headers = headers)
        override fun toString(): String {
            return "SecretsReply"
        }
    }

    @JsonClass(generateAdapter = true)
    @TypeLabel("database_file_transfer_completed_reply")
    data class DatabaseFileTransferCompletedReply(
        val hash: String,
        override val headers: ServerMessageHeaders? = null,
    ) : ServerMessage() {
        override fun copyWithHeaders(headers: ServerMessageHeaders?) = copy(headers = headers)
        override fun toString(): String {
            return "Database file download completed reply: $hash"
        }
    }


    @JsonClass(generateAdapter = true)
    @TypeLabel("database_file_reply")
    data class DatabaseFileReply(
        val content: String,
        /**
         * how much bytes have been downloaded already
         */
        val bytesProgress: Long,
        /**
         * the target file length
         */
        val bytesMax: Long,
        val isAppend: Boolean,
        override val headers: ServerMessageHeaders? = null,
    ) : ServerMessage() {
        override fun copyWithHeaders(headers: ServerMessageHeaders?) = copy(headers = headers)
        override fun toString(): String {
            return "Database File reply:${if (!isAppend) " start" else ""} $bytesProgress/$bytesMax bytes"
        }
    }

    @JsonClass(generateAdapter = true)
    @TypeLabel("database_file_error_reply")
    data class DatabaseFileErrorReply(
        val reason: Reason,
        override val headers: ServerMessageHeaders? = null,
    ) : ServerMessage() {
        enum class Reason { OutOfDiskSpace, IOError, Timeout, Unknown }

        override fun copyWithHeaders(headers: ServerMessageHeaders?) = copy(headers = headers)
        override fun toString(): String {
            return "Couldn't send database file for reason: $reason"
        }
    }

    @JsonClass(generateAdapter = true)
    @TypeLabel("participant_template_files_reply")
    data class ParticipantTemplateFilesReply(
        val files: List<TemplateFileWithContent>,
        override val headers: ServerMessageHeaders? = null,
    ) : ServerMessage() {
        override fun copyWithHeaders(headers: ServerMessageHeaders?) = copy(headers = headers)

        override fun toString(): String {
            return "ParticipantTemplateFilesReply: ${files.size}"
        }
    }


    @JsonClass(generateAdapter = true)
    @TypeLabel("participant_image_files_reply")
    data class ParticipantImageFilesReply(
        val files: List<ImageFileWithContent>,
        override val headers: ServerMessageHeaders? = null,
    ) : ServerMessage() {
        override fun copyWithHeaders(headers: ServerMessageHeaders?) = copy(headers = headers)

        override fun toString(): String {
            return "ParticipantImageFilesReply: ${files.size}"
        }
    }

    @JsonClass(generateAdapter = true)
    @TypeLabel("authentication_required_reply")
    data class AuthenticationRequiredReply(override val headers: ServerMessageHeaders? = null) :
        ServerMessage() {
        override fun copyWithHeaders(headers: ServerMessageHeaders?) = copy(headers = headers)

        override fun toString(): String {
            return "AuthenticationRequiredReply"
        }
    }
}

