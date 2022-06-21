package com.jnj.vaccinetracker.sync.p2p.domain.usecases

import com.jnj.vaccinetracker.common.data.repositories.EncryptionKeyRepository
import com.jnj.vaccinetracker.common.data.repositories.MasterDataRepository
import com.jnj.vaccinetracker.common.helpers.logInfo
import javax.inject.Inject

class SwapDataFileSecretUseCase @Inject constructor(
    private val encryptionKeyRepository: EncryptionKeyRepository,
    private val masterDataRepository: MasterDataRepository
) {

    private suspend fun shouldSwap(byteArray: ByteArray): Boolean {
        val currentKey = encryptionKeyRepository.getOrGenerateAesEncryptionSecretKey()
        return !currentKey.contentEquals(byteArray)
    }

    suspend fun swapDataFileSecret(byteArray: ByteArray) {
        logInfo("swapDataFileSecret")
        if (!shouldSwap(byteArray)) {
            logInfo("data file secret is already up to date")
            return
        } else {
            logInfo("new data file secret, going to swap")
        }
        encryptionKeyRepository.setAesEncryptionSecretKey(byteArray)
        masterDataRepository.deleteAll()
    }
}