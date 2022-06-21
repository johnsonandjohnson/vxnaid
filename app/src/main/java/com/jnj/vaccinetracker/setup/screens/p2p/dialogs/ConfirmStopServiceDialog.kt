package com.jnj.vaccinetracker.setup.screens.p2p.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.findParent
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogP2pConfirmStopServiceBinding

class ConfirmStopServiceDialog : BaseDialogFragment() {

    companion object {
        private const val KEY_BY_BACK = "by_back"
        fun newInstance(byBackButton: Boolean): ConfirmStopServiceDialog {
            return ConfirmStopServiceDialog().apply {
                arguments = Bundle().apply {
                    putBoolean(KEY_BY_BACK, byBackButton)
                }
            }
        }
    }

    private lateinit var binding: DialogP2pConfirmStopServiceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        isCancelable = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_p2p_confirm_stop_service, container, false)
        binding.btnExit.setOnClickListener {
            findParent<Callback>()?.onConfirmStopService(requireArguments().getBoolean(KEY_BY_BACK))
            dismissAllowingStateLoss()
        }
        binding.btnCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
        return binding.root
    }

    interface Callback {
        fun onConfirmStopService(byBackButton: Boolean)
    }
}