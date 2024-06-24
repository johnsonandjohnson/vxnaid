package com.jnj.vaccinetracker.participantflow.screens

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.models.IrisPosition
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentParticipantFlowIntroBinding
import com.jnj.vaccinetracker.databinding.ItemParticipantFlowItemBinding
import com.jnj.vaccinetracker.participantflow.ParticipantFlowViewModel
import com.jnj.vaccinetracker.register.RegisterParticipantFlowActivity
import com.jnj.vaccinetracker.update.UpdateDialog
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach


/**
 * @author maartenvangiel
 * @author tbuehler
 * @author druelens
 * @version 2
 */
class ParticipantFlowIntroFragment : BaseFragment() {

    private companion object {
        private const val TAG_UPDATE_DIALOG = "updateDialog"
    }

    private val viewModel: ParticipantFlowViewModel by activityViewModels { viewModelFactory }
    private val viewModelParticipantFlow: ParticipantFlowMatchingViewModel by viewModels { viewModelFactory }

    private lateinit var binding: FragmentParticipantFlowIntroBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_participant_flow_intro, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.btnContinue.setOnClickListener {
            viewModel.confirmIntro()
        }
        binding.btnNewParticipant.setOnClickListener {
            viewModelParticipantFlow.onNewParticipantButtonClick()
        }

        // Add dynamic content
        populateWorkflowSteps(inflater)

        setHasOptionsMenu(true)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)

        return binding.root
    }

    @OptIn(FlowPreview::class)
    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        logInfo("observeViewModel")
        viewModel.updateAvailableEvents
            .asFlow()
            .debounce(100)
            .onEach {
                onNewVersionAvailable()
            }.launchIn(lifecycleOwner)
        viewModelParticipantFlow.launchRegistrationFlowEvents.asFlow().onEach {
            startActivityForResult(
                RegisterParticipantFlowActivity.create(
                    context = requireContext(),
                    participantId = null,
                    isManualEnteredParticipantId = false,
                    irisScannedLeft = false,
                    irisScannedRight = false,
                    countryCode = null,
                    phoneNumber = null
                ), Constants.REQ_REGISTER_PARTICIPANT
            )
            (requireActivity() as BaseActivity).setForwardAnimation()
        }.launchIn(lifecycleOwner)
    }

    private fun onNewVersionAvailable() {
        // Launch new dialog if none visible before
        if (requireActivity().supportFragmentManager.findFragmentByTag(TAG_UPDATE_DIALOG) == null)
            UpdateDialog().show(requireActivity().supportFragmentManager, TAG_UPDATE_DIALOG)
    }

    /**
     * Adds the participant workflow steps in the correct order according to the configuration.
     * The matching and visit step are always added at the end.
     */
    private fun populateWorkflowSteps(inflater: LayoutInflater) {
        var index = 1

        // Add dynamic identification steps
        viewModel.workflowItems.forEach { step ->
            val view = DataBindingUtil.inflate<ItemParticipantFlowItemBinding>(inflater, R.layout.item_participant_flow_item, binding.authStepsContainer, true)
            when (step) {
                ParticipantFlowViewModel.WorkflowItem.ID_CARD -> {
                    view.label = this.getString(R.string.match_or_register_patient_step_id_card)
                    view.stepIndex = this.getString(R.string.match_or_register_patient_step_index, index.toString())
                    view.imgId.setImageResource(R.drawable.ic_id_card)
                }
                ParticipantFlowViewModel.WorkflowItem.PHONE -> {
                    view.label = this.getString(R.string.match_or_register_patient_step_phone)
                    view.stepIndex = this.getString(R.string.match_or_register_patient_step_index, index.toString())
                    view.imgId.setImageResource(R.drawable.ic_baseline_phone)
                }
                ParticipantFlowViewModel.WorkflowItem.IRIS_SCAN -> {
                    view.label = this.getString(R.string.match_or_register_patient_step_iris_scan)
                    view.stepIndex = this.getString(R.string.match_or_register_patient_step_index, index.toString())
                    view.imgId.setImageResource(R.drawable.ic_eyeglasses_right)
                    view.imgId.updateLayoutParams {
                        width = 153.toPx()
                        height = 153.toPx()
                    }
                }
                ParticipantFlowViewModel.WorkflowItem.MATCHING -> {
                    view.label = this.getString(R.string.match_or_register_patient_step_identify)
                    view.stepIndex = this.getString(R.string.match_or_register_patient_step_index, index.toString())
                    view.imgId.setImageResource(R.drawable.ic_account)
                    view.imgId.updateLayoutParams {
                        width = 120.toPx()
                        height = 120.toPx()
                    }
                }
                ParticipantFlowViewModel.WorkflowItem.VISIT -> {
                    view.label = this.getString(R.string.match_or_register_patient_step_visit)
                    view.stepIndex = this.getString(R.string.match_or_register_patient_step_index, index.toString())
                    view.imgId.setImageResource(R.mipmap.ic_launcher_foreground)
                    view.imgId.updateLayoutParams {
                        width = 140.toPx()
                        height = 140.toPx()
                    }
                }

            }
            index++

            // Add a divider if we haven't reached the last element yet
            if (index <= viewModel.workflowItems.size) {
                inflater.inflate(R.layout.item_participant_flow_divider, binding.authStepsContainer, true)
            }
        }

    }

    private fun Int.toPx() = this * resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT

    /**
     * We don't want to show the cancel workflow button on the home page.
     */
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.action_logout).isVisible = true
        menu.findItem(R.id.action_cancel).isVisible = false
    }

}