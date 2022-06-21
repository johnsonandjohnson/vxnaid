package com.jnj.vaccinetracker.register.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.findParent
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogRegisterParticipantConfirmNoTelephoneBinding

/**
 * @author druelens
 * @version 1
 */
class RegisterParticipantConfirmNoTelephoneDialog : BaseDialogFragment() {

    private lateinit var binding: DialogRegisterParticipantConfirmNoTelephoneBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        isCancelable = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_register_participant_confirm_no_telephone, container, false)
        binding.btnConfirm.setOnClickListener {
            findParent<RegisterParticipationNoTelephoneConfirmationListener>()?.confirmNoTelephone()
            dismissAllowingStateLoss()
        }
        binding.btnCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
        return binding.root
    }

    interface RegisterParticipationNoTelephoneConfirmationListener {
        fun confirmNoTelephone()
    }

}