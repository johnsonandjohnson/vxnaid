package com.jnj.vaccinetracker.sync.p2p.domain.usecases

import com.jnj.vaccinetracker.common.exceptions.DeleteDatabaseRequiredException
import com.jnj.vaccinetracker.common.helpers.seconds
import com.jnj.vaccinetracker.sync.p2p.common.models.dtos.EncryptedSecret
import com.jnj.vaccinetracker.sync.p2p.data.encryption.DataFileSecretEncryptionService
import com.jnj.vaccinetracker.sync.p2p.data.encryption.DatabasePassphraseEncryptionService
import kotlinx.coroutines.delay
import javax.inject.Inject

class ApplySecretsUseCase @Inject constructor(
    private val databasePassphraseEncryptionService: DatabasePassphraseEncryptionService,
    private val dataFileSecretEncryptionService: DataFileSecretEncryptionService,
    private val swapDataFileSecretUseCase: SwapDataFileSecretUseCase,
    private val swapDatabasePassphraseUseCase: SwapDatabasePassphraseUseCase,
) {
    companion object {
        private const val SWAP_DB_PASS_RETRY_COUNT = 3
        private val SWAP_DB_PASS_RETRY_DELAY = 1.seconds
    }

    suspend fun applySecrets(databasePassphraseEncrypted: EncryptedSecret, dataFileSecretEncrypted: EncryptedSecret, osApiLevel: Int) {
        val databasePassphrase = databasePassphraseEncryptionService.decryptDatabasePassphrase(databasePassphraseEncrypted, osApiLevel)
        val dataFileSecret = dataFileSecretEncryptionService.decryptDataFileSecret(dataFileSecretEncrypted, osApiLevel)
        swapDbPassphrase(databasePassphrase, retryCount = SWAP_DB_PASS_RETRY_COUNT)
        swapDataFileSecretUseCase.swapDataFileSecret(dataFileSecret)
    }

    private suspend fun swapDbPassphrase(databasePassphrase: String, retryCount: Int) {
        try {
            swapDatabasePassphraseUseCase.swapDbPassphrase(databasePassphrase, deleteDatabaseIfRequired = true)
        } catch (ex: DeleteDatabaseRequiredException) {
            if (retryCount > 0) {
                delay(SWAP_DB_PASS_RETRY_DELAY)
                return swapDbPassphrase(databasePassphrase, retryCount - 1)
            } else {
                error("swapDbPassphrase failed after retries")
            }
        }
    }
}