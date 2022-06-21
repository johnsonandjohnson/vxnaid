package com.jnj.vaccinetracker.participantflow.dialogs

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogMissingIdentifiersBinding
import com.jnj.vaccinetracker.participantflow.ParticipantFlowActivity

/**
 * @author druelens
 * @version 1
 */
class ParticipantFlowMissingIdentifiersDialog : BaseDialogFragment() {
    private lateinit var binding: DialogMissingIdentifiersBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_missing_identifiers, container, false)
        binding.btnRestart.setOnClickListener {

            // Redirect back to to home
            val intent = Intent(activity, ParticipantFlowActivity::class.java)
            requireActivity().startActivity(intent)
            requireActivity().overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            dismissAllowingStateLoss()
            requireActivity().finish()
        }
        return binding.root
    }
}