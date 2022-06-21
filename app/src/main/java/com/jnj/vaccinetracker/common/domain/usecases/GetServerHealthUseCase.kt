package com.jnj.vaccinetracker.common.domain.usecases

import com.jnj.vaccinetracker.common.exceptions.InvalidSessionException
import com.jnj.vaccinetracker.common.exceptions.ServerUnavailableException
import com.jnj.vaccinetracker.common.helpers.logDebug
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiService
import java.net.SocketTimeoutException
import javax.inject.Inject

class GetServerHealthUseCase @Inject constructor(private val apiService: VaccineTrackerSyncApiService) {

    /**
     * @return httpStatusCode
     */
    suspend fun getHealth(): Result<Int> {
        logDebug("getHealth")
        return kotlin.runCatching {
            try {
                apiService.getHealth().code()
            } catch (ex: InvalidSessionException) {
                403
            } catch (ex: ServerUnavailableException) {
                503
            } catch (ex: SocketTimeoutException) {
                500
            }
        }

    }

}