package com.jnj.vaccinetracker.setup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.models.NavigationDirection
import com.jnj.vaccinetracker.common.helpers.hideKeyboard
import com.jnj.vaccinetracker.common.helpers.logWarn
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.common.ui.animateNavigationDirection
import com.jnj.vaccinetracker.databinding.ActivitySetupBinding
import com.jnj.vaccinetracker.setup.dialogs.SetupCancelWizardDialog
import com.jnj.vaccinetracker.setup.models.P2pDeviceRole
import com.jnj.vaccinetracker.setup.models.SetupScreen
import com.jnj.vaccinetracker.setup.screens.SetupBackendConfigFragment
import com.jnj.vaccinetracker.setup.screens.SetupIntroFragment
import com.jnj.vaccinetracker.setup.screens.SetupPermissionsFragment
import com.jnj.vaccinetracker.setup.screens.SetupSyncConfigFragment
import com.jnj.vaccinetracker.setup.screens.licenses.SetupLicensesFragment
import com.jnj.vaccinetracker.setup.screens.mainmenu.SetupMainMenuFragment
import com.jnj.vaccinetracker.setup.screens.p2p.device_role.SetupP2pDeviceRoleFragment
import com.jnj.vaccinetracker.setup.screens.p2p.dialogs.ConfirmStopServiceDialog
import com.jnj.vaccinetracker.setup.screens.p2p.transfer.client.SetupP2pDeviceClientTransferFragment
import com.jnj.vaccinetracker.setup.screens.p2p.transfer.server.SetupP2pDeviceServerTransferFragment
import kotlinx.coroutines.flow.onEach

class SetupFlowActivity : BaseActivity(), ConfirmStopServiceDialog.Callback {

    companion object {
        fun create(context: Context): Intent {
            return Intent(context, SetupFlowActivity::class.java)
        }

        private const val TAG_CANCEL_SETUP_DIALOG = "cancelSetupDialog"
        private const val TAG_CONFIRM_BACK_PRESS = "confirmBackPressDialog"
    }

    private val viewModel: SetupFlowViewModel by viewModels { viewModelFactory }
    private lateinit var binding: ActivitySetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { viewModel.restoreInstanceState(it) }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_setup)
        binding.lifecycleOwner = this

        binding.root.setOnClickListener { hideKeyboard() }
        observeViewModel()
        // No title for this setup activity
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun observeViewModel() {
        // Observe changes in the currentScreen set in the viewModel,
        // to navigate to this screen accordingly
        viewModel.currentScreen.observe(this) { screen ->
            navigateToScreen(screen, viewModel.navigationDirection)
        }

        viewModel.showConfirmBackPressDialog.asFlow().onEach {
            ConfirmStopServiceDialog.newInstance(byBackButton = true).show(supportFragmentManager, TAG_CONFIRM_BACK_PRESS)
        }.launchIn(this)

    }

    override val isAuthenticatedOperatorScreen: Boolean
        get() = false

    private fun createP2pTransferFragment(): Fragment {
        return when (viewModel.deviceType.value) {
            P2pDeviceRole.SERVER -> SetupP2pDeviceServerTransferFragment()
            P2pDeviceRole.CLIENT -> SetupP2pDeviceClientTransferFragment()
            null -> error("device role is required to createP2pTransferFragment")
        }
    }

    /**
     * Navigate to the desired screen by loading the corresponding fragment.
     * Animation will be set according to the specified navigationDirection.
     *
     * @param screen                Screen to load
     * @param navigationDirection   Direction in which we move (FORWARD / BACKWARD)
     */
    private fun navigateToScreen(screen: SetupScreen?, navigationDirection: NavigationDirection) {
        val fragment = when (screen) {
            SetupScreen.Intro -> SetupIntroFragment()
            SetupScreen.Backend -> SetupBackendConfigFragment()
            SetupScreen.MainMenu.Item.Site -> SetupSyncConfigFragment()
            SetupScreen.MainMenu.Item.Permissions -> SetupPermissionsFragment()
            SetupScreen.MainMenu.Item.Licenses -> SetupLicensesFragment()
            SetupScreen.MainMenu.Menu -> SetupMainMenuFragment()
            SetupScreen.MainMenu.Item.P2pSync.DeviceTypeChooser -> SetupP2pDeviceRoleFragment()
            SetupScreen.MainMenu.Item.P2pSync.Transfer -> createP2pTransferFragment()
            null -> null
        }

        fragment?.let { newFragment ->
            // Don't load the fragment if already shown
            supportFragmentManager.findFragmentById(R.id.fragment_container)?.let { existingFragment ->
                if (newFragment::class == existingFragment::class) return logWarn("Fragment of this type is already shown, not navigating")
            }

            val transaction = supportFragmentManager.beginTransaction()

            // Set animation based on navigationDirection
            transaction.animateNavigationDirection(navigationDirection)

            transaction
                .replace(R.id.fragment_container, newFragment)
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        viewModel.navigateBack()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_cancel, menu)
        // Show cancel button only if the sync settings were already set
        menu.findItem(R.id.action_cancel).isVisible = viewModel.showCancelButton.get()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_cancel -> {
                runOnUiThread {
                    SetupCancelWizardDialog().show(supportFragmentManager, TAG_CANCEL_SETUP_DIALOG)
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onConfirmStopService(byBackButton: Boolean) {
        if (byBackButton) {
            viewModel.navigateBack(forced = true)
        } else {
            viewModel.confirmStopP2PTransfer()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.saveInstanceState(outState)
    }
}