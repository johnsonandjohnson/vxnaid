package com.jnj.vaccinetracker.register

import androidx.annotation.StringRes
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.models.NavigationDirection
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.participantflow.model.ParticipantImageUiModel
import javax.inject.Inject

/**
 * Responsible for managing the flow of registering a new participant.
 */
class RegisterParticipantFlowViewModel @Inject constructor(override val dispatchers: AppCoroutineDispatchers) : ViewModelBase() {

    val currentScreen = mutableLiveData<Screen>()
    var navigationDirection = NavigationDirection.NONE
    val participantPicture = mutableLiveData<ParticipantImageUiModel>()
    val participantId = mutableLiveData<String>()
    val leftEyeScanned = mutableLiveBoolean()
    val rightEyeScanned = mutableLiveBoolean()
    val isManualEnteredId = mutableLiveBoolean()
    val countryCode = mutableLiveData<String>()
    val phoneNumber = mutableLiveData<String>()

    fun setArguments(
        participantId: String?,
        leftEyeScanned: Boolean,
        rightEyeScanned: Boolean,
        countryCode: String?,
        phoneNumber: String?,
        isManualEnteredId: Boolean,
    ) {
        if (currentScreen.get() == null) {
            currentScreen.set(Screen.CAMERA_PERMISSION)
        }
        this.participantId.set(participantId)
        this.leftEyeScanned.set(leftEyeScanned)
        this.rightEyeScanned.set(rightEyeScanned)
        this.countryCode.set(countryCode)
        this.phoneNumber.set(phoneNumber)
        this.isManualEnteredId.set(isManualEnteredId)
    }

    fun navigateBack(): Boolean {
        val currentScreen = currentScreen.get() ?: return false
        val screens = Screen.values()
        val previousScreenIndex = screens.indexOf(currentScreen) - 1

        if (previousScreenIndex in screens.indices) {
            navigationDirection = NavigationDirection.BACKWARD
            this.currentScreen.set(screens[previousScreenIndex])
            return true
        }

        return false
    }

    private fun navigateForward(): Boolean {
        val currentScreen = currentScreen.get() ?: return false
        val screens = Screen.values()
        val nextScreenIndex = screens.indexOf(currentScreen) + 1

        if (nextScreenIndex in screens.indices) {
            navigationDirection = NavigationDirection.FORWARD
            this.currentScreen.set(screens[nextScreenIndex])
            return true
        }

        return false
    }

    fun confirmCameraPermissionGranted() {
        navigateForward()
    }

    fun savePictureAndContinue(imgBytes: ParticipantImageUiModel) {
        participantPicture.set(imgBytes)
        navigateForward()
    }

    fun retakePicture() {
        participantPicture.set(null)
        navigateBack()
    }

    fun skipPicture() {
        participantPicture.set(null)
        navigationDirection = NavigationDirection.FORWARD
        currentScreen.set(Screen.PARTICIPANT_DETAILS)
    }

    fun confirmPicture() {
        navigationDirection = NavigationDirection.FORWARD
        currentScreen.set(Screen.PARTICIPANT_DETAILS)
    }

    enum class Screen(@StringRes val title: Int) {
        CAMERA_PERMISSION(R.string.participant_registration_picture_title),
        TAKE_PICTURE(R.string.participant_registration_picture_title),
        CONFIRM_PICTURE(R.string.participant_registration_picture_title),
        PARTICIPANT_DETAILS(R.string.participant_registration_details_title)
    }

}
