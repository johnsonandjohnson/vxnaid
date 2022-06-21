/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jnj.vaccinetracker.register.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.Metadata
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.logWarn
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.CameraUiContainerBinding
import com.jnj.vaccinetracker.databinding.FragmentRegisterParticipantTakePictureBinding
import com.jnj.vaccinetracker.participantflow.model.ParticipantImageUiModel
import com.jnj.vaccinetracker.register.RegisterParticipantFlowViewModel
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Fragment used to let the operator take a picture of the new participant.
 */
@SuppressWarnings("TooManyFunctions")
class RegisterParticipantTakePictureFragment : BaseFragment() {

    private companion object {
        private const val ANIMATION_FAST_MILLIS = 50L
        private const val ANIMATION_SLOW_MILLIS = 100L
        private const val FOCUS_AUTOCANCEL_DURATION = 5L
    }

    private val flowViewModel: RegisterParticipantFlowViewModel by activityViewModels { viewModelFactory }
    private lateinit var binding: FragmentRegisterParticipantTakePictureBinding

    private var displayId: Int = -1
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraControl: CameraControl? = null

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val displayManager by lazy {
        requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@RegisterParticipantTakePictureFragment.displayId) {
                imageCapture?.targetRotation = view.display.rotation
            }
        } ?: Unit
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_register_participant_take_picture, container, false)
        binding.btnSkip.setOnClickListener {
            flowViewModel.skipPicture()
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the user could have removed them while the app was in paused state.
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            flowViewModel.navigateBack()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayManager.registerDisplayListener(displayListener, null)

        binding.viewFinder.post {
            displayId = binding.viewFinder.display.displayId
            updateCameraUi()
            setUpCamera()
            setupZoomAndTapToFocus()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupZoomAndTapToFocus() {
        val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scale = camera!!.cameraInfo.zoomState.value!!.zoomRatio * detector.scaleFactor
                camera!!.cameraControl.setZoomRatio(scale)
                return true
            }
        })
        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val factory = binding.viewFinder.meteringPointFactory
                val point = factory.createPoint(e.x, e.y)
                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                    .setAutoCancelDuration(FOCUS_AUTOCANCEL_DURATION, TimeUnit.SECONDS)
                    .build()
                cameraControl?.startFocusAndMetering(action)
                return true
            }
        })

        binding.viewFinderOverlay.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            return@setOnTouchListener true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        displayManager.unregisterDisplayListener(displayListener)
    }

    /**
     * Inflate camera controls and update the UI manually upon config changes to avoid removing
     * and re-adding the view finder from the view hierarchy; this provides a seamless rotation
     * transition on devices that support it.
     *
     * NOTE: The flag is supported starting in Android 8 but there still is a small flash on the
     * screen for devices that run Android 9 or below.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        updateCameraUi()
        updateCameraSwitchButton()
    }

    /** Initialize CameraX, and prepare to bind the camera use cases  */
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            lensFacing = when {
                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("Back and front camera are unavailable")
            }

            updateCameraSwitchButton()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /** Declare and bind preview, capture and analysis use cases */
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            cameraControl = camera?.cameraControl
            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            logError("Use case binding failed", exc)
        }
    }

    /** Method used to re-draw the camera UI controls, called every time configuration changes. */
    private fun updateCameraUi() {
        binding.cameraContainer.findViewById<ConstraintLayout>(R.id.camera_ui_container)?.let {
            binding.cameraContainer.removeView(it)
        }

        val controls = DataBindingUtil.inflate<CameraUiContainerBinding>(layoutInflater, R.layout.camera_ui_container, binding.cameraContainer, true)
        controls.cameraCaptureButton.setOnClickListener {
            takePicture(it)
        }
        controls.cameraSwitchButton.apply {
            isEnabled = false
            setOnClickListener {
                switchCamera()
            }
        }
    }

    private fun takePicture(captureButton: View) {
        val imageCapture = imageCapture ?: return logError("Cannot capture picture without imageCapture instance")
        captureButton.isEnabled = false

        val outputStream = ByteArrayOutputStream()
        val metadata = Metadata().apply {
            isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputStream)
            .setMetadata(metadata)
            .build()

        imageCapture.takePicture(outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                logError("Photo capture failed: ${exc.message}", exc)
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val imgBytes = outputStream.toByteArray()
                activity?.runOnUiThread {
                    flowViewModel.savePictureAndContinue(ParticipantImageUiModel(imgBytes))
                }
            }
        })

        showFlashAnimation()
    }

    private fun switchCamera() {
        logInfo("switchCamera")
        lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        bindCameraUseCases()
    }

    private fun showFlashAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.cameraContainer.postDelayed({
                binding.cameraContainer.foreground = ColorDrawable(Color.WHITE)
                binding.cameraContainer.postDelayed({ binding.cameraContainer.foreground = null }, ANIMATION_FAST_MILLIS)
            }, ANIMATION_SLOW_MILLIS)
        }
    }

    /** Enabled or disabled a button to switch cameras depending on the available cameras */
    private fun updateCameraSwitchButton() {
        val switchCamerasButton = binding.cameraContainer.findViewById<ImageButton>(R.id.camera_switch_button)
        try {
            switchCamerasButton.isEnabled = hasBackCamera() && hasFrontCamera()
        } catch (exception: CameraInfoUnavailableException) {
            logWarn("camera info unavailable", exception)
            switchCamerasButton.isEnabled = false
        }
    }

    /** Returns true if the device has an available back camera. False otherwise */
    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /** Returns true if the device has an available front camera. False otherwise */
    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

}
