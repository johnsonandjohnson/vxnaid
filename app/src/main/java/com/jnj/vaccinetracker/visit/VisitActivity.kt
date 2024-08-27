package com.jnj.vaccinetracker.visit

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.barcode.ScanBarcodeViewModel
import com.jnj.vaccinetracker.common.helpers.findParent
import com.jnj.vaccinetracker.common.helpers.hideKeyboard
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.common.ui.SyncBanner
import com.jnj.vaccinetracker.databinding.ActivityVisitBinding
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.register.dialogs.AlreadyAdministeredVaccineDatePickerDialog
import com.jnj.vaccinetracker.register.dialogs.VaccineDialog
import com.jnj.vaccinetracker.register.screens.RegisterParticipantAdministeredVaccinesFragment
import com.jnj.vaccinetracker.splash.SplashActivity
import com.jnj.vaccinetracker.visit.adapters.OtherSubstanceItemAdapter
import com.jnj.vaccinetracker.visit.dialog.DialogScheduleMissingSubstances
import com.jnj.vaccinetracker.visit.dialog.DosingOutOfWindowDialog
import java.util.Date
import com.jnj.vaccinetracker.visit.model.SubstanceDataModel
import com.soywiz.klock.DateTime
import kotlinx.coroutines.launch

/**
 * @author maartenvangiel
 * @version 1
 */
@RequiresApi(Build.VERSION_CODES.O)
class VisitActivity :
    BaseActivity(),
    DosingOutOfWindowDialog.DosingOutOfWindowDialogListener,
    DialogScheduleMissingSubstances.DialogScheduleMissingSubstancesListener,
    VaccineDialog.AddVaccineListener
{

    companion object {
        private const val EXTRA_PARTICIPANT = "participant"
        private const val EXTRA_TYPE = "newParticipantRegistration"
        private const val TAG_DIALOG_SUCCESS = "successDialog"
        private const val TAG_DIALOG_DOSING_OUT_OF_WINDOW = "dosingOutOfWindowDialog"
        private const val TAG_DIALOG_SCHEDULE_MISSING_SUBSTANCES = "scheduleMissingSubstances"
        private const val TAG_VACCINE_PICKER = "vaccinePicker"

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
    private var isFirstTab = true

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
        binding.root.setOnClickListener { this.currentFocus?.hideKeyboard() }
        binding.btnSubmit.setOnClickListener {
            onSubmit()
        }
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (tab.position == 0) {
                    makeSubmitBtnInvisible()
                    makeAddVaccineButtonInvisible()
                    isFirstTab = true
                } else if (tab.position == 1) {
                    makeSubmitBtnVisible()
                    makeAddVaccineButtonVisible()
                    isFirstTab = false
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                // Optional: Handle tab unselected logic here
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                // Optional: Handle tab reselected logic here
            }
        })
        binding.switchSuggest.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setIsSuggesting(isChecked)
            onVisitTypesChanged(viewModel.visitTypes.value)
        }
        binding.btnAddVaccine.setOnClickListener {
            onBtnAddVaccine()
        }
        binding.dropdownVisitTypes.setOnItemClickListener { _, _, position, _ ->
            val selectedVisitType = viewModel.visitTypes.value?.distinct()?.get(position)
                ?: return@setOnItemClickListener

            lifecycleScope.launch {
                updateSelectedVisitType(selectedVisitType)
                viewModel.onVisitTypeDropdownChange()
            }
        }
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
        viewModel.isSuggesting.observe(this) {isSuggesting ->
            onSuggestingSwitch(isSuggesting)
        }
        viewModel.visitTypes.observe(this) { visitTypes ->
            onVisitTypesChanged(visitTypes)
        }
    }

    private fun onVisitTypesChanged(visitTypes: List<String>?) {
        val adapter =
            ArrayAdapter(this, R.layout.item_dropdown, visitTypes?.distinct().orEmpty())
        binding.dropdownVisitTypes.setAdapter(adapter)
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

    fun makeAddVaccineButtonVisible() {
        if (viewModel.isSuggesting.value == false && !isFirstTab) {
            binding.btnAddVaccine.visibility = View.VISIBLE
        }
    }

    fun makeAddVaccineButtonInvisible() {
        binding.btnAddVaccine.visibility = View.INVISIBLE
    }

    private fun makeVisitTypeDropdownVisible() {
        binding.groupVisitTypdropdown.visibility = View.VISIBLE
    }

    private fun makeVisitTypeDropdownGone() {
        binding.groupVisitTypdropdown.visibility = View.GONE
    }

    private fun makeVisitTypeLabelVisible() {
        binding.labelVisitType.visibility = View.VISIBLE
    }

    private fun makeVisitTypeLabelGone() {
        binding.labelVisitType.visibility = View.GONE
    }

    private fun onSubmit() {
        viewModel.checkIfAnyOtherSubstancesEmpty()
        viewModel.checkVisitLocationSelection()
        if (viewModel.isAnyOtherSubstancesEmpty.value == true || !viewModel.isVisitLocationValid()) {
            viewModel.isAnyOtherSubstancesEmpty.value = false
            binding.tabLayout.getTabAt(0)?.select()
            return
        }
        submitDosingVisit()
    }

    private fun onSuggestingSwitch(suggestingValue: Boolean) {
        if (!suggestingValue) {
            val generalColor = ContextCompat.getColorStateList(this, R.color.colorTextOnLight)
            binding.tabLayout.backgroundTintList = null
            binding.viewPagerVisit.backgroundTintList = null
            binding.labelSuggest.setTextColor(generalColor)
            makeAddVaccineButtonVisible()
            makeVisitTypeDropdownVisible()
            makeVisitTypeLabelGone()
        } else {
            val colorAccent = ContextCompat.getColorStateList(this, R.color.colorAccent)
            binding.tabLayout.backgroundTintList = colorAccent
            binding.viewPagerVisit.backgroundTintList = colorAccent
            binding.labelSuggest.setTextColor(colorAccent)
            makeAddVaccineButtonInvisible()
            makeVisitTypeDropdownGone()
            makeVisitTypeLabelVisible()
        }
    }

    private fun onBtnAddVaccine() {
        val allSubstances = viewModel.substancesDataAll.value.orEmpty()
        val selectedSubstances = viewModel.selectedSubstancesData.value?.map{ substance ->
            substance.conceptName
        }?.toSet() ?: setOf()
        val filteredSubstances = allSubstances.filter { substance ->
            substance.conceptName !in selectedSubstances
        }
        VaccineDialog(filteredSubstances, withDate=false).show(supportFragmentManager, TAG_VACCINE_PICKER)
    }

    private fun updateSelectedVisitType(name: String?) {
        if (name == viewModel.selectedVisitType.value) return
        val text = name ?: ""
        viewModel.selectedVisitType.value = text
    }

    override val syncBanner: SyncBanner
        get() = binding.syncBanner

    override fun addVaccine(vaccine: SubstanceDataModel) {
        viewModel.addToSelectedSubstances(vaccine)
    }

    override fun addVaccineDate(conceptName: String, dateValue: DateTime) {
        // no need to implement
    }

}