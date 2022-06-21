package com.jnj.vaccinetracker.barcode

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.viewModels
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.databinding.ActivityScanBarcodeBinding

/**
 * @author maartenvangiel
 * @version 1
 */
class ScanBarcodeActivity : BaseActivity() {

    companion object {
        const val EXTRA_BARCODE = "barcode"
        private const val REQ_CAMERA_PERMISSION = 13
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        fun create(context: Context): Intent {
            return Intent(context, ScanBarcodeActivity::class.java)
        }
    }

    private val viewModel: ScanBarcodeViewModel by viewModels { viewModelFactory }
    private lateinit var binding: ActivityScanBarcodeBinding
    private var previewUseCase: Preview? = null
    private var barcodeScanUseCase: ImageAnalysis? = null
    private var camera: Camera? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_scan_barcode)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.buttonSubmit.setOnClickListener {
            if (viewModel.barcodeValid.value) {
                setResult(RESULT_OK, Intent().putExtra(EXTRA_BARCODE, viewModel.scannedBarcode.value.orEmpty()))
                finish()
            }
        }
        binding.btnFlash.setOnClickListener {
            viewModel.toggleFlash()
        }
        binding.btnClose.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
        init()
    }

    private fun init() {
        if (allPermissionsGranted()) {
            setUp()
        } else {
            showPermissionRationale()
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.flashOn.observe(this) { torchOn ->
            camera?.cameraControl?.enableTorch(torchOn)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun bindToCamera(cameraProvider: ProcessCameraProvider) {
        listOfNotNull(previewUseCase, barcodeScanUseCase)
            .forEach { cameraProvider.unbind(it) }
        camera = null

        previewUseCase = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(binding.barcodeView.surfaceProvider) }

        barcodeScanUseCase = createBarcodeScanUseCase()

        camera = cameraProvider.bindToLifecycle(
            this,
            CameraSelector.DEFAULT_BACK_CAMERA,
            previewUseCase,
            barcodeScanUseCase
        )
    }

    private fun createBarcodeScanUseCase(): ImageAnalysis {
        val scanner = BarcodeScanning.getClient()
        val useCase = ImageAnalysis.Builder().build()
        useCase.setAnalyzer(
            ContextCompat.getMainExecutor(applicationContext), { imageProxy ->
                processImageProxy(scanner, imageProxy)
            }
        )
        return useCase
    }


    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(
        barcodeScanner: BarcodeScanner,
        imageProxy: ImageProxy,
    ) {
        imageProxy.image?.let { image ->
            try {
                val inputImage =
                    InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)

                barcodeScanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        val barcode = barcodes.asSequence()
                            .map { it.rawValue }
                            .lastOrNull()
                        if (barcode != null) {
                            viewModel.onBarcodeScanned(barcode)
                        }
                    }
                    .addOnFailureListener {
                        logError("barcode scanner failure", it)
                    }.addOnCompleteListener {
                        // When the image is from CameraX analysis use case, must call image.close() on received
                        // images when finished using them. Otherwise, new images may not be received or the camera
                        // may stall.
                        imageProxy.close()
                    }
            } catch (e: MlKitException) {
                logError(e.localizedMessage ?: e.message ?: "error code: ${e.errorCode}", e)
            }
        }

    }

    private fun setUp() {
        logInfo("setUp")
        val cameraProviderFeature = ProcessCameraProvider.getInstance(this)

        cameraProviderFeature.addListener(
            {
                bindToCamera(cameraProviderFeature.get())
            },
            ContextCompat.getMainExecutor(applicationContext)
        )
    }

    private fun showPermissionRationale() {
        logInfo("showPermissionRationale")
        MaterialAlertDialogBuilder(this)
            .setMessage(getString(R.string.scan_barcode_permission_message))
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                setResult(RESULT_CANCELED)
                finish()
            }
            .setPositiveButton(getString(R.string.scan_barcode_grant_permission)) { _, _ ->
                requestCameraPermission()
            }
            .show()
    }

    private fun requestCameraPermission() {
        logInfo("requestCameraPermission")
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQ_CAMERA_PERMISSION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        logInfo("onRequestPermissionsResult")
        when (requestCode) {
            REQ_CAMERA_PERMISSION -> {
                if (grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
                    logInfo("onRequestPermissionsResult permission not granted")
                    setResult(RESULT_CANCELED)
                    finish()
                } else {
                    init()
                }
            }
        }

    }

}