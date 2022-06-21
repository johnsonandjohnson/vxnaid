package com.jnj.vaccinetracker.common.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.managers.IrisScannerConnectionManager
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.ui.dialog.SyncErrorDialog
import com.jnj.vaccinetracker.login.LoginActivity
import com.jnj.vaccinetracker.login.RefreshSessionDialog
import com.jnj.vaccinetracker.participantflow.dialogs.ParticipantFlowCancelWorkflowDialog
import com.jnj.vaccinetracker.sync.presentation.SyncAndroidService
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * @author maartenvangiel
 * @version 1
 */
abstract class BaseActivity :
    DaggerAppCompatActivity(), ResourcesWrapper, UiFlowExt {

    private companion object {
        private const val TAG_LOGIN_DIALOG = "loginDialog"
        private const val TAG_SYNC_ERROR_DIALOG = "syncErrorDialog"
    }

    @Inject
    lateinit var irisScannerConnectionManager: IrisScannerConnectionManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val baseActivityViewModel: BaseActivityViewModel by viewModels { viewModelFactory }

    protected open val syncBanner: SyncBanner? = null

    protected inline val resourcesWrapper: ResourcesWrapper
        get() = this

    @OptIn(FlowPreview::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeViewModel()
    }

    private fun observeViewModel() {
        baseActivityViewModel.shouldShowSessionRefreshDialog.onEach { shouldShowSessionRefreshDialog ->
            logInfo("onEach shouldShowSessionRefreshDialog=$shouldShowSessionRefreshDialog isAuthenticatedOperatorScreen=$isAuthenticatedOperatorScreen")
            if (isAuthenticatedOperatorScreen) {
                // we only care if it's an authenticated operator screen
                if (shouldShowSessionRefreshDialog) {
                    onOperatorSessionExpired()
                } else {
                    onOperatorSessionRefreshed()
                }
            }
        }.launchIn(this)

        baseActivityViewModel.syncState.onEach { syncState ->
            logInfo("onSyncStateChanged: $syncState ${syncBanner != null}")
            syncBanner?.state = syncState
        }.launchIn(this)

        baseActivityViewModel.openLoginScreenEvent.asFlow().onEach {
            openLoginScreen()
        }.launchIn(this)
    }

    private fun openLoginScreen() {
        startActivity(LoginActivity.create(this))
        finishAffinity()
        setBackAnimation()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        initSyncBanner()
    }

    protected open fun initSyncBanner() {
        syncBanner?.apply {
            onErrorClick = ::onSyncBannerErrorClick
        } ?: run {
            logError("initSyncBanner: sync banner is null")
        }

    }

    private fun onSyncBannerErrorClick() {
        logInfo("onSyncBannerErrorClick")
        lifecycleScope.launchWhenStarted {
            SyncErrorDialog.create().show(supportFragmentManager, TAG_SYNC_ERROR_DIALOG)
        }
    }

    /**
     * Is this an activity to be navigated to by operators and where they must be logged in
     */
    protected open val isAuthenticatedOperatorScreen get() = true

    private fun onOperatorSessionExpired() {
        logInfo("onOperatorSessionExpired")
        // Launch new dialog if none visible before
        if (supportFragmentManager.findFragmentByTag(TAG_LOGIN_DIALOG) == null)
            RefreshSessionDialog().show(supportFragmentManager, TAG_LOGIN_DIALOG)
    }

    private fun onOperatorSessionRefreshed() {
        logInfo("onOperatorSessionRefreshed")
        // close refreshsessiondialog if there is one active
        val refreshSessionDialogFragment = supportFragmentManager.findFragmentByTag(TAG_LOGIN_DIALOG)
        if (refreshSessionDialogFragment != null)
            supportFragmentManager.commit(allowStateLoss = true) { remove(refreshSessionDialogFragment) }
    }


    override fun onResume() {
        super.onResume()
        checkIrisScannerRecentlyAttached()
        SyncAndroidService.start(this)
    }

    override fun getInt(resId: Int): Int {
        return resources.getInteger(resId)
    }

    protected fun logOut() {
        baseActivityViewModel.logOut()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_cancel, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_cancel -> {
                runOnUiThread {
                    ParticipantFlowCancelWorkflowDialog().show(supportFragmentManager, TAG_LOGIN_DIALOG)
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    /**
     * Check if the iris scanner was connected
     */
    private fun checkIrisScannerRecentlyAttached() {
        if (irisScannerConnectionManager.hasRecentlyConnectedDevice()) {
            Snackbar.make(
                findViewById(android.R.id.content),
                R.string.iris_scan_prompt_scanner_connected,
                Snackbar.LENGTH_SHORT
            ).show()
            irisScannerConnectionManager.resetRecentlyConnectedDevice()
        } else {
            logInfo("No new USB device")
        }
    }

    /**
     * Finishes this activity.
     * Shows a scroll forward animation.
     */
    override fun finish() {
        super.finish()
        setForwardAnimation()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        setBackAnimation()
    }

    fun setBackAnimation() {
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    fun setForwardAnimation() {
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

}
