package com.jnj.vaccinetracker.setup.screens.p2p.transfer.client

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
import com.jnj.vaccinetracker.common.di.AppResources
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.databinding.ConnectedDeviceContainerBinding
import com.jnj.vaccinetracker.databinding.FragmentSetupP2pTransferClientBinding
import com.jnj.vaccinetracker.databinding.ProgressBarContainerBinding
import com.jnj.vaccinetracker.databinding.ThisDeviceContainerBinding
import com.jnj.vaccinetracker.setup.screens.p2p.transfer.base.SetupP2pDeviceTransferFragmentBase
import com.jnj.vaccinetracker.setup.screens.p2p.transfer.helpers.throttleProgress
import com.jnj.vaccinetracker.sync.p2p.common.models.CompatibleNsdDevice
import com.jnj.vaccinetracker.sync.p2p.domain.entities.ClientProgress
import kotlinx.coroutines.flow.onEach

class SetupP2pDeviceClientTransferFragment : SetupP2pDeviceTransferFragmentBase() {

    override val viewModel: SetupP2pDeviceClientTransferViewModel by viewModels { viewModelFactory }
    private lateinit var binding: FragmentSetupP2pTransferClientBinding
    override val progressBarContainer: ProgressBarContainerBinding
        get() = binding.progressBarContainer

    override val thisDeviceContainer: ThisDeviceContainerBinding
        get() = binding.thisDeviceContainer

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_setup_p2p_transfer_client, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        // Show back-button again
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.finishButton.setOnClickListener { onDone() }
        binding.startTransferButton.setOnClickListener { viewModel.startDownload() }
        binding.connectedDeviceContainer.reconnectButton.setOnClickListener {
            viewModel.reconnect()
        }
    }


    private val ClientProgress.displayName: String
        get() = displayName(AppResources(requireContext()))

    private fun ClientProgress.render() {
        logInfo("render client progress")
        when (this) {
            is ClientProgress.DownloadingDatabase -> setProgressDeterminate(this, displayName)
            is ClientProgress.DownloadingImages -> setProgressDeterminate(this, displayName)
            ClientProgress.DownloadingSecrets -> setProgressIndeterminate(displayName)
            is ClientProgress.DownloadingTemplates -> setProgressDeterminate(this, displayName)
            ClientProgress.Idle -> setProgressNone()
            ClientProgress.ImportingDatabase -> setProgressIndeterminate(displayName)
            ClientProgress.LoggingIn -> setProgressIndeterminate(displayName)
            ClientProgress.TransferCompletedSuccessfully -> setProgressDescriptionOnly(displayName)
            ClientProgress.ImportingSecrets -> setProgressIndeterminate(displayName)
            ClientProgress.AwaitingDatabaseIdle -> setProgressIndeterminate(displayName)
        }.let { }

        binding.finishButton.isVisible = isCompletedSuccessfully()
        updateTransferButtonState()
    }

    private fun updateTransferButtonState() {
        binding.startTransferButton.isVisible = !viewModel.progress.value.isCompletedSuccessfully()
                && !viewModel.isDownloading.value
                && viewModel.serviceDevice.value != null
    }

    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        super.observeViewModel(lifecycleOwner)

        viewModel.progress
            .throttleProgress()
            .onEach { it.render() }
            .launchIn(lifecycleOwner)
        viewModel.isDownloading.onEach {
            updateTransferButtonState()
        }.launchIn(lifecycleOwner)
        viewModel.serviceDevice.onEach { it.render(binding.connectedDeviceContainer) }
            .launchIn(lifecycleOwner)
    }

    private fun CompatibleNsdDevice?.render(connectedDeviceContainerBinding: ConnectedDeviceContainerBinding) {
        val device = this
        connectedDeviceContainerBinding.apply {
            deviceNotFoundContainer.isVisible = device == null
            deviceNameContainer.isVisible = device != null
            deviceName.text = device?.deviceName
        }
        updateTransferButtonState()
    }
}