package com.jnj.vaccinetracker.visit.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.findParent
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogVisitDifferentManufacturerBinding
import com.jnj.vaccinetracker.visit.VisitViewModel

/**
 * @author druelens
 * @version 1
 */
class DifferentManufacturerExpectedDialog : BaseDialogFragment() {

    private val viewModel: VisitViewModel by activityViewModels { viewModelFactory }

    private lateinit var binding: DialogVisitDifferentManufacturerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_visit_different_manufacturer, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.executePendingBindings()
        binding.btnConfirm.setOnClickListener {
            findParent<DifferentManufacturerExpectedListener>()?.onDifferentManufacturerConfirmed()
            dismissAllowingStateLoss()
        }
        binding.btnCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
        return binding.root
    }

    interface DifferentManufacturerExpectedListener {
        fun onDifferentManufacturerConfirmed()
    }

}