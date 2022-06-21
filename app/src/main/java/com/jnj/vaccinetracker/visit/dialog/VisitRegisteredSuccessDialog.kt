package com.jnj.vaccinetracker.visit.dialog

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
import com.jnj.vaccinetracker.databinding.DialogVistRegisteredSuccessBinding
import com.jnj.vaccinetracker.sync.domain.entities.UpcomingVisit

/**
 * @author timonelen
 * @version 1
 */
class VisitRegisteredSuccessDialog : BaseDialogFragment() {

    companion object {
        private const val ARG_NEXT_VISIT = "next_visit"

        fun create(nextVisit: UpcomingVisit?): VisitRegisteredSuccessDialog {
            return VisitRegisteredSuccessDialog().apply { arguments = bundleOf(ARG_NEXT_VISIT to nextVisit) }
        }
    }

    private lateinit var binding: DialogVistRegisteredSuccessBinding
    private val nextVisit: UpcomingVisit? by lazy { requireArguments().getParcelable(ARG_NEXT_VISIT) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_vist_registered_success, container, false)
        binding.nextVisit = nextVisit
        binding.executePendingBindings()

        binding.btnFinish.setOnClickListener {
            dismissAllowingStateLoss()
        }
        return binding.root
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        findParent<VisitRegisteredSuccessDialogListener>()?.onVisitRegisteredSuccessDialogClosed()
    }

    interface VisitRegisteredSuccessDialogListener {
        fun onVisitRegisteredSuccessDialogClosed()
    }

}