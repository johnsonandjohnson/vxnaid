package com.jnj.vaccinetracker.participantflow.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogMandatoryIrisBinding
import com.jnj.vaccinetracker.participantflow.ParticipantFlowViewModel

/**
 * @author druelens
 * @version 1
 */
class ParticipantFlowMandatoryIrisDialog : BaseDialogFragment() {
    private lateinit var binding: DialogMandatoryIrisBinding
    private val flowViewModel: ParticipantFlowViewModel by activityViewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_mandatory_iris, container, false)
        binding.btnOk.setOnClickListener {

            // Navigate to the previous screen (the first iris to be scanned) and dismiss the dialog
            dismissAllowingStateLoss()
            flowViewModel.navigateBack()
        }
        return binding.root
    }
}