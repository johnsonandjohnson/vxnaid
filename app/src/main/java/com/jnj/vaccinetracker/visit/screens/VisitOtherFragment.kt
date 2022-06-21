package com.jnj.vaccinetracker.visit.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentVisitOtherBinding
import com.jnj.vaccinetracker.splash.SplashActivity
import com.jnj.vaccinetracker.visit.VisitViewModel
import com.jnj.vaccinetracker.visit.dialog.VisitRegisteredSuccessDialog
import kotlinx.coroutines.flow.onEach

/**
 * @author maartenvangiel
 * @version 1
 */
class VisitOtherFragment : BaseFragment(), VisitRegisteredSuccessDialog.VisitRegisteredSuccessDialogListener {

    private companion object {
        private const val TAG_DIALOG_SUCCESS = "successDialog"
    }

    private val viewModel: VisitViewModel by activityViewModels { viewModelFactory }
    private lateinit var binding: FragmentVisitOtherBinding

    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        viewModel.otherVisitEvents
            .asFlow()
            .onEach { success ->
                if (success) {
                    VisitRegisteredSuccessDialog.create(viewModel.upcomingVisit.value).show(childFragmentManager, TAG_DIALOG_SUCCESS)
                } else
                    Snackbar.make(binding.root, R.string.general_label_error, Snackbar.LENGTH_LONG).show()
            }.launchIn(lifecycleOwner)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_visit_other, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.btnSubmit.setOnClickListener {
            viewModel.submitOtherVisit()
        }
        return binding.root

    }

    override fun onVisitRegisteredSuccessDialogClosed() {
        requireActivity().apply {
            startActivity(SplashActivity.create(this)) // Restart the participant flow
            finishAffinity()
        }
    }

}