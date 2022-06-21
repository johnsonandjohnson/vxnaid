package com.jnj.vaccinetracker.setup.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentSetupSiteBinding
import com.jnj.vaccinetracker.setup.SetupFlowViewModel
import kotlinx.coroutines.flow.onEach

class SetupSyncConfigFragment : BaseFragment() {
    private val flowViewModel: SetupFlowViewModel by activityViewModels { viewModelFactory }
    private val viewModel: SetupSyncConfigViewModel by viewModels { viewModelFactory }
    private lateinit var binding: FragmentSetupSiteBinding
    private var adapter: SetupSiteSelectionAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_setup_site, container, false)
        binding.flowViewModel = flowViewModel
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.siteSelectionExposedDropdown.doOnTextChanged { text, _, _, _ ->
            viewModel.onSiteTextChanged(text.toString())
        }
        binding.siteSelectionExposedDropdown.setOnItemClickListener { adapterView, _, index, _ ->
            // When item in dropdown list tapped, store this as the selected site
            val ada = adapterView.adapter as SetupSiteSelectionAdapter
            val siteUiModel = ada.getItem(index)
            viewModel.setSelectedSite(siteUiModel)
        }
        binding.siteSelectionExposedDropdown.setOnClickListener {
            binding.siteSelectionExposedDropdown.post { binding.siteSelectionExposedDropdown.showDropDown() }
        }
        binding.finishButton.setOnClickListener {
            // On finish button tap, attempt to save the sync settings
            viewModel.saveSyncSettings()
        }

        return binding.root
    }

    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        super.observeViewModel(lifecycleOwner)

        // If sync settings successfully saved, navigate to next screen
        viewModel.syncSettingsCompleted
            .asFlow()
            .onEach {
                flowViewModel.confirmSiteSetup()
            }
            .launchIn(lifecycleOwner)

        // If the site was saved previously and not selected yet, pre-load this value into the dropdown
        viewModel.sites.observe(this) { sites ->
            val adapter = SetupSiteSelectionAdapter(this.requireContext(), R.layout.item_dropdown, sites.orEmpty())
            binding.siteSelectionExposedDropdown.setAdapter(adapter)
            this.adapter = adapter
        }

        viewModel.siteValidationMessage.observe(this) { errorMsg ->
            if (errorMsg != null) {
                binding.siteSelectionExposedDropdown.requestFocus()
            }
        }
    }
}