package com.jnj.vaccinetracker.visit

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.barcode.ScanBarcodeViewModel
import com.jnj.vaccinetracker.common.helpers.findParent
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.common.ui.SyncBanner
import com.jnj.vaccinetracker.databinding.ActivityVisitBinding
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.splash.SplashActivity
import com.jnj.vaccinetracker.visit.adapters.OtherSubstanceItemAdapter
import com.jnj.vaccinetracker.visit.dialog.DialogScheduleMissingSubstances
import com.jnj.vaccinetracker.visit.dialog.DosingOutOfWindowDialog
import java.util.Date

/**
 * @author maartenvangiel
 * @version 1
 */
@RequiresApi(Build.VERSION_CODES.O)
class VisitActivity :
    BaseActivity(),
    DosingOutOfWindowDialog.DosingOutOfWindowDialogListener,
    DialogScheduleMissingSubstances.DialogScheduleMissingSubstancesListener
{

    companion object {
        private const val EXTRA_PARTICIPANT = "participant"
        private const val EXTRA_TYPE = "newParticipantRegistration"
        private const val TAG_DIALOG_SUCCESS = "successDialog"
        private const val TAG_DIALOG_DOSING_OUT_OF_WINDOW = "dosingOutOfWindowDialog"
        private const val TAG_DIALOG_SCHEDULE_MISSING_SUBSTANCES = "scheduleMissingSubstances"

        fun create(context: Context, participant: ParticipantSummaryUiModel, newRegisteredParticipant: Boolean): Intent {
            return Intent(context, VisitActivity::class.java)
                .putExtra(EXTRA_PARTICIPANT, participant)
                .putExtra(EXTRA_TYPE, newRegisteredParticipant)
        }
    }

    private val participant: ParticipantSummaryUiModel by lazy { intent.getParcelableExtra(EXTRA_PARTICIPANT)!! }
    private val newRegisteredParticipant: Boolean by lazy { intent.getBooleanExtra(EXTRA_TYPE, false) }
    private val viewModel: VisitViewModel by viewModels { viewModelFactory }
    private val scanModel:ScanBarcodeViewModel by viewModels{ viewModelFactory }
    private lateinit var binding: ActivityVisitBinding

    private var errorSnackbar: Snackbar? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scanModel.setArguments(participant)
        viewModel.setArguments(participant)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_visit)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        binding.viewPagerVisit.adapter = VisitPagerAdapter(this, supportFragmentManager)
        binding.tabLayout.setupWithViewPager(binding.viewPagerVisit)
        setupClickListeners()

        setTitle(R.string.visit_label_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(!newRegisteredParticipant)
    }

    private fun setupClickListeners() {
        binding.btnSubmit.setOnClickListener {
            onSubmit()
        }
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (tab.position == 0) {
                    makeSubmitBtnInvisible()
                } else if (tab.position == 1) {
                    makeSubmitBtnVisible()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                // Optional: Handle tab unselected logic here
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                // Optional: Handle tab reselected logic here
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
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
                    viewModel.onRetryClick()
                }.also {
                    it.show()
                }
        }
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

    private fun submitDosingVisit(overrideOutsideWindowCheck: Boolean = false,
                                  newVisitDate: Date? = null) {
        viewModel.submitDosingVisit(
            outsideTimeWindowConfirmationListener = ::showOutsideTimeWindowConfirmationDialog,
            missingSubstancesListener = ::showScheduleMissingSubstancesDialog,
            overrideOutsideTimeWindowCheck = overrideOutsideWindowCheck,
            newVisitDate = newVisitDate
        )
    }

    private fun showOutsideTimeWindowConfirmationDialog() {
        DosingOutOfWindowDialog().show(supportFragmentManager,
            TAG_DIALOG_DOSING_OUT_OF_WINDOW
        )
    }

    private fun showScheduleMissingSubstancesDialog(substances: List<String>) {
        DialogScheduleMissingSubstances(substances).show(supportFragmentManager,
            TAG_DIALOG_SCHEDULE_MISSING_SUBSTANCES
        )
    }

    override fun onOutOfWindowDosingConfirmed() {
        submitDosingVisit(overrideOutsideWindowCheck = true)
    }

    override fun onDialogScheduleMissingSubstancesConfirmed(date: Date) {
        submitDosingVisit(newVisitDate = date)
    }

    fun makeSubmitBtnVisible() {
        binding.btnSubmit.visibility = View.VISIBLE
    }

    fun makeSubmitBtnInvisible() {
        binding.btnSubmit.visibility = View.INVISIBLE
    }

    private fun onSubmit() {
        viewModel.checkIfAnyOtherSubstancesEmpty()
        if (viewModel.isAnyOtherSubstancesEmpty.value == true) {
            viewModel.isAnyOtherSubstancesEmpty.value = false
            binding.tabLayout.getTabAt(0)?.select()
            return
        }
        submitDosingVisit()
    }

    override val syncBanner: SyncBanner
        get() = binding.syncBanner

}