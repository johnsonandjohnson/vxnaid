package com.jnj.vaccinetracker.setup.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentSetupPermissionsBinding
import com.jnj.vaccinetracker.setup.SetupFlowViewModel
import kotlinx.coroutines.flow.onEach


class SetupPermissionsFragment : BaseFragment() {
    private val flowViewModel: SetupFlowViewModel by activityViewModels { viewModelFactory }
    private val viewModel: SetupPermissionsViewModel by viewModels { viewModelFactory }
    private lateinit var binding: FragmentSetupPermissionsBinding

    private companion object {
        private const val PERMISSION_CAMERA_REQUEST_CODE = 21
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_setup_permissions, container, false)
        binding.flowViewModel = flowViewModel
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnRequestCamera.setOnClickListener {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), PERMISSION_CAMERA_REQUEST_CODE)
        }
        binding.btnRequestDoze.setOnClickListener {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + this.context?.applicationContext?.packageName)))
            }
        }
        binding.btnRequestInstall.setOnClickListener {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + this.context?.applicationContext?.packageName)))
            }
        }
        binding.finishButton.setOnClickListener {
            viewModel.onDoneClick()
        }
    }

    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        super.observeViewModel(lifecycleOwner)

        // If permission settings successfully completed, navigate to Login screen
        viewModel.permissionsSettingsCompleted
            .asFlow()
            .onEach {
                flowViewModel.confirmPermissions()
            }
            .launchIn(lifecycleOwner)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                PERMISSION_CAMERA_REQUEST_CODE -> viewModel.setCameraPermissionGranted()
            }
        } else {
            Toast.makeText(context, "Permission request denied", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkAllPermissionStatus()

    }
}