package com.jnj.vaccinetracker.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogRefreshSessionBinding
import com.jnj.vaccinetracker.splash.SplashActivity
import kotlinx.coroutines.flow.onEach

/**
 * @author maartenvangiel
 * @version 1
 */
class RefreshSessionDialog : BaseDialogFragment() {

    private val viewModel: LoginViewModel by viewModels { viewModelFactory }

    private lateinit var binding: DialogRefreshSessionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        isCancelable = false

    }

    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        viewModel.prefillUsername.observe(lifecycleOwner) { prefillUsername ->
            if (binding.editTextUsername.text.isEmpty()) {
                binding.editTextUsername.setText(prefillUsername)
            }
        }
        viewModel.loginCompleted
            .asFlow()
            .onEach {
                dismissAllowingStateLoss()
            }
            .launchIn(lifecycleOwner)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_refresh_session, container, false)
        binding.viewModel = viewModel
        viewModel.init(false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.buttonLogin.setOnClickListener { login() }
        binding.buttonReset.setOnClickListener { logout() }
        return binding.root
    }

    private fun login() {
        val username = binding.editTextUsername.text.toString()
        val password = binding.editTextPassword.text.toString()
        viewModel.login(username, password)
    }

    private fun logout() {
        viewModel.logout()
        startActivity(SplashActivity.create(requireContext()))
        requireActivity().finishAffinity()
    }

}