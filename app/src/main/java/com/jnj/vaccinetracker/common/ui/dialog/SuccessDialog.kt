package com.jnj.vaccinetracker.common.ui.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.findParent
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogSuccessBinding

/**
 * @author maartenvangiel
 * @version 1
 */
class SuccessDialog : BaseDialogFragment() {

    companion object {
        private const val ARG_TITLE = "title"

        fun create(title: String): SuccessDialog {
            return SuccessDialog().apply { arguments = bundleOf(ARG_TITLE to title) }
        }
    }

    private lateinit var binding: DialogSuccessBinding
    private val title: String by lazy { requireArguments().getString(ARG_TITLE)!! }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_success, container, false)
        binding.title = title
        binding.executePendingBindings()

        binding.btnFinish.setOnClickListener {
            dismissAllowingStateLoss()
        }
        return binding.root
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        findParent<SuccessDialogListener>()?.onSuccessDialogClosed()
    }

    interface SuccessDialogListener {
        fun onSuccessDialogClosed()
    }

}