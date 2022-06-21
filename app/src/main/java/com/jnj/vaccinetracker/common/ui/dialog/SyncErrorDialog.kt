package com.jnj.vaccinetracker.common.ui.dialog

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.snackbar.Snackbar
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.createSyncErrorsShareIntent
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogSyncErrorBinding
import kotlinx.coroutines.flow.onEach

class SyncErrorDialog : BaseDialogFragment() {

    companion object {
        fun create(): SyncErrorDialog {
            return SyncErrorDialog()
        }
    }

    private lateinit var binding: DialogSyncErrorBinding
    private lateinit var adapter: SyncErrorAdapter

    private val viewModel: SyncErrorViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        isCancelable = false
    }


    private fun share(uri: Uri) {
        val intent = createSyncErrorsShareIntent(uri)
        startActivity(intent)
    }

    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        super.observeViewModel(lifecycleOwner)
        viewModel.errorsFileToShare.asFlow().onEach { errorsFileToShare ->
            when (errorsFileToShare) {
                ErrorsFileToShareUiModel.Failure -> showErrorMessage(getString(R.string.sync_error_dialog_share_failure))
                is ErrorsFileToShareUiModel.Success -> share(errorsFileToShare.uri)
            }.let {}

        }.launchIn(lifecycleOwner)
        viewModel.items.observe(lifecycleOwner) { items ->
            adapter.updateItems(items.orEmpty())
        }
        viewModel.deleteAllErrorsCompletedEvent.asFlow().onEach { success ->
            if (!success)
                showErrorMessage(getString(R.string.general_label_error))
            else
                adapter.updateItems(emptyList())
        }.launchIn(lifecycleOwner)
    }

    private fun showErrorMessage(msg: String) {
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_sync_error, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        adapter = SyncErrorAdapter()
        binding.recyclerViewSyncErrors.adapter = adapter
        binding.recyclerViewSyncErrors.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        binding.executePendingBindings()

        binding.btnFinish.setOnClickListener {
            dismissAllowingStateLoss()
        }
        binding.btnShare.setOnClickListener {
            viewModel.onShareClick()
        }
        binding.btnDeleteAll.setOnClickListener {
            viewModel.onDeleteAllClick()
        }
        return binding.root
    }
}