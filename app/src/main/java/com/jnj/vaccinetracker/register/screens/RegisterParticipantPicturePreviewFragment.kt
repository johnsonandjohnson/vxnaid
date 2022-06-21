package com.jnj.vaccinetracker.register.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentRegisterParticipantPicturePreviewBinding
import com.jnj.vaccinetracker.register.RegisterParticipantFlowViewModel

/**
 * Fragment used to present the operator with the photo that was taken
 * */
class RegisterParticipantPicturePreviewFragment : BaseFragment() {

    private val viewModel: RegisterParticipantFlowViewModel by activityViewModels { viewModelFactory }
    private lateinit var binding: FragmentRegisterParticipantPicturePreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_register_participant_picture_preview, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnSkip.setOnClickListener {
            viewModel.skipPicture()
        }
        binding.redoButton.setOnClickListener {
            viewModel.retakePicture()
        }
        binding.btnSubmit.setOnClickListener {
            viewModel.confirmPicture()
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.participantPicture.get() == null) {
            viewModel.navigateBack()
        }
    }

}

