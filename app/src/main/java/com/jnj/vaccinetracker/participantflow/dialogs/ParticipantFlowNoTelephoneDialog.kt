package com.jnj.vaccinetracker.participantflow.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.findParent
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogParticipantFlowNoTelephoneBinding

class ParticipantFlowNoTelephoneDialog : BaseDialogFragment() {

    private lateinit var binding: DialogParticipantFlowNoTelephoneBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        isCancelable = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_participant_flow_no_telephone, container, false)
        binding.btnConfirm.setOnClickListener {
            findParent<ParticipantFlowNoPhoneConfirmationListener>()?.confirmNoTelephone()
            dismissAllowingStateLoss()
        }
        binding.btnCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
        return binding.root
    }

    interface ParticipantFlowNoPhoneConfirmationListener {
        fun confirmNoTelephone()
    }

}