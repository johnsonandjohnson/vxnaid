package com.jnj.vaccinetracker.common.exceptions

import com.jnj.vaccinetracker.common.data.models.LicenseType
import com.jnj.vaccinetracker.common.domain.entities.LicenseObtainedStatus
import com.jnj.vaccinetracker.sync.data.models.SyncStatus
import java.io.IOException

abstract class AppException : Exception()

class SiteNotFoundException(val uuid: String) : AppException() {
    override val message: String
        get() = "No site found with uuid $uuid"
}

class AddressNotFoundException(val country: String) : AppException() {
    override val message: String
        get() = "No address fields for country $country"
}

class NoSiteUuidAvailableException(override val message: String = "No site UUID set") : AppException()

sealed class AppIoException : IOException() {

}

class InvalidSessionException : AppIoException()

class MatchNotFoundException : AppIoException()

class ParticipantAlreadyExistsException(override val message: String? = null) : AppIoException()

class ParticipantUuidAlreadyExistsException : AppIoException()

/**
 * thrown by backend when we upload exact replica of already present template
 */
class TemplateAlreadyExistsException : AppIoException()
class TemplateInvalidException : AppIoException()
class ParticipantNotFoundException : AppIoException()

abstract class DatabaseException : AppException()

class InsertEntityException(message: String, override val cause: Throwable? = null, orReplace: Boolean) : DatabaseException() {
    private val _message = "$message [orReplace=$orReplace]"
    override val message: String
        get() = _message
}

class UpdateDraftStateException(override val message: String, override val cause: Throwable? = null) : DatabaseException()

class NoNetworkException : AppException()

class GetMasterDataRemoteException(override val message: String, override val cause: Throwable?) : AppIoException()
class MapMasterDataDomainException(override val message: String, override val cause: Throwable?) : AppException()
class StoreMasterDataException(override val message: String, override val cause: Throwable?) : AppIoException()

sealed class BiometricsException : AppException()

class EnrollBiometricsTemplateFailed(override val message: String?, override val cause: Throwable? = null) : BiometricsException()
class IdentifyBiometricsTemplateFailed(override val cause: Throwable? = null) : BiometricsException()

class LicensesNotObtainedException(override val message: String, override val cause: Throwable? = null, private val licenseObtainedStatus: LicenseObtainedStatus?) :
    AppException() {
    init {
        require(licenseObtainedStatus != LicenseObtainedStatus.OBTAINED) { "LicensesNotObtainedException thrown when license is obtained" }
    }

    val isObtainableAfterForceClose get() = licenseObtainedStatus == LicenseObtainedStatus.OBTAINABLE_AFTER_FORCE_CLOSE
}

class WebCallException(msg: String, override val cause: Throwable?, val code: Int?, val errorBody: String?) : AppIoException() {
    override val message: String = "$msg [code:$code, errorBody:$errorBody]"
}

class SyncUserCredentialsNotAvailableException(override val message: String, override val cause: Throwable? = null) : AppIoException()

/**
 * the configuration contains a different syncScope than the cached one
 */
class SyncScopeChangedException : AppException()

class SyncScopeEntityNotFoundException : DatabaseException()

class ReplaceSyncScopeException(override val message: String, override val cause: Throwable? = null) : AppException()

class OperatorUuidNotAvailableException(override val message: String = "Operator Uuid is not available in preferences") : AppException()

class OperatorAuthenticationException(override val message: String? = null, override val cause: Throwable? = null, val reason: Reason) : AppException() {
    enum class Reason {
        /**
         * we are offline and stored operator credentials with specified username not found
         */
        LocalCredentialsNotFound,

        /**
         * we found stored user credentials based on username but the password is not equal
         */
        LocalCredentialsPasswordMismatch,

        /**
         * we tried logging in using the api, but it failed for any reason
         */
        RemoteLoginError,

        /**
         * sync admin logged into operator screen
         */
        SyncAdminRole,

        /**
         * user without operator role logged in
         */
        NotOperatorRole,
    }
}

class SyncAdminAuthenticationException(override val message: String? = null, override val cause: Throwable? = null, val reason: Reason) : AppException() {
    enum class Reason {
        /**
         * [cause] is not null with this reason given
         */
        RemoteLoginError,
        InvalidCredentials,
        NotSyncAdminRole,
        OperatorRole,
    }
}

class FileCollisionException(override val message: String) : AppIoException()

class StoreSyncErrorException(override val message: String, override val cause: Throwable? = null) : AppException()

class EncryptionException(override val message: String, override val cause: Throwable? = null) : AppIoException()


class TotalSyncScopeRecordCountMismatchException(
    override val message: String, val backendTableCount: Long,
) : AppException()

class ReportSyncCompletedDateException(override val message: String, override val cause: Throwable? = null) : AppException()

class FailedToDownloadAnySyncRecordsInPageException : AppException()

class SyncResponseValidationException(override val cause: Throwable, val syncStatus: SyncStatus) : AppException()

/**
 * thrown when licenses must be activated manually in the settings dialog
 */
class ManualLicenseActivationRequiredException(val licenseType: LicenseType) : AppException() {
    override val message: String
        get() = "ManualLicenseActivationRequiredException: $licenseType"
}

/**
 * thrown when backend returns HTTP status 409
 */
class DuplicateRequestException : AppIoException()

/**
 * thrown when backend returns HTTP status 503
 */
class ServerUnavailableException : AppIoException()

class DeviceNameNotAvailableException : AppException()

class HostNotAvailableException : AppException()
class PortNotAvailableException : AppException()

class DeleteDatabaseRequiredException(val dbName: String) : AppException() {
    override fun toString(): String {
        return "DeleteDatabaseRequiredException: $dbName"
    }
}