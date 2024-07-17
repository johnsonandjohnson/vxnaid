package com.jnj.vaccinetracker.register.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.findParent
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogRegisterParticipantSuccessfulBinding
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel

/**
 * @author maartenvangiel
 * @version 1
 */
class RegisterParticipantSuccessfulDialog : BaseDialogFragment() {

    companion object {
        private const val ARG_PARTICIPANT = "participant"

        fun create(participant: ParticipantSummaryUiModel): RegisterParticipantSuccessfulDialog {
            return RegisterParticipantSuccessfulDialog().apply {
                arguments = bundleOf(ARG_PARTICIPANT to participant)
            }
        }
    }

    private lateinit var binding: DialogRegisterParticipantSuccessfulBinding
    private val participant: ParticipantSummaryUiModel by lazy { requireArguments().getParcelable(ARG_PARTICIPANT)!! }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_register_participant_successful, container, false)
        binding.btnContinue.setOnClickListener {
            findParent<RegisterParticipationCompletionListener>()?.continueWithParticipantVisit(participant)
            dismissAllowingStateLoss()
        }
        binding.btnFinish.setOnClickListener {
            findParent<RegisterParticipationCompletionListener>()?.finishParticipantFlow()
            dismissAllowingStateLoss()
        }
        return binding.root
    }

    interface RegisterParticipationCompletionListener {
        fun continueWithParticipantVisit(participant: ParticipantSummaryUiModel)
        fun finishParticipantFlow()
    }

}