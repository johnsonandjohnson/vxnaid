package com.jnj.vaccinetracker.settings.mock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.createShareIntent
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogMockSettingsBinding
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Provider


class MockSettingsDialog : BaseDialogFragment() {

    @Inject
    lateinit var vmFactory: Provider<MockBackendSettingsViewModel>

    private val mockSettingsViewModel: MockBackendSettingsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return vmFactory.get() as T
            }
        }
    }
    private lateinit var binding: DialogMockSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    private fun createTimeTrackerReportFileShareIntent(shareTimeTrackerReport: ShareTimeTrackerReport) =
        createShareIntent(shareTimeTrackerReport.uri, "text/csv", shareTimeTrackerReport.fileName)

    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        mockSettingsViewModel.observeParticipantGenerationCount().onEach {
            binding.editParticipantGenerationCount.setText(it.takeIf { it > 0 }?.toString().orEmpty())
        }.launchIn(lifecycleOwner)
        mockSettingsViewModel.shareTimeTrackerReportEvent.asFlow()
            .onEach { shareTimeTrackerReport ->
                logInfo("share time tracker report event: $shareTimeTrackerReport")
                val intent = createTimeTrackerReportFileShareIntent(shareTimeTrackerReport)
                startActivity(intent)
            }.launchIn(lifecycleOwner)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_mock_settings, container, false)
        binding.viewModel = mockSettingsViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.btnClose.setOnClickListener {
            dismissAllowingStateLoss()
        }
        binding.btnConfirmParticipantCount.setOnClickListener {
            val value = binding.editParticipantGenerationCount.text.toString().toIntOrNull() ?: 0
            mockSettingsViewModel.onParticipantGenerationCountChanged(value)
        }
        binding.btnShareTimeTrackerReport.setOnClickListener {
            mockSettingsViewModel.onShareTimeTrackerReportClick()
        }
        return binding.root
    }

}