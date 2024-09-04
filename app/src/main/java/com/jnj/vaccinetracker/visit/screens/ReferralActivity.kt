package com.jnj.vaccinetracker.visit.screens

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.managers.VisitManager
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.domain.entities.VisitDetail
import com.jnj.vaccinetracker.common.domain.usecases.UpdateVisitUseCase
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.databinding.ActivityReferralFlowBinding
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import kotlinx.coroutines.launch
import javax.inject.Inject

class ReferralActivity: BaseActivity() {
    private lateinit var binding: ActivityReferralFlowBinding
    private lateinit var clinicsDropdown: AutoCompleteTextView
    private lateinit var additionalInfoTextView: TextView
    private lateinit var referButton: Button
    private lateinit var doNotReferButton: Button
    private lateinit var referralResultText: TextView
    private lateinit var referralCloseButton: Button
    @Inject lateinit var configurationManager: ConfigurationManager
    @Inject lateinit var updateVisitUseCase: UpdateVisitUseCase
    @Inject lateinit var vaccineTrackerSyncApiDataSource: VaccineTrackerSyncApiDataSource
    @Inject lateinit var visitManager: VisitManager

    private lateinit var allVisits: List<VisitDetail>

    private var currentVisitUuid: String? = null
    private var participantUuid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReferralFlowBinding.inflate(layoutInflater)
        setContentView(binding.root)

        clinicsDropdown = binding.root.findViewById(R.id.dropdown_clinics)
        additionalInfoTextView = binding.root.findViewById(R.id.editText_additionalInfo)
        referButton = binding.root.findViewById(R.id.btn_saveReferral)
        doNotReferButton = binding.root.findViewById(R.id.btn_cancelReferral)
        referralResultText = binding.root.findViewById(R.id.textView_referralResult)
        referralCloseButton = binding.root.findViewById(R.id.btn_closeReferral)

        currentVisitUuid = intent.getStringExtra("currentVisitUuid")
        participantUuid = intent.getStringExtra("participantUuid")

        title = getString(R.string.referral_page_title)
        lifecycleScope.launch {
            try {
                val locations = configurationManager.getSites()
                val adapter = ArrayAdapter(this@ReferralActivity, R.layout.item_dropdown, locations.map { it.name })
                adapter.setDropDownViewResource(R.layout.item_dropdown)
                clinicsDropdown.setAdapter(adapter)
                allVisits = visitManager.getVisitsForParticipant(participantUuid!!)
            } catch (e: Exception) {
                Log.e("ReferralActivity", "Locations fetching failed", e)
            }
        }

        binding.btnSaveReferral.setOnClickListener {
            val selectedClinic = clinicsDropdown.text.toString()
            val additionalInfo = additionalInfoTextView.text.toString()

            val referralObservations: MutableMap<String, String> = mutableMapOf()
            if (selectedClinic.isNotEmpty()) {
                referralObservations[Constants.REFERRAL_CLINIC_CONCEPT_NAME] = selectedClinic
            }

            if (additionalInfo.isNotEmpty()) {
                referralObservations[Constants.REFERRAL_ADDITIONAL_INFO_CONCEPT_NAME] = additionalInfo
            }

            lifecycleScope.launch {
                try {
                    validateClinic()
                    if (selectedClinic.isNotEmpty()) {
                        vaccineTrackerSyncApiDataSource.updateEncounterObservationsByVisit(currentVisitUuid!!, referralObservations)
                        referralResultText.text = "${getString(R.string.referral_page_success_referral_text)} $selectedClinic"
                        referralResultText.setTextColor(ContextCompat.getColor(this@ReferralActivity, R.color.successDark))
                        referButton.visibility = View.GONE
                        doNotReferButton.visibility = View.GONE
                        referralCloseButton.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    Log.e("ReferralActivity", "Something went wrong during referring", e)
                    referralResultText.text = getString(R.string.referral_page_failed_referral_text)
                    referralResultText.setTextColor(ContextCompat.getColor(this@ReferralActivity, R.color.design_default_color_error))
                }
            }
        }

        doNotReferButton.setOnClickListener {
            finish()
        }

        referralCloseButton.setOnClickListener {
            finish()
        }
    }

    private fun validateClinic() {
        val selectedClinic = clinicsDropdown.text.toString()
        if (selectedClinic.isEmpty()) {
            clinicsDropdown.error = getString(R.string.referral_page_clinic_not_selected_hint)
        } else {
            clinicsDropdown.error = null
        }
    }
}