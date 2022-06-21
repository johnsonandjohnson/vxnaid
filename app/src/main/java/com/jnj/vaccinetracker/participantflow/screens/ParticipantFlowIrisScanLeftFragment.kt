package com.jnj.vaccinetracker.participantflow.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.models.IrisPosition
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.databinding.FragmentParticipantFlowIrisScanLeftBinding
import com.neurotec.biometrics.view.NIrisView

/**
 * Fragment for the scanning of the left iris.
 *
 * @author druelens
 * @version 1
 */
class ParticipantFlowIrisScanLeftFragment : ParticipantFlowIrisScanFragment() {

    private lateinit var binding: FragmentParticipantFlowIrisScanLeftBinding
    private lateinit var mIrisViewLeft: NIrisView
    private var errorSnackbar: Snackbar? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_participant_flow_iris_scan_left,
            container,
            false
        )
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        binding.btnSkip.setOnClickListener {
            viewModel.onSkipButtonClick()
            flowViewModel.confirmIrisScan(IrisPosition.LEFT, false, ::onNoIrisScanned)
        }
        binding.btnSubmit.setOnClickListener {
            flowViewModel.confirmIrisScan(IrisPosition.LEFT, true)
        }

        binding.btnLoadImage.setOnClickListener {
            viewModel.onLoadImageButtonClick()
        }

        binding.btnCapture.setOnClickListener {
            onCapture()
        }

        binding.btnRedoCapture.setOnClickListener {
            onCapture() // Same behavior as the capture button
        }

        binding.btnStopScanning.setOnClickListener {
            viewModel.onStopScanningButtonClick()
        }
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        return binding.root
    }


    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        super.observeViewModel(lifecycleOwner)
        viewModel.irisObject.observe(lifecycleOwner) { iris ->
            binding.irisViewLeft.removeAllViews()
            mIrisViewLeft = NIrisView(requireActivity())
            binding.irisViewLeft.addView(mIrisViewLeft)
            mIrisViewLeft.iris = iris
            logInfo("irisView left set new iris object")
        }

    }

    override fun onDestroy() {
        super.onDestroy()
    }

    /**
     * Function to be executed when starting iris capturing using a scanner.
     * Dismisses open error (if present) and starts the capturing.
     */
    override fun onCapture() {
        errorSnackbar?.dismiss()
        viewModel.onCaptureButtonClick()
    }

    /**
     * Callback function implementation for when a scanner is connected.
     * If successfully connected, will trigger capturing of the iris.
     * If not successfully connected, an error message will be shown.
     *
     * @param   connected   Indication whether the scanner was successfully connected.
     */
    override fun onFailedToConnectScanner() {
        errorSnackbar = Snackbar.make(
            binding.root,
            R.string.iris_scan_msg_capturing_device_is_unavailable,
            Snackbar.LENGTH_SHORT
        ).also { it.show() }
    }

    override fun onStart() {
        super.onStart()
        viewModel.setArguments(flowViewModel.subject, flowViewModel.irisIndexes, IrisPosition.LEFT)
    }
}