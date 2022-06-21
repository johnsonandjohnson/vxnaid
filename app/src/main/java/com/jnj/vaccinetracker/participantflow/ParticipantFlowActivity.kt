package com.jnj.vaccinetracker.participantflow

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.google.android.material.snackbar.Snackbar
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.models.NavigationDirection
import com.jnj.vaccinetracker.common.helpers.logWarn
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.common.ui.SyncBanner
import com.jnj.vaccinetracker.common.ui.animateNavigationDirection
import com.jnj.vaccinetracker.databinding.ActivityParticipantFlowBinding
import com.jnj.vaccinetracker.participantflow.screens.*

/**
 * @author maartenvangiel
 * @version 1
 */
class ParticipantFlowActivity : BaseActivity() {

    companion object {
        fun create(context: Context): Intent {
            return Intent(context, ParticipantFlowActivity::class.java)
        }
    }

    private val viewModel: ParticipantFlowViewModel by viewModels { viewModelFactory }

    private lateinit var binding: ActivityParticipantFlowBinding
    private var errorSnackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { viewModel.restoreInstanceState(it) }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_participant_flow)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.currentScreen.observe(this) { screen ->
            navigateToScreen(screen, viewModel.navigationDirection)
        }
        viewModel.errorMessage.observe(this) { errorMessage ->
            errorSnackbar?.dismiss()

            if (errorMessage == null) {
                return@observe
            }

            errorSnackbar = Snackbar
                .make(binding.root, errorMessage, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.general_label_retry) {
                    errorSnackbar?.dismiss()
                    viewModel.onRetryClick()
                }.also { it.show() }

        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.saveInstanceState(outState)
    }

    private fun navigateToScreen(screen: ParticipantFlowViewModel.Screen?, navigationDirection: NavigationDirection) {
        val fragment = when (screen) {
            ParticipantFlowViewModel.Screen.INTRO -> ParticipantFlowIntroFragment()
            ParticipantFlowViewModel.Screen.PARTICIPANT_ID -> ParticipantFlowParticipantIdFragment()
            ParticipantFlowViewModel.Screen.PHONE -> ParticipantFlowPhoneNumberFragment()
            ParticipantFlowViewModel.Screen.IRIS_SCAN_LEFT_EYE -> ParticipantFlowIrisScanLeftFragment()
            ParticipantFlowViewModel.Screen.IRIS_SCAN_RIGHT_EYE -> ParticipantFlowIrisScanRightFragment()
            ParticipantFlowViewModel.Screen.PARTICIPANT_MATCHING -> ParticipantFlowMatchingFragment()
            else -> null
        }
        screen?.let { title = getString(it.title) }

        fragment?.let { newFragment ->
            supportFragmentManager.findFragmentById(R.id.fragment_container)?.let { existingFragment ->
                if (newFragment::class == existingFragment::class) return logWarn("Fragment of this type is already shown, not navigating")
            }

            val transaction = supportFragmentManager.beginTransaction()

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
        if (!viewModel.navigateBack()) {
            logOut()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_logout -> {
                logOut()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override val syncBanner: SyncBanner
        get() = binding.syncBanner

}