package com.jnj.vaccinetracker.participantflow.screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.barcode.ScanBarcodeActivity
import com.jnj.vaccinetracker.barcode.formatParticipantId
import com.jnj.vaccinetracker.common.helpers.setTextKeepSelection
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentParticipantFlowParticipantIdBinding
import com.jnj.vaccinetracker.participantflow.ParticipantFlowViewModel

/**
 * @author maartenvangiel
 * @version 1
 */
class ParticipantFlowParticipantIdFragment : BaseFragment() {

    private companion object {
        private const val REQ_BARCODE = 213
    }

    private val viewModel: ParticipantFlowParticipantIdViewModel by viewModels { viewModelFactory }
    private val flowViewModel: ParticipantFlowViewModel by activityViewModels { viewModelFactory }

    private lateinit var binding: FragmentParticipantFlowParticipantIdBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_participant_flow_participant_id, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        // If ID_CARD is in the workflowItems, this fragment can be skipped only if not mandatory
        viewModel.canSkip.set(!flowViewModel.workflowItems.first { it == ParticipantFlowViewModel.WorkflowItem.ID_CARD }.mandatory)

        binding.editIdNumber.doAfterTextChanged {
            val text = binding.editIdNumber.text.toString()
            viewModel.validateInput(text)
            val formattedText = formatParticipantId(text)
            if (formattedText != text) {
                binding.editIdNumber.setTextKeepSelection(formattedText)
            }
        }
        binding.btnScanCode.setOnClickListener {
            startActivityForResult(ScanBarcodeActivity.create(requireContext(),ScanBarcodeActivity.PARTICIPANT), REQ_BARCODE)
        }
        binding.btnSkip.setOnClickListener {
            flowViewModel.confirmParticipantId(null, manual = false)
        }
        binding.btnSubmit.setOnClickListener {
            val participantId = if (viewModel.canSubmit.get()) binding.editIdNumber.text.toString() else null
            flowViewModel.confirmParticipantId(participantId, manual = true)
        }
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        return binding.root
    }


    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        flowViewModel.participantId.observe(lifecycleOwner) { participantId ->
            if (binding.editIdNumber.text.isEmpty()) {
                binding.editIdNumber.setText(participantId)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_BARCODE && resultCode == Activity.RESULT_OK) {
            val participantIdBarcode = data?.getStringExtra(ScanBarcodeActivity.EXTRA_BARCODE) ?: return
            if (isVisible) {
                binding.editIdNumber.setText(participantIdBarcode)
            }
            flowViewModel.confirmParticipantId(participantIdBarcode, manual = false)
        }
    }

}