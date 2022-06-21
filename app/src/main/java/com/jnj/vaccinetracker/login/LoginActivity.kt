package com.jnj.vaccinetracker.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.hideKeyboard
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.common.ui.SyncBanner
import com.jnj.vaccinetracker.databinding.ActivityLoginBinding
import com.jnj.vaccinetracker.participantflow.ParticipantFlowActivity
import com.jnj.vaccinetracker.settings.SettingsDialog
import com.jnj.vaccinetracker.update.UpdateDialog
import kotlinx.coroutines.flow.onEach

/**
 * @author maartenvangiel
 * @version 1
 */
class LoginActivity : BaseActivity() {

    companion object {
        private const val TAG_SETTINGS_DIALOG = "SettingsDialog"
        private const val TAG_UPDATE_DIALOG = "UpdateDialog"

        fun create(context: Context): Intent {
            return Intent(context, LoginActivity::class.java)
        }
    }

    private val loginActivityMenuHelper by lazy {
        LoginActivityMenuHelper(supportFragmentManager)
    }

    private val viewModel: LoginViewModel by viewModels { viewModelFactory }

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        viewModel.init(true)

        binding.btnLogin.setOnClickListener { login() }
        binding.editPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                login()
                true
            } else {
                false
            }
        }
        binding.root.setOnClickListener { hideKeyboard() }
        binding.btnUpdate.setOnClickListener { showUpdateDialog() }
        observeViewModel(this)
    }

    override val syncBanner: SyncBanner
        get() = binding.syncBanner

    override val isAuthenticatedOperatorScreen: Boolean
        get() = false

    private fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        viewModel.loginCompleted
            .asFlow()
            .onEach {
                onLoginCompleted()
            }
            .launchIn(lifecycleOwner)

        viewModel.prefillUsername.observe(lifecycleOwner) { prefillUsername ->
            if (binding.editUsername.text.isEmpty()) {
                binding.editUsername.setText(prefillUsername)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_settings, menu)
        loginActivityMenuHelper.onCreateOptionsMenu(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (loginActivityMenuHelper.onOptionsItemSelected(item))
            return true
        return when (item.itemId) {
            R.id.action_settings -> {
                showSettingsDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSettingsDialog() {
        SettingsDialog().show(supportFragmentManager, TAG_SETTINGS_DIALOG)
    }

    private fun showUpdateDialog() {
        UpdateDialog().show(supportFragmentManager, TAG_UPDATE_DIALOG)
    }

    private fun login() {
        val username = binding.editUsername.text.toString()
        val password = binding.editPassword.text.toString()
        viewModel.login(username, password)
    }

    private fun onLoginCompleted() {
        startActivity(ParticipantFlowActivity.create(this))
        finish()
    }

}
