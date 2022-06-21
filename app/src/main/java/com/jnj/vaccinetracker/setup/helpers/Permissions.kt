package com.jnj.vaccinetracker.setup.helpers

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.jnj.vaccinetracker.common.helpers.isManualFlavor
import javax.inject.Inject

class Permissions @Inject constructor(private val context: Context) {

    val packageInstallPermissionRequired: Boolean get() = isManualFlavor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    val dosePermissionSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    @SuppressLint("NewApi")
    private fun calcDosePermission(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    private fun calcCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    val packageInstallPermissionsSet: Boolean get() = !packageInstallPermissionRequired || context.packageManager.canRequestPackageInstalls()
    val dosePermissionSet: Boolean get() = !dosePermissionSupported || calcDosePermission()
    val cameraPermissionSet: Boolean get() = calcCameraPermission()


    fun areMandatoryPermissionsSet(): Boolean {
        return cameraPermissionSet && (!isManualFlavor || packageInstallPermissionsSet)
    }
}