package com.jnj.vaccinetracker.login

import com.jnj.vaccinetracker.BuildConfig
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.data.managers.LicenseManager
import com.jnj.vaccinetracker.common.data.managers.LoginManager
import com.jnj.vaccinetracker.common.data.managers.UpdateManager
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.exceptions.OperatorAuthenticationException
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.isManualFlavor
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.inject.Inject


/**
 * @author maartenvangiel
 * @author druelens
 * @version 2
 */
class LoginViewModel @Inject constructor(
    private val loginManager: LoginManager,
    private val userRepository: UserRepository,
    private val syncSettingsRepository: SyncSettingsRepository,
    private val licenseManager: LicenseManager,
    private val updateManager: UpdateManager,
    override val dispatchers: AppCoroutineDispatchers,
    private val resourcesWrapper: ResourcesWrapper,
) : ViewModelBase() {

    val loading = mutableLiveBoolean()
    val usernameValidationMessage = mutableLiveData<String>()
    val passwordValidationMessage = mutableLiveData<String>()
    val errorMessage = mutableLiveData<String>()
    val prefillUsername = mutableLiveData<String>()
    val versionNumber = mutableLiveData<String>()
    val deviceName = mutableLiveData<String>()
    val latestVersion = mutableLiveBoolean(true)
    private val prefillBackendUrl = mutableLiveData<String>()

    val loginCompleted = eventFlow<Unit>()

    fun init(isLoginActivity: Boolean) {
        initState()
        if (isLoginActivity) {
            checkVersion()
            getDeviceName()
        }
    }

    private fun initState() {
        userRepository.observeLastUsername()
            .filterNotNull()
            .onEach { username ->
                prefillUsername.set(username)
            }.launchIn(scope)
        syncSettingsRepository.observeBackendUrl()
            .onEach { prefillBackendUrl.set(it) }
            .launchIn(scope)
    }

    private suspend fun doLogin(
        username: String,
        password: String,
    ) {
        try {
            loading.set(true)
            val user = loginManager.login(username, password)
            userRepository.saveUser(user, dateNow())
            loading.set(false)
            loginCompleted.tryEmit(Unit)
        } catch (ex: OperatorAuthenticationException) {
            val stringResource = when (ex.reason) {
                OperatorAuthenticationException.Reason.LocalCredentialsNotFound -> R.string.login_label_error_offline
                OperatorAuthenticationException.Reason.LocalCredentialsPasswordMismatch,
                OperatorAuthenticationException.Reason.RemoteLoginError,
                -> R.string.login_label_error_not_authenticated
                OperatorAuthenticationException.Reason.SyncAdminRole -> R.string.login_label_error_sync_admin_role_not_allowed
                OperatorAuthenticationException.Reason.NotOperatorRole -> R.string.login_label_error_opertor_role_required
            }
            errorMessage.set(resourcesWrapper.getString(stringResource))
            loading.set(false)
        } catch (ex: Throwable) {
            yield()
            ex.rethrowIfFatal()
            logError("Something went wrong while logging in: ", ex)
            errorMessage.set(resourcesWrapper.getString(R.string.login_label_error))
            loading.set(false)
        }
    }

    /**
     * Log in using username and password. backendUrl will be updated if its value is not null (useful for refreshing a login without replacing the backend url)
     */
    fun login(
        username: String,
        password: String,
    ) {
        if (!validateInput(username, password)) return
        scope.launch {
            doLogin(username, password)
        }
    }

    /**
     * Check for the latest version of the application
     * if this app is being manually installed/updated (manual flavor)
     */
    private fun checkVersion() {
        versionNumber.set(BuildConfig.VERSION_NAME)
        if (isManualFlavor) {
            scope.launch {
                try {
                    // On the login screen we always want to call the API to check for update,
                    // so we clear any cache present first
                    updateManager.clearLatestVersionCache()
                    latestVersion.set(updateManager.isLatestVersion())
                } catch (ex: Throwable) {
                    yield()
                    ex.rethrowIfFatal()
                    logError("Something went wrong retrieving the latest version: ", ex)
                }

            }
        }
    }

    /**
     * get the device name from the user repository
     */
    private fun getDeviceName() {
        println("get device name: ${userRepository.getDeviceName()}")
        deviceName.set(userRepository.getDeviceName())
    }

    fun logout() {
        userRepository.logOut()
    }

    private fun validateInput(
        username: String,
        password: String,
    ): Boolean {
        var validated = true
        usernameValidationMessage.set(null)
        passwordValidationMessage.set(null)

        if (username.isEmpty()) {
            validated = false
            usernameValidationMessage.set(resourcesWrapper.getString(R.string.login_label_validation_no_username))
        }

        if (password.isEmpty()) {
            validated = false
            passwordValidationMessage.set(resourcesWrapper.getString(R.string.login_label_validation_no_password))
        }

        return validated
    }
}