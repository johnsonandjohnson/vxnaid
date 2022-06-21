package com.jnj.vaccinetracker.participantflow.screens

import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.participantflow.ParticipantFlowViewModel
import com.jnj.vaccinetracker.participantflow.dialogs.ParticipantFlowMandatoryIrisDialog
import kotlinx.coroutines.flow.onEach

/**
 * Abstract base class for IrisScanFragments.
 *
 * @author druelens
 * @version 1
 */
abstract class ParticipantFlowIrisScanFragment : BaseFragment() {

    private companion object {
        private const val TAG_MISSING_MANDATORY_IRIS = "missingMandatoryIrisDialog"
    }

    protected val viewModel: ParticipantFlowIrisScanViewModel by viewModels { viewModelFactory }
    protected val flowViewModel: ParticipantFlowViewModel by activityViewModels { viewModelFactory }

    /**
     * Callback function to show a dialog when the iris scan is mandatory, but no irises were scanned.
     */
    protected fun onNoIrisScanned() {
        ParticipantFlowMandatoryIrisDialog().show(requireActivity().supportFragmentManager, TAG_MISSING_MANDATORY_IRIS)
    }

    protected abstract fun onFailedToConnectScanner()
    protected abstract fun onCapture()

    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        super.observeViewModel(lifecycleOwner)
        viewModel.failedToConnectScannerEvents.asFlow().onEach {
            onFailedToConnectScanner()
        }.launchIn(lifecycleOwner)
    }

}