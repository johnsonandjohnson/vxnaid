package com.jnj.vaccinetracker.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.createLogFileShareIntent
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogSettingsBinding
import com.jnj.vaccinetracker.setup.SetupFlowActivity
import kotlinx.coroutines.flow.onEach


/**
 * @author maartenvangiel
 * @author druelens
 * @version 2
 */
class SettingsDialog : BaseDialogFragment() {

    private val viewModel: SettingsViewModel by viewModels { viewModelFactory }
    private lateinit var binding: DialogSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        isCancelable = false
    }

    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        viewModel.rerunSetupWizardEvent.asFlow()
            .onEach {
                startActivity(SetupFlowActivity.create(requireContext()))
                requireActivity().finishAffinity()
            }
            .launchIn(lifecycleOwner)

        viewModel.shareLogsEvents.asFlow()
            .onEach { uri ->
                logInfo("share log event: $uri")
                val intent = createLogFileShareIntent(uri)
                startActivity(intent)
            }.launchIn(lifecycleOwner)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_settings, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnClose.setOnClickListener {
            dismissAllowingStateLoss()
        }
        binding.btnCopyDeviceId.setOnClickListener {
            viewModel.copyDeviceIdToClipboard()
            Snackbar.make(binding.root, R.string.settings_msg_copied_deviceID, Snackbar.LENGTH_SHORT)
                .also { it.show() }
        }
        binding.btnRerunSetupWizard.setOnClickListener {
            viewModel.rerunSetupWizard()

        }
        binding.btnSyncNow.setOnClickListener {
            viewModel.syncNow()
        }
        binding.btnShareLogs.setOnClickListener {
            viewModel.onShareLogsClick()
        }
        binding.btnClearLogs.setOnClickListener {
            viewModel.onClearLogsClick()
        }

    }

}