package com.jnj.vaccinetracker.visit.screens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.material.snackbar.Snackbar
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.hideKeyboard
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.common.ui.SyncBanner
import com.jnj.vaccinetracker.databinding.FragmentContradictionsBinding
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.splash.SplashActivity
import com.jnj.vaccinetracker.visit.VisitActivity
import com.jnj.vaccinetracker.visit.dialog.RescheduleVisitDialog

class ContradictionsActivity : BaseActivity() {
   private val participant: ParticipantSummaryUiModel by lazy {
      intent.getParcelableExtra(EXTRA_PARTICIPANT)!!
   }
   private val newRegisteredParticipant: Boolean by lazy {
      intent.getBooleanExtra(EXTRA_TYPE, false)
   }
   private val viewModel: ContradictionsViewModel by viewModels { viewModelFactory }
   private lateinit var binding: FragmentContradictionsBinding
   private var errorSnackbar: Snackbar? = null

   companion object {
      const val TAG_DIALOG_RESCHEDULE_VISIT = "rescheduleVisitDialog"
      private const val EXTRA_PARTICIPANT = "participant"
      private const val EXTRA_TYPE = "newParticipantRegistration"

      fun create(context: Context, participant: ParticipantSummaryUiModel, newRegisteredParticipant: Boolean): Intent {
         return Intent(context, ContradictionsActivity::class.java).apply {
            putExtra(EXTRA_PARTICIPANT, participant)
            putExtra(EXTRA_TYPE, newRegisteredParticipant)
         }
      }
   }

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      binding = DataBindingUtil.setContentView(this, R.layout.fragment_contradictions)
      binding.viewModel = viewModel
      binding.lifecycleOwner = this

      setupClickListeners()
   }

   private fun setupClickListeners() {
      binding.root.setOnClickListener {
         currentFocus?.hideKeyboard()
      }
      binding.btnYes.setOnClickListener {
         RescheduleVisitDialog.create(participant = participant)
            .show(supportFragmentManager, TAG_DIALOG_RESCHEDULE_VISIT)
      }
      binding.btnNo.setOnClickListener {
         startParticipantVisit(participant, newRegisteredParticipant)
      }
   }

   private fun startParticipantVisit(participant: ParticipantSummaryUiModel, newRegisteredParticipant: Boolean) {
      startActivity(VisitActivity.create(this, participant, newRegisteredParticipant))
      setForwardAnimation()
   }

   override fun onSupportNavigateUp(): Boolean {
      onBackPressed()
      return true
   }

   override fun onBackPressed() {
      if (newRegisteredParticipant) {
         startActivity(SplashActivity.create(this)) // Restart the participant flow
         finishAffinity()
      } else {
         super.onBackPressed()
      }
   }

   override fun onStart() {
      super.onStart()
      viewModel.errorMessage.observe(this) { errorMessage ->
         errorSnackbar?.dismiss()

         if (errorMessage == null) {
            return@observe
         }

         errorSnackbar = Snackbar
            .make(binding.root, errorMessage, Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.general_label_retry) {
               errorSnackbar?.dismiss()
               RescheduleVisitDialog.create(participant = participant)
                  .show(supportFragmentManager, TAG_DIALOG_RESCHEDULE_VISIT)
            }.also {
               it.show()
            }
      }
   }

   override val syncBanner: SyncBanner
      get() = binding.syncBanner
}
