package com.jnj.vaccinetracker.setup.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentSetupIntroBinding
import com.jnj.vaccinetracker.setup.SetupFlowViewModel

class SetupIntroFragment : BaseFragment() {

    private val flowViewModel: SetupFlowViewModel by activityViewModels { viewModelFactory }
    private lateinit var binding: FragmentSetupIntroBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_setup_intro, container, false)
        binding.flowViewModel = flowViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.startButton.setOnClickListener {
            flowViewModel.startSetup()
        }

        // Hide back-button for this fragment
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)

        return binding.root
    }
}