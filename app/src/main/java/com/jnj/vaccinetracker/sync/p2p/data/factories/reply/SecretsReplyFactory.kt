package com.jnj.vaccinetracker.sync.p2p.data.factories.reply

import com.jnj.vaccinetracker.sync.p2p.common.models.dtos.messages.ServerMessage
import com.jnj.vaccinetracker.sync.p2p.data.encryption.DataFileSecretEncryptionService
import com.jnj.vaccinetracker.sync.p2p.data.encryption.DatabasePassphraseEncryptionService
import javax.inject.Inject

class SecretsReplyFactory @Inject constructor(
    private val databasePassphraseEncryptionService: DatabasePassphraseEncryptionService,
    private val dataFileSecretEncryptionService: DataFileSecretEncryptionService
) {

    suspend fun createSecretsReply(osApiLevel: Int): ServerMessage.SecretsReply {
        return ServerMessage.SecretsReply(
            databasePassphrase = databasePassphraseEncryptionService.encryptDatabasePassphrase(osApiLevel),
            dataFileSecret = dataFileSecretEncryptionService.encryptDataFileSecret(osApiLevel)
        )
    }
}