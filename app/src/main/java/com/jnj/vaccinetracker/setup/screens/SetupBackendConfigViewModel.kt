package com.jnj.vaccinetracker.setup.screens

import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.domain.usecases.SyncAdminLoginUseCase
import com.jnj.vaccinetracker.common.exceptions.InvalidSessionException
import com.jnj.vaccinetracker.common.exceptions.NoNetworkException
import com.jnj.vaccinetracker.common.exceptions.SyncAdminAuthenticationException
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.sync.data.models.SyncUserCredentials
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

class SetupBackendConfigViewModel @Inject constructor(
    override val dispatchers: AppCoroutineDispatchers,
    private val syncSettingsRepository: SyncSettingsRepository,
    private val syncAdminLoginUseCase: SyncAdminLoginUseCase,
    private val resourcesWrapper: ResourcesWrapper,
) : ViewModelBase() {

    val backendUrlValidationMessage = mutableLiveData<String>()
    val usernameValidationMessage = mutableLiveData<String>()
    val passwordValidationMessage = mutableLiveData<String>()
    val credentialsValidationMessage = mutableLiveData<String>()

    val backendSettingsCompleted = eventFlow<Unit>()

    val loading = mutableLiveBoolean()
    val backendUrl = mutableLiveData<String>()

    val isSavingBackendSettings = mutableLiveBoolean()

    init {
        initState()
    }

    private fun initState() {
        // Set backend URL from repository if previously entered
        syncSettingsRepository.observeBackendUrl().onEach { url ->
            backendUrl.set(url)
        }.launchIn(scope)
    }

    /**
     * Try to store the settings entered in this fragment.
     * Will first try to store the backend URL, and if successful, will try to store the sync credentials.
     */
    fun saveBackendSettings(backendUrl: String, username: String, password: String) {
        if (isSavingBackendSettings.value)
            return
        scope.launch {
            isSavingBackendSettings.value = true
            try {
                if (saveBackendUrl(backendUrl))
                    saveSyncCredentials(username, password)
            } finally {
                isSavingBackendSettings.value = false
            }
        }

    }

    /**
     * Save the backend URL in the repository if it passed validation.
     */
    private fun saveBackendUrl(backendUrl: String): Boolean {
        if (!validateBackendUrlInput(backendUrl)) return false
        syncSettingsRepository.saveBackendUrl(backendUrl)
        return true
    }

    /**
     * Check the format of the entered backend URL.
     * Needs to start with 'http(s)://'
     *
     * @return True if valid format for backend URL, false otherwise.
     */
    private fun validateBackendUrlInput(backendUrl: String?): Boolean {
        var validated = true
        backendUrlValidationMessage.set(null)

        if (backendUrl.isNullOrEmpty() || !(backendUrl.startsWith("http://") || (backendUrl.startsWith("https://")))) {
            validated = false
            backendUrlValidationMessage.set(resourcesWrapper.getString(R.string.login_label_validation_no_backend_url))
        }

        return validated
    }

    /**
     * Save the sync credentials if they pass validation.
     * Emit an event if successfully saved.
     */
    private fun saveSyncCredentials(username: String, password: String) {
        scope.launch {
            val validated = validateSyncCredentials(username, password)
            if (validated) {
                backendSettingsCompleted.tryEmit(Unit)
            }
        }
    }

    private fun onInvalidCredentials() {
        credentialsValidationMessage.set(resourcesWrapper.getString(R.string.login_label_error_not_authenticated))
    }

    private fun onInvalidBackend() {
        backendUrlValidationMessage.set(resourcesWrapper.getString(R.string.login_label_validation_no_backend_url))
    }

    /**
     * Check if sync credentials are entered and whether they can be used to successfully authenticate.
     * Also checks if the backendURL is reachable and has the login endpoint.
     *
     * @return True if validation passed, false otherwise.
     */
    private suspend fun validateSyncCredentials(username: String?, password: String?): Boolean {
        var validated = true
        credentialsValidationMessage.set(null)

        // Check if username and password are entered
        if (username.isNullOrEmpty()) {
            usernameValidationMessage.set(resourcesWrapper.getString(R.string.login_label_validation_no_username))
        }
        if (password.isNullOrEmpty()) {
            passwordValidationMessage.set(resourcesWrapper.getString(R.string.login_label_validation_no_password))
        }
        if (!username.isNullOrEmpty() && !password.isNullOrEmpty()) {
            // Try to login using the specified credentials
            loading.set(true)
            try {
                syncAdminLoginUseCase.login(SyncUserCredentials(username, password))
            } catch (ex: SyncAdminAuthenticationException) {
                // No network available
                validated = false
                when (ex.reason) {
                    SyncAdminAuthenticationException.Reason.RemoteLoginError -> {
                        when (ex.cause) {
                            is IOException -> {
                                // HTTP client was not able to connect to the backend URL, likely entered incorrectly
                                onInvalidBackend()
                            }
                            is NoNetworkException -> {
                                backendUrlValidationMessage.set(resourcesWrapper.getString(R.string.setup_error_no_network))
                            }
                            is InvalidSessionException -> onInvalidCredentials()
                            else -> onInvalidBackend()
                        }
                    }
                    SyncAdminAuthenticationException.Reason.InvalidCredentials -> onInvalidCredentials()
                    SyncAdminAuthenticationException.Reason.NotSyncAdminRole -> {
                        credentialsValidationMessage.set(resourcesWrapper.getString(R.string.login_label_error_sync_admin_role_required))
                    }
                    SyncAdminAuthenticationException.Reason.OperatorRole -> {
                        credentialsValidationMessage.set(resourcesWrapper.getString(R.string.login_label_error_opertor_role_not_allowed))
                    }
                }.let {}
            } finally {
                loading.set(false)
            }
        } else {
            validated = false
        }
        return validated
    }
}