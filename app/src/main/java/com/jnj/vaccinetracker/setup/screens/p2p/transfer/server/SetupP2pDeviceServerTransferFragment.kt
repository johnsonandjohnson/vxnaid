package com.jnj.vaccinetracker.setup.screens.p2p.transfer.server

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.databinding.AuthenticatedDeviceContainerBinding
import com.jnj.vaccinetracker.databinding.FragmentSetupP2pTransferServerBinding
import com.jnj.vaccinetracker.databinding.ProgressBarContainerBinding
import com.jnj.vaccinetracker.databinding.ThisDeviceContainerBinding
import com.jnj.vaccinetracker.setup.screens.p2p.dialogs.ConfirmStopServiceDialog
import com.jnj.vaccinetracker.setup.screens.p2p.transfer.base.SetupP2pDeviceTransferFragmentBase
import com.jnj.vaccinetracker.setup.screens.p2p.transfer.helpers.throttleProgress
import com.jnj.vaccinetracker.sync.p2p.common.models.CompatibleNsdDevice
import com.jnj.vaccinetracker.sync.p2p.domain.entities.ServerProgress
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class SetupP2pDeviceServerTransferFragment : SetupP2pDeviceTransferFragmentBase(), ConfirmStopServiceDialog.Callback {
    companion object {
        private const val CONFIRM_STOP_SERVICE_DIALOG = "ConfirmStopServiceDialog"
    }

    override val viewModel: SetupP2pDeviceServerTransferViewModel by viewModels { viewModelFactory }
    private lateinit var binding: FragmentSetupP2pTransferServerBinding

    override val progressBarContainer: ProgressBarContainerBinding
        get() = binding.progressBarContainer

    override val thisDeviceContainer: ThisDeviceContainerBinding
        get() = binding.thisDeviceContainer

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_setup_p2p_transfer_server, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        // Show back-button again
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.finishButton.setOnClickListener {
            viewModel.onStopBroadcastClick()
        }
    }

    val ServerProgress.displayName: String
        get() = when (this) {
            ServerProgress.Authenticating -> "Authenticating"
            ServerProgress.Idle -> ""
            is ServerProgress.UploadingDatabase -> "Uploading Database"
            is ServerProgress.UploadingImages -> "Uploading %d images"
            ServerProgress.UploadingSecrets -> "Uploading secrets"
            is ServerProgress.UploadingTemplates -> "Uploading %d templates"
        }

    private fun ServerProgress.render() {
        logInfo("render server progress")
        when (this) {
            ServerProgress.Authenticating -> setProgressIndeterminate(displayName)
            ServerProgress.Idle -> setProgressNone()
            is ServerProgress.UploadingDatabase -> setProgressDeterminate(this, displayName)
            is ServerProgress.UploadingImages -> setProgressIndeterminate(String.format(displayName, amount))
            ServerProgress.UploadingSecrets -> setProgressIndeterminate(displayName)
            is ServerProgress.UploadingTemplates -> setProgressIndeterminate(String.format(displayName, amount))
        }.let { }
    }

    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        super.observeViewModel(lifecycleOwner)
        viewModel.progress.throttleProgress()
            .onEach { it.render() }
            .launchIn(lifecycleOwner)
        viewModel.nsdSession.map { it?.device }.onEach {
            it.render(binding.authenticatedDeviceContainer)
        }.launchIn(lifecycleOwner)
        viewModel.finishScreenEvent.asFlow().onEach {
            onDone()
        }.launchIn(lifecycleOwner)
        viewModel.showConfirmFinishServicePopupEvent.asFlow().onEach {
            ConfirmStopServiceDialog.newInstance(byBackButton = false).show(childFragmentManager, CONFIRM_STOP_SERVICE_DIALOG)
        }.launchIn(lifecycleOwner)
    }

    private fun CompatibleNsdDevice?.render(authenticatedDeviceContainerBinding: AuthenticatedDeviceContainerBinding) {
        val device = this
        authenticatedDeviceContainerBinding.apply {
            deviceNotAuthenticatedContainer.isVisible = device == null
            deviceNameContainer.isVisible = device != null
            deviceName.text = device?.deviceName
        }
    }

    override fun onConfirmStopService(byBackButton: Boolean) {
        logInfo("onConfirmStopService: $byBackButton")
        viewModel.onConfirmStopService()
    }
}