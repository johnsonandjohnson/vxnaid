package com.jnj.vaccinetracker.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogUpdateBinding
import kotlinx.coroutines.flow.onEach

/**
 * @author druelens
 * @version 1
 */
class UpdateDialog : BaseDialogFragment() {

    private val viewModel: UpdateViewModel by viewModels { viewModelFactory }
    private lateinit var binding: DialogUpdateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_update, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.btnCancel.setOnClickListener {
            viewModel.onCancelClick()
            dismissAllowingStateLoss()
        }
        binding.btnUpdate.setOnClickListener {
            viewModel.update()
        }

        return binding.root
    }

    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        super.observeViewModel(lifecycleOwner)

        viewModel.downloadCompletedEvent
            .asFlow()
            .onEach {
                onDownloadCompleted()
            }
            .launchIn(lifecycleOwner)
    }

    private fun onDownloadCompleted() {
        val manager = context?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val install = Intent(Intent.ACTION_VIEW)
        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        install.setDataAndType(viewModel.destContentUri,
            manager.getMimeTypeForDownloadedFile(viewModel.downloadId))
        startActivity(install)
    }
}