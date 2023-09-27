package com.jnj.vaccinetracker.visit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.google.android.material.snackbar.Snackbar
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.barcode.ScanBarcodeViewModel
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.common.ui.SyncBanner
import com.jnj.vaccinetracker.databinding.ActivityVisitBinding
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.splash.SplashActivity

/**
 * @author maartenvangiel
 * @version 1
 */
class VisitActivity : BaseActivity() {

    companion object {
        private const val EXTRA_PARTICIPANT = "participant"
        private const val EXTRA_TYPE = "newParticipantRegistration"

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scanModel.setArguments(participant)
        viewModel.setArguments(participant)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_visit)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        binding.viewPagerVisit.adapter = VisitPagerAdapter(this, supportFragmentManager)
        binding.tabLayout.setupWithViewPager(binding.viewPagerVisit)

        setTitle(R.string.visit_label_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(!newRegisteredParticipant)

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

    override val syncBanner: SyncBanner
        get() = binding.syncBanner

}