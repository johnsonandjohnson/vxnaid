package com.jnj.vaccinetracker.setup.screens.p2p.transfer.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.ProgressBarContainerBinding
import com.jnj.vaccinetracker.databinding.ThisDeviceContainerBinding
import com.jnj.vaccinetracker.setup.SetupFlowViewModel
import com.jnj.vaccinetracker.sync.p2p.domain.entities.BaseDeterminateProgress

abstract class SetupP2pDeviceTransferFragmentBase : BaseFragment() {
    protected val flowViewModel: SetupFlowViewModel by activityViewModels { viewModelFactory }
    protected abstract val viewModel: SetupP2pDeviceTransferViewModelBase
    protected abstract val progressBarContainer: ProgressBarContainerBinding
    protected abstract val thisDeviceContainer: ThisDeviceContainerBinding
    private fun setFlagKeepScreenOn(enabled: Boolean) {
        val window = requireActivity().window
        val flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        if (enabled) {
            window.addFlags(flag)
        } else {
            window.clearFlags(flag)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val tag = this::class.simpleName
        return FrameLayout(requireContext()).also { it.addView(TextView(context).apply { text = tag }) }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // load view model
        viewModel

        setFlagKeepScreenOn(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setFlagKeepScreenOn(true)
    }

    override fun onDestroy() {
        setFlagKeepScreenOn(false)
        super.onDestroy()
    }

    protected fun onDone() {
        flowViewModel.onP2pTransferCompleted()
    }

    protected fun setProgressNone() {
        progressBarContainer.root.isVisible = false

    }

    protected fun setProgressIndeterminate(message: String) {
        progressBarContainer.progressBar.isIndeterminate = true
        progressBarContainer.progressBarDescription.text = message
        progressBarContainer.root.isVisible = true
        progressBarContainer.progressBar.isVisible = true
    }

    protected fun setProgressDeterminate(progress: BaseDeterminateProgress, message: String) {
        progressBarContainer.progressBar.isIndeterminate = false
        progressBarContainer.progressBarDescription.text = message
        progressBarContainer.progressBar.progress = progress.progress
        progressBarContainer.progressBar.max = Constants.MAX_PERCENT
        progressBarContainer.root.isVisible = true
        progressBarContainer.progressBar.isVisible = true
    }

    protected fun setProgressDescriptionOnly(message: String) {
        progressBarContainer.progressBarDescription.text = message
        progressBarContainer.progressBar.isVisible = false
        progressBarContainer.root.isVisible = true
    }

    private fun renderThisDevice(deviceName: String?) {
        thisDeviceContainer.thisDeviceName.text = deviceName
    }

    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        super.observeViewModel(lifecycleOwner)
        viewModel.deviceName.observe(lifecycleOwner) {
            renderThisDevice(it)
        }
    }
}