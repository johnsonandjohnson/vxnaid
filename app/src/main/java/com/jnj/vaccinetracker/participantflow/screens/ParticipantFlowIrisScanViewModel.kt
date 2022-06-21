package com.jnj.vaccinetracker.participantflow.screens

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.collection.ArrayMap
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.biometrics.IrisScannerClientProvider
import com.jnj.vaccinetracker.common.data.biometrics.cancelSilently
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.managers.LicenseManager
import com.jnj.vaccinetracker.common.data.models.IrisPosition
import com.jnj.vaccinetracker.common.data.models.LicenseType
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.domain.usecases.LoadAssetIrisTemplateUseCase
import com.jnj.vaccinetracker.common.exceptions.LicensesNotObtainedException
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.config.appSettings
import com.neurotec.biometrics.*
import com.neurotec.biometrics.client.NBiometricClient
import com.neurotec.devices.NDeviceType
import com.neurotec.devices.NIrisScanner
import com.neurotec.util.concurrent.CompletionHandler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for the iris scan fragments (left and right)
 *
 * @author druelens
 * @version 1
 */
@SuppressWarnings("TooManyFunctions")
class ParticipantFlowIrisScanViewModel @Inject constructor(
    private val configurationManager: ConfigurationManager,
    private val licenseManager: LicenseManager,
    private val irisScannerClientProvider: IrisScannerClientProvider,
    override val dispatchers: AppCoroutineDispatchers,
    private val resourcesWrapper: ResourcesWrapper,
    private val loadAssetIrisTemplateUseCase: LoadAssetIrisTemplateUseCase,
) : ViewModelBase() {


    private companion object {

        private const val SCAN_TONE_VOLUME = 100
        private const val SCAN_TONE_DURATION = 400
    }

    private var _biometricClient: NBiometricClient? = null

    private suspend fun biometricsClient(): NBiometricClient = irisScannerClientProvider.provideClient().also {
        _biometricClient = it
    }

    private suspend fun resetBiometricClient() {
        _biometricClient = null
        irisScannerClientProvider.reset()
    }

    enum class IrisScannerState {
        IDLE, INITIALIZING,

        /**
         * due to failed to read config or failed to get license
         */
        INIT_FAILED,
        LOADING_IMAGE, START_IRIS_CAPTURING,
        CAPTURE, CANCELLING
    }

    private fun isScannerIdle() = irisScannerEvents.value == IrisScannerState.IDLE

    private fun setIrisScannerState(irisScannerState: IrisScannerState) {
        irisScannerEvents.value = irisScannerState
    }

    val failedToConnectScannerEvents = eventFlow<Unit>()
    private val irisScannerEvents = MutableStateFlow(IrisScannerState.IDLE)
    val loading = mutableLiveBoolean()
    val canSkip = mutableLiveBoolean(true)
    val canCapture = mutableLiveBoolean()
    val canRedoCapture = mutableLiveBoolean()
    val canLoadImage = mutableLiveBoolean()
    val canStopScanning = mutableLiveBoolean()
    val canSubmit = mutableLiveBoolean()
    val sufficientQuality = mutableLiveBoolean()
    val infoMessage = mutableLiveData<String>()
    private var capturejob: Job? = null

    private val qualityThreshold = stateFlow(0)
    private lateinit var subject: NSubject
    private lateinit var irisIndexes: ArrayMap<IrisPosition, Int?>
    private lateinit var irisPosition: IrisPosition

    var irisObject = mutableLiveData<NIris>()

    var isArgsSet = false

    init {
        initState()
    }

    private val biometricPropertyChanged = PropertyChangeListener { evt: PropertyChangeEvent ->
        if ("Status" == evt.propertyName) {
            onStatusChanged((evt.source as NBiometric).status)
        }
    }

    private fun onStatusChanged(status: NBiometricStatus) {
        logInfo("onStatusChanged: $status")
    }

    private suspend fun load() {
        setIrisScannerState(IrisScannerState.INITIALIZING)
        val licenseObtained = try {
            licenseManager.getLicensesOrThrow(licenseTypes = listOf(LicenseType.IRIS_CLIENT))
            true
        } catch (ex: LicensesNotObtainedException) {
            yield()
            ex.rethrowIfFatal()
            logError("Something went wrong retrieving the license: ", ex)
            if (ex.isObtainableAfterForceClose)
                infoMessage.set(resourcesWrapper.getString(R.string.msg_no_iris_license_must_force_close))
            else
                infoMessage.set(resourcesWrapper.getString(R.string.iris_scan_msg_no_iris_license))
            false
        }
        val configOk = try {
            val config = configurationManager.getConfiguration()
            this.qualityThreshold.value = (config.irisScore)
            logInfo("Iris quality threshold set to: " + config.irisScore)
            true
        } catch (ex: Throwable) {
            yield()
            ex.rethrowIfFatal()
            logError("Failed to get iris quality threshold: ", ex)
            false
        }
        val initSuccess = licenseObtained && configOk
        if (!initSuccess)
            setIrisScannerState(IrisScannerState.INIT_FAILED)
        else
            setIrisScannerState(IrisScannerState.IDLE)
    }

    private fun initState() {
        licenseManager.setWritableStoragePath()
        scope.launch {
            load()
        }
        scope.launch {
            biometricsClient()
        }
        irisScannerEvents.onEach { state ->
            onIrisScannerStateChanged(state)
        }.launchIn(scope)
    }

    private fun refreshIrisScannerState() {
        onIrisScannerStateChanged(irisScannerEvents.value)
    }

    /**
     * Check if the biometrics client is currently executing a task
     */
    private fun isBusy(): Boolean {
        return _biometricClient?.currentBiometric != null || _biometricClient?.currentSubject != null
    }

    private fun onIrisScannerStateChanged(state: IrisScannerState) {
        logInfo("onIrisScannerStateChanged: $state")
        loading.set(state == IrisScannerState.INITIALIZING || state == IrisScannerState.LOADING_IMAGE || isBusy())
        canSkip.set(true)
        val showCaptureButton = state == IrisScannerState.IDLE
        canLoadImage.set(showCaptureButton)
        if (irisObject.value != null) {
            canRedoCapture.set(showCaptureButton)
            canCapture.set(false)
        } else {
            canCapture.set(showCaptureButton)
        }
        canStopScanning.set(state == IrisScannerState.CAPTURE)
        canSubmit.set(sufficientQuality.value)
    }

    /**
     * triggered on [canRedoCapture] and [canCapture] button click
     */
    fun onCaptureButtonClick() {
        startIrisCapturing()
    }

    /**
     * Called when start iris scanning.
     * Will attempt to connect the scanner and trigger a callback with the result.
     *
     */
    private fun startIrisCapturing() {
        if (!isScannerIdle())
            return
        capturejob?.cancel()
        capturejob = scope.launch {
            setIrisScannerState(IrisScannerState.START_IRIS_CAPTURING)
            try {
                val scanner: NIrisScanner? = getScanner()
                if (scanner == null) {
                    failedToConnectScannerEvents.tryEmit(Unit)
                } else {
                    biometricsClient().irisScanner = scanner
                    capture()
                }
            } finally {
                yield()
                setIrisScannerState(IrisScannerState.IDLE)
            }
        }
    }

    fun onStopScanningButtonClick() {
        stopIrisCapturing()
    }

    /**
     * Called when interrupting the iris scanning. Stops the task for the biometricsClient.
     */
    private fun stopIrisCapturing() = scope.launch {
        setIrisScannerState(IrisScannerState.CANCELLING)
        capturejob?.cancel()
        _biometricClient?.cancelSilently()
        capturejob?.join()
        resetBiometricClient()
        setIrisScannerState(IrisScannerState.IDLE)
    }

    private suspend fun capture() {
        val iris = NIris()
        iris.addPropertyChangeListener(biometricPropertyChanged)

        // Normally we would use the IrisPosition here to get the correct position for the scanner to use
        // However, as the iris scanner in our position is for 1 eye only, it does not use a specific position,
        // so we assign NEPosition.UNKNOWN
        iris.position = NEPosition.UNKNOWN

        irisObject.set(iris)
        performCapture(iris)
    }

    /**
     * Checks if the scan quality meets the defined threshold.
     *
     * @param   qualityMeasured Measured iris quality
     * @return                  True if the measured quality is higher than or equal to the threshold.
     *                          False otherwise.
     */
    private fun checkScanQuality(qualityMeasured: Float): Boolean {
        logInfo("checkScanQuality: $qualityMeasured")
        val successfulScan = if (qualityMeasured >= qualityThreshold.value) {
            sufficientQuality.set(true)
            infoMessage.set(
                resourcesWrapper.getString(
                    R.string.iris_scan_msg_quality_indication,
                    qualityMeasured.toString()
                )
            )
            true
        } else {
            sufficientQuality.set(false)
            infoMessage.set(
                resourcesWrapper.getString(
                    R.string.iris_scan_msg_quality_fail,
                    qualityMeasured.toString()
                )
            )
            false
        }
        refreshIrisScannerState()
        return successfulScan
    }

    /**
     * Callback function implementation for when an image is loaded as an NIris.
     * Extracts the iris from the image.
     *
     * @param   iris   NIris loaded from a JPEG image.
     */
    private suspend fun onLoadImageCompleted(iris: NIris) {
        extractIris(iris, irisPosition)
    }


    private suspend fun performCapture(iris: NIris) {
        extractIris(iris, irisPosition)
    }


    fun onSkipButtonClick() {
        capturejob?.cancel()
        _biometricClient?.cancelSilently()
        removeIrisFromSubject(irisPosition)
    }

    /**
     * Loads an image file from the assets and creates an NIris from it.
     * Passes the NIris to a callback function.
     *
     * This functionality is used to demo the iris scan component when no USB scanner is available,
     * e.g. when simulating the app in a virtual emulator. It should not be used in production.
     *
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    fun onLoadImageButtonClick() {
        setIrisScannerState(IrisScannerState.LOADING_IMAGE)
        scope.launch(dispatchers.io) {
            loadImage()
        }
    }

    private suspend fun loadImage() {
        // load image
        try {
            val iris = loadAssetIrisTemplateUseCase.loadIris(irisPosition)
            // Save iris to the iris object
            irisObject.set(iris)
            onLoadImageCompleted(iris)
        } catch (ex: Exception) {
            yield()
            ex.rethrowIfFatal()
            logError("load image fail", ex)
            infoMessage.set(resourcesWrapper.getString(R.string.general_label_error))
        } finally {
            yield()
            setIrisScannerState(IrisScannerState.IDLE)
        }
    }

    private fun createCompletionHandler(completableDeferred: CompletableDeferred<NBiometricStatus>): CompletionHandler<NBiometricTask, NBiometricOperation> {
        return object : CompletionHandler<NBiometricTask, NBiometricOperation> {
            override fun completed(task: NBiometricTask, operation: NBiometricOperation) {
                val status = task.status
                if (task.error != null) {
                    completableDeferred.completeExceptionally(task.error)
                } else when (status) {
                    NBiometricStatus.CANCELED -> completableDeferred.cancel()
                    else -> completableDeferred.complete(status)
                }
            }

            override fun failed(th: Throwable, operation: NBiometricOperation?) {
                completableDeferred.completeExceptionally(th)
            }
        }

    }

    /**
     * Saves the NIris to the NSubject and extracts the NERecord of it
     *
     * @param iris          NIris that is captured
     * @param irisPosition  IrisPosition at which the iris was scanned
     */
    private suspend fun extractIris(iris: NIris, irisPosition: IrisPosition) = withContext(dispatchers.io) {
        infoMessage.set(null)
        sufficientQuality.set(false)
        // Add the new capture
        addIrisToSubject(iris, irisPosition)
        val status = runCreateTemplateTask()
        if (status != null)
            onCreateTemplateCompletedSuccessfully(status)
    }

    private suspend fun runCreateTemplateTask(): NBiometricStatus? {
        return try {
            logInfo("creating template")
            val task = biometricsClient().createTask(EnumSet.of(NBiometricOperation.CREATE_TEMPLATE), subject)
            val completable = CompletableDeferred<NBiometricStatus>()
            biometricsClient().performTask(task, NBiometricOperation.CREATE_TEMPLATE, createCompletionHandler(completable))
            setIrisScannerState(IrisScannerState.CAPTURE)
            completable.await()
        } catch (ex: Throwable) {
            yield()
            ex.rethrowIfFatal()
            onCreateTemplateCompletedFailed(ex)
            when {
                ex.findMessage("The device cannot be accessed or it fails to perform an IO operation.")
                        || ex.findMessage("No supported devices found.") -> {
                    logWarn("iris scanner device is probably not connected properly")
                    infoMessage.set(resourcesWrapper.getString(R.string.general_label_error))
                    failedToConnectScannerEvents.tryEmit(Unit)
                    null
                }
                ex.findMessage("Surface was already locked") -> {
                    logWarn("Got 'Surface was already locked' error. Resetting biometric client.")
                    infoMessage.set(resourcesWrapper.getString(R.string.general_label_error))
                    resetBiometricClient()
                    null
                }
                ex.findMessage("Scanner is already capturing. Resetting biometric client.") -> {
                    logWarn("scanner is already capturing")
                    infoMessage.set(resourcesWrapper.getString(R.string.general_label_error))
                    resetBiometricClient()
                    null
                }
                else -> {
                    logWarn("Got unknown error")
                    infoMessage.set(resourcesWrapper.getString(R.string.general_label_error))
                    null
                }
            }
        }
    }


    /**
     * Removed NIris from the NSubject for the given position.
     *
     * @param   position    IrisPosition for which the subject iris should be removed.
     */
    private fun removeIrisFromSubject(position: IrisPosition) {

        // Check if the iris at this position was already saved, if so remove the old one
        val index = irisIndexes[position]
        if (index != null) {
            subject.irises.removeAt(index)
            irisIndexes[position] = null

            // For other eyes, the index needs to be lowered by 1 if it was greater,
            // because the array will be shifted
            for (pos in irisIndexes.keys) {
                if (irisIndexes[pos]?.let { it > index } == true) {
                    irisIndexes[pos] = irisIndexes[pos]?.minus(1)
                }
            }

        }
    }

    /**
     * Saves the NIris to the NSubject.
     * If an existing iris at this position was already captured, the old one will be discarded.
     *
     * @param   iris            NIris that is captured
     * @param   irisPosition    IrisPosition at which the iris was scanned
     */
    private fun addIrisToSubject(iris: NIris, irisPosition: IrisPosition) {

        // Check if the iris at this position was already saved, if so remove the old one
        removeIrisFromSubject(irisPosition)

        // Add the new iris
        subject.irises.add(iris)
        // Save the index to the ArrayMap
        irisIndexes[irisPosition] = subject.irises.indexOf(iris)
    }

    /**
     * Called upon completion of a biometric operation.
     * If the task was completed successfully, a tone will sound to indicate
     * that the iris extraction was performed.
     */
    private fun onOperationCompleted(status: NBiometricStatus?) {
        if (status == NBiometricStatus.OK) {
            val toneG = ToneGenerator(AudioManager.STREAM_ALARM, SCAN_TONE_VOLUME)
            toneG.startTone(ToneGenerator.TONE_PROP_ACK, SCAN_TONE_DURATION)
            logInfo("Captured iris")
        }
    }

    private fun onCreateTemplateCompletedSuccessfully(status: NBiometricStatus) {
        onOperationCompleted(status)
        if (status == NBiometricStatus.OK) {
            // Indicates the quality of the iris scan/image
            val quality: Float =
                subject.template.irises.records.last().quality.toFloat()
            checkScanQuality(quality)
        } else {
            logError("onCreateTemplateCompleted failed: $status")
        }
    }

    private fun onCreateTemplateCompletedFailed(th: Throwable) {
        logError("onCreateTemplateCompletedFailed", th)
    }


    /**
     * Retrieve the iris scanner device
     *
     * @return  NIrisScanner from the devices registered in the biometricsClient deviceManager.
     *          Null if none are found.
     */
    private suspend fun getScanner(): NIrisScanner? {
        for (device in biometricsClient().deviceManager.devices) {
            if (device.deviceType.contains(NDeviceType.IRIS_SCANNER)) {
                return device as NIrisScanner
            }
        }
        return null
    }

    /**
     * Set the subject for which irises should be captured.
     * Sets the indexes for the iris scans linked to the subject.
     * This array is set external to this view model, as it needs to be remembered across the participant identification flow,
     * in case the operator returns to the scanning screens to rescan an eye.
     *  @param   subject     NSubject representing a participant for which the irises are being captured.
     *  @param   irisIndexes     ArrayMap providing the indexes within the subject.irises array at which irises with given IrisPosition are stored.
     */
    fun setArguments(subject: NSubject, irisIndexes: ArrayMap<IrisPosition, Int?>, irisPosition: IrisPosition) {
        logInfo("setArguments")
        if (!isArgsSet) {
            this.subject = subject
            this.irisIndexes = irisIndexes
            this.irisPosition = irisPosition
            isArgsSet = true
            onArgumentsSet()
        }

    }

    private fun onArgumentsSet() {
        scope.launch {
            //wait for setup to complete
            logInfo("onArgumentsSet: waiting for iris scanner state idle")
            irisScannerEvents.filter { it == IrisScannerState.IDLE }.await()
            //load existing iris
            val isBusy = if (appSettings.isUatOrLess) isBusy() else null
            logInfo("onArgumentsSet: checking existing indexes, isBusy:$isBusy")
            val index = irisIndexes[irisPosition]
            if (index != null && index in subject.irises.indices) {
                val iris = subject.irises[index]
                if (iris != null) {
                    val records = subject.template?.irises?.records
                    if (records != null && index in records.indices) {
                        val quality = records[index].quality.toFloat()
                        irisObject.value = iris
                        logInfo("onArgumentsSet: $irisPosition matches with $index in irises array of subject and quality $quality")
                        checkScanQuality(quality)
                    } else {
                        logWarn("onArgumentsSet: $index not part of records size ${records?.size}")
                    }
                } else {
                    logWarn("onArgumentsSet: iris == null")
                }
            } else {
                logWarn("onArgumentsSet: $index is null or not part of irises")
            }
        }
    }
}
