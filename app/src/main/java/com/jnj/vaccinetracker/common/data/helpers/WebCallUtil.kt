package com.jnj.vaccinetracker.common.data.helpers

import android.system.ErrnoException
import com.jnj.vaccinetracker.common.data.network.VaccineTrackerApiService
import com.jnj.vaccinetracker.common.exceptions.InvalidSessionException
import com.jnj.vaccinetracker.common.exceptions.ServerUnavailableException
import com.jnj.vaccinetracker.common.exceptions.WebCallException
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.config.Counters
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.net.ssl.SSLException

@Suppress("UNUSED_PARAMETER")
class WebCallUtil @Inject constructor(
    private val networkConnectivity: NetworkConnectivity,
    private val dispatchers: AppCoroutineDispatchers,
    private val serverHealthMeter: ServerHealthMeter,
) {

    companion object {
        private val counterSync = Counters.SyncWebCall
    }

    /**
     * for [VaccineTrackerApiService] calls
     */
    suspend fun <T> webCall(requireAuthentication: Boolean = true, callName: String = "", block: suspend () -> T): T {
        beforeWebCall()
        try {
            return executeWebCall(callName = callName, block)
        } catch (ex: WebCallException) {
            when (ex.cause) {
                is ServerUnavailableException -> {
                    onServerUnavailableException()
                    throw ex
                }
                else -> throw ex
            }
        }
    }

    private suspend fun <T> executeWebCall(callName: String = "", block: suspend () -> T): T {
        try {
            logInfo("executeWebCall: $callName")
            return block()
        } catch (ex: InvalidSessionException) {
            throw ex
        } catch (ex: Throwable) {
            yield()
            ex.rethrowIfFatal()
            throw createWebCallException("Something went wrong during webCall '$callName'", ex)
        }
    }

    private suspend fun createWebCallException(message: String, stackTrace: Throwable): WebCallException = withContext(dispatchers.io) {
        val httpException = stackTrace.findHttpException()
        val code = httpException?.code()
        val errorBody: String? = httpException?.readErrorBody()
        WebCallException(message, stackTrace, code, errorBody)
    }

    private suspend fun beforeWebCall() {
        networkConnectivity.requireFastInternet()
    }

    private suspend fun onServerUnavailableException() {
        serverHealthMeter.startMeasuring()
        networkConnectivity.requireFastInternet()
    }

    /**
     * for [VaccineTrackerSyncApiService] calls
     */
    suspend fun <T> webCallSync(
        requireAuthentication: Boolean = true,
        callName: String = "",
        refreshSession: suspend () -> Unit,
        retryCount: Int = counterSync.SYNC_API_RETRY_COUNT,
        block: suspend () -> T,
    ): T {
        require(retryCount >= 0)
        beforeWebCall()
        return try {
            executeWebCall(callName = callName, block)
        } catch (ex: InvalidSessionException) {
            if (requireAuthentication) {
                logInfo("webCallSync: got invalid session, going to refresh session")
                refreshSession()
            } else
                throw createWebCallException("invalid session exception during unauthenticated call: '$callName'", ex)
            webCallSync(requireAuthentication, callName, refreshSession, retryCount, block)
        } catch (ex: WebCallException) {
            when (ex.cause) {
                is ServerUnavailableException -> {
                    onServerUnavailableException()
                    throw ex
                }
                /**
                 * [UnknownHostException] when performing a REST call right after disabling flight mode.
                 */
                is SocketTimeoutException, is UnknownHostException, is ConnectException, is ErrnoException, is SSLException -> {

                    //Turning flight mode off will give us the network connected signal too early.
                    if (retryCount == 0) {
                        if (ex.cause is SocketTimeoutException)
                            onServerUnavailableException()
                        throw ex
                    } else {
                        delay(counterSync.RETRY_DELAY)
                        webCallSync(requireAuthentication, callName, refreshSession, retryCount - 1, block)
                    }
                }
                else -> throw ex
            }
        }
    }

    private fun Throwable?.findHttpException(): HttpException? = if (this == null) null else this as? HttpException ?: cause.findHttpException()

    private fun HttpException.readErrorBody(): String? = getErrorBody(this)

    @Suppress("UnnecessaryVariable")
    private fun getErrorBody(e: HttpException): String? {
        return try {
            e.response()?.errorBody()?.string()
        } catch (ex: Exception) {
            logError("failed to read error body", ex)
            null
        }
    }
}