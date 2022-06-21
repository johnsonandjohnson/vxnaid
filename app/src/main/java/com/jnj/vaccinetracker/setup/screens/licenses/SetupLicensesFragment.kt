package com.jnj.vaccinetracker.setup.screens.licenses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentSetupLicensesBinding
import com.jnj.vaccinetracker.setup.SetupFlowViewModel
import kotlinx.coroutines.flow.onEach

class SetupLicensesFragment : BaseFragment() {
    private val viewModel: SetupLicensesViewModel by viewModels { viewModelFactory }
    private val flowViewModel: SetupFlowViewModel by activityViewModels { viewModelFactory }
    private lateinit var binding: FragmentSetupLicensesBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_setup_licenses, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        viewModel.finishScreenEvent.asFlow().onEach {
            flowViewModel.confirmLicensesSetup()
        }.launchIn(lifecycleOwner)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnDeactivate.setOnClickListener {
            viewModel.deactivateLicenses()
        }
        binding.btnActivate.setOnClickListener {
            viewModel.activateLicenses()
        }
    }
}