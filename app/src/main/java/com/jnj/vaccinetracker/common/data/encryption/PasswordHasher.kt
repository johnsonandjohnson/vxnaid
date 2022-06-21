package com.jnj.vaccinetracker.common.data.encryption

import at.favre.lib.crypto.bcrypt.BCrypt
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject


class PasswordHasher @Inject constructor(private val dispatchers: AppCoroutineDispatchers) {

    suspend fun hash(password: String): String = withContext(dispatchers.computation) {
        BCrypt.withDefaults().hashToString(12, password.toCharArray())
    }

    suspend fun verify(password: String, bcryptHash: String): Boolean = withContext(dispatchers.computation) {
        val result: BCrypt.Result = BCrypt.verifyer().verify(password.toCharArray(), bcryptHash)
        result.verified
    }
}