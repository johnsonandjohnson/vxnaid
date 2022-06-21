package com.jnj.vaccinetracker.sync.p2p.data.encryption

import com.jnj.vaccinetracker.common.data.repositories.EncryptionKeyRepository
import com.jnj.vaccinetracker.sync.data.repositories.SyncUserCredentialsRepository
import com.jnj.vaccinetracker.sync.p2p.common.models.dtos.EncryptedSecret
import javax.inject.Inject

class DatabasePassphraseEncryptionService @Inject constructor(
    private val syncUserCredentialsRepository: SyncUserCredentialsRepository,
    private val peerPassphraseEncryptionService: PeerPassphraseEncryptionService,
    private val encryptionKeyRepository: EncryptionKeyRepository,
) {

    suspend fun encryptDatabasePassphrase(osApiLevel: Int): EncryptedSecret {
        return peerPassphraseEncryptionService.encrypt(
            syncUserCredentialsRepository.getSyncUserCredentials(),
            osApiLevel,
            encryptionKeyRepository.getOrGenerateDatabasePassphrase()
        )
    }

    suspend fun decryptDatabasePassphrase(databasePassphraseSecret: EncryptedSecret, osApiLevel: Int): String {
        return peerPassphraseEncryptionService.decrypt(
            syncUserCredentialsRepository.getSyncUserCredentials(),
            osApiLevel,
            databasePassphraseSecret
        )
    }
}