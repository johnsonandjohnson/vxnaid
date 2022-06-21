package com.jnj.vaccinetracker.setup.screens.p2p.device_role

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
import com.jnj.vaccinetracker.databinding.FragmentSetupP2pDeviceRoleBinding
import com.jnj.vaccinetracker.setup.SetupFlowViewModel
import com.jnj.vaccinetracker.setup.models.P2pDeviceRole
import kotlinx.coroutines.flow.onEach

class SetupP2pDeviceRoleFragment : BaseFragment() {

    private val viewModel: SetupP2pDeviceRoleViewModel by viewModels { viewModelFactory }
    private val flowViewModel: SetupFlowViewModel by activityViewModels { viewModelFactory }
    private lateinit var binding: FragmentSetupP2pDeviceRoleBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_setup_p2p_device_role, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        // Show back-button again
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.finishButton.setOnClickListener {
            viewModel.onContinueClick()
        }
        binding.deviceRoleGroup.setOnCheckedChangeListener { _, checkedId ->
            val deviceRole = when (checkedId) {
                R.id.radio_role_receiver -> P2pDeviceRole.CLIENT
                R.id.radio_role_sender -> P2pDeviceRole.SERVER
                else -> error("unrecognized checkedId")
            }
            viewModel.onDeviceRoleSelected(deviceRole)
        }
        if (binding.deviceRoleGroup.checkedRadioButtonId == View.NO_ID) {
            flowViewModel.deviceType.value?.let { viewModel.onDeviceRoleSelected(it) }
        }
    }

    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        super.observeViewModel(lifecycleOwner)
        viewModel.confirmP2pDeviceRoleEvent.asFlow().onEach { deviceRole ->
            flowViewModel.confirmP2PDeviceType(deviceRole)
        }.launchIn(lifecycleOwner)
    }
}