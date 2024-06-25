package com.jnj.vaccinetracker.participantflow.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentParticipantAddOrSearchBinding
import com.jnj.vaccinetracker.participantflow.ParticipantFlowViewModel
import com.jnj.vaccinetracker.register.RegisterParticipantFlowActivity
import com.jnj.vaccinetracker.update.UpdateDialog
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach

class ParticipantFlowAddOrSearchFragment: BaseFragment() {
   private companion object {
      private const val TAG_UPDATE_DIALOG = "updateDialog"
   }

   private val viewModel: ParticipantFlowViewModel by activityViewModels { viewModelFactory }
   private val viewModelParticipantFlow: ParticipantFlowMatchingViewModel by viewModels { viewModelFactory }

   private lateinit var binding: FragmentParticipantAddOrSearchBinding

   override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
      binding = DataBindingUtil.inflate(inflater, R.layout.fragment_participant_add_or_search, container, false)
      binding.viewModel = viewModel
      binding.lifecycleOwner = viewLifecycleOwner
      binding.btnContinue.setOnClickListener {
         viewModel.onSearchParticipant()
      }
      binding.btnNewParticipant.setOnClickListener {
         viewModelParticipantFlow.onNewParticipantButtonClick()
      }

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
    * We don't want to show the cancel workflow button on the home page.
    */
   override fun onPrepareOptionsMenu(menu: Menu) {
      super.onPrepareOptionsMenu(menu)
      menu.findItem(R.id.action_logout).isVisible = true
      menu.findItem(R.id.action_cancel).isVisible = false
   }

}