package com.jnj.vaccinetracker.setup.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentSetupConnectionBinding
import com.jnj.vaccinetracker.setup.SetupFlowViewModel
import kotlinx.coroutines.flow.onEach

class SetupBackendConfigFragment : BaseFragment() {

    private val flowViewModel: SetupFlowViewModel by activityViewModels { viewModelFactory }
    private val viewModel: SetupBackendConfigViewModel by viewModels { viewModelFactory }
    private lateinit var binding: FragmentSetupConnectionBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_setup_connection, container, false)
        binding.flowViewModel = flowViewModel
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        // Show back-button again
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.continueButton.setOnClickListener {
            // On button pressed, try to save backend settings
            viewModel.saveBackendSettings(
                backendUrl = binding.editSetupBackendUrl.text.toString(),
                username = binding.editUsername2.text.toString(),
                password = binding.editPassword.text.toString()
            )
        }
    }

    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        super.observeViewModel(lifecycleOwner)

        // If the backend settings are saved, let the flow know to continue to the next screen
        viewModel.backendSettingsCompleted
            .asFlow()
            .onEach {
                flowViewModel.confirmBackendSetup()
            }
            .launchIn(lifecycleOwner)

        // If the backend URL was saved previously and not entered yet, pre-load this value into the field
        viewModel.backendUrl.observe(lifecycleOwner) { backendUrl ->
            if (binding.editSetupBackendUrl.text.isEmpty()) binding.editSetupBackendUrl.setText(backendUrl)
        }
    }
}