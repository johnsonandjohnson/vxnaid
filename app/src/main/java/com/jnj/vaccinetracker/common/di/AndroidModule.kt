package com.jnj.vaccinetracker.common.di

import com.jnj.vaccinetracker.barcode.ScanBarcodeActivity
import com.jnj.vaccinetracker.common.ui.dialog.SuccessDialog
import com.jnj.vaccinetracker.common.ui.dialog.SyncErrorDialog
import com.jnj.vaccinetracker.irisscanner.ScannerConnectedActivity
import com.jnj.vaccinetracker.login.LoginActivity
import com.jnj.vaccinetracker.login.RefreshSessionDialog
import com.jnj.vaccinetracker.participantflow.ParticipantFlowActivity
import com.jnj.vaccinetracker.participantflow.dialogs.ParticipantFlowCancelWorkflowDialog
import com.jnj.vaccinetracker.participantflow.dialogs.ParticipantFlowMandatoryIrisDialog
import com.jnj.vaccinetracker.participantflow.dialogs.ParticipantFlowMissingIdentifiersDialog
import com.jnj.vaccinetracker.participantflow.dialogs.ParticipantFlowNoTelephoneDialog
import com.jnj.vaccinetracker.participantflow.screens.*
import com.jnj.vaccinetracker.register.RegisterParticipantFlowActivity
import com.jnj.vaccinetracker.register.dialogs.HomeLocationPickerDialog
import com.jnj.vaccinetracker.register.dialogs.RegisterParticipantConfirmNoTelephoneDialog
import com.jnj.vaccinetracker.register.dialogs.RegisterParticipantIdNotMatchingDialog
import com.jnj.vaccinetracker.register.dialogs.RegisterParticipantSuccessfulDialog
import com.jnj.vaccinetracker.register.screens.RegisterParticipantCameraPermissionFragment
import com.jnj.vaccinetracker.register.screens.RegisterParticipantParticipantDetailsFragment
import com.jnj.vaccinetracker.register.screens.RegisterParticipantPicturePreviewFragment
import com.jnj.vaccinetracker.register.screens.RegisterParticipantTakePictureFragment
import com.jnj.vaccinetracker.settings.SettingsDialog
import com.jnj.vaccinetracker.setup.SetupFlowActivity
import com.jnj.vaccinetracker.setup.dialogs.SetupCancelWizardDialog
import com.jnj.vaccinetracker.setup.screens.SetupBackendConfigFragment
import com.jnj.vaccinetracker.setup.screens.SetupIntroFragment
import com.jnj.vaccinetracker.setup.screens.SetupPermissionsFragment
import com.jnj.vaccinetracker.setup.screens.SetupSyncConfigFragment
import com.jnj.vaccinetracker.setup.screens.licenses.SetupLicensesFragment
import com.jnj.vaccinetracker.setup.screens.mainmenu.SetupMainMenuFragment
import com.jnj.vaccinetracker.setup.screens.p2p.device_role.SetupP2pDeviceRoleFragment
import com.jnj.vaccinetracker.setup.screens.p2p.dialogs.ConfirmStopServiceDialog
import com.jnj.vaccinetracker.setup.screens.p2p.transfer.client.SetupP2pDeviceClientTransferFragment
import com.jnj.vaccinetracker.setup.screens.p2p.transfer.server.SetupP2pDeviceServerTransferFragment
import com.jnj.vaccinetracker.splash.SplashActivity
import com.jnj.vaccinetracker.sync.presentation.SyncAndroidService
import com.jnj.vaccinetracker.update.UpdateDialog
import com.jnj.vaccinetracker.visit.VisitActivity
import com.jnj.vaccinetracker.visit.dialog.DifferentManufacturerExpectedDialog
import com.jnj.vaccinetracker.visit.dialog.DosingOutOfWindowDialog
import com.jnj.vaccinetracker.visit.dialog.VisitRegisteredSuccessDialog
import com.jnj.vaccinetracker.visit.screens.VisitDosingFragment
import com.jnj.vaccinetracker.visit.screens.VisitOtherFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * @author maartenvangiel
 * @version 1
 */
@Module
@SuppressWarnings("TooManyFunctions")
interface AndroidModule {

    @ContributesAndroidInjector
    fun bindSplashActivity(): SplashActivity

    @ContributesAndroidInjector
    fun bindSetupActivity(): SetupFlowActivity

    @ContributesAndroidInjector
    fun bindSetupCancelWizardDialog(): SetupCancelWizardDialog

    @ContributesAndroidInjector
    fun bindSetupIntroFragment(): SetupIntroFragment

    @ContributesAndroidInjector
    fun bindSetupBackendConfigFragment(): SetupBackendConfigFragment

    @ContributesAndroidInjector
    fun bindSetupSyncConfigFragment(): SetupSyncConfigFragment

    @ContributesAndroidInjector
    fun bindSetupPermissionsFragment(): SetupPermissionsFragment

    @ContributesAndroidInjector
    fun bindScannerConnectedActivity(): ScannerConnectedActivity

    @ContributesAndroidInjector
    fun bindParticipantFlowActivity(): ParticipantFlowActivity

    @ContributesAndroidInjector
    fun bindParticipantFlowIntroFragment(): ParticipantFlowIntroFragment

    @ContributesAndroidInjector
    fun bindParticipantFlowAddOrSearchFragment(): ParticipantFlowAddOrSearchFragment

    @ContributesAndroidInjector
    fun bindLoginActivity(): LoginActivity

    @ContributesAndroidInjector
    fun bindSettingsDialog(): SettingsDialog

    @ContributesAndroidInjector
    fun bindUpdateDialog(): UpdateDialog

    @ContributesAndroidInjector
    fun bindRefreshSessionDialog(): RefreshSessionDialog

    @ContributesAndroidInjector
    fun bindParticipantFlowParticipantIdFragment(): ParticipantFlowParticipantIdFragment

    @ContributesAndroidInjector
    fun bindParticipantFlowPhoneNumberFragment(): ParticipantFlowPhoneNumberFragment

    @ContributesAndroidInjector
    fun bindParticipantFlowNoTelephoneDialog(): ParticipantFlowNoTelephoneDialog

    @ContributesAndroidInjector
    fun bindParticipantFlowIrisScanFragment(): ParticipantFlowIrisScanFragment

    @ContributesAndroidInjector
    fun bindParticipantFlowIrisScanLeftFragment(): ParticipantFlowIrisScanLeftFragment

    @ContributesAndroidInjector
    fun bindParticipantFlowIrisScanRightFragment(): ParticipantFlowIrisScanRightFragment

    @ContributesAndroidInjector
    fun bindParticipantFlowMatchingFragment(): ParticipantFlowMatchingFragment

    @ContributesAndroidInjector
    fun bindScanBarcodeActivity(): ScanBarcodeActivity

    @ContributesAndroidInjector
    fun bindRegisterParticipantActivity(): RegisterParticipantFlowActivity

    @ContributesAndroidInjector
    fun bindRegisterParticipantCameraPermissionFragment(): RegisterParticipantCameraPermissionFragment

    @ContributesAndroidInjector
    fun bindRegisterParticipantTakePictureFragment(): RegisterParticipantTakePictureFragment

    @ContributesAndroidInjector
    fun bindRegisterParticipantPicturePreviewFragment(): RegisterParticipantPicturePreviewFragment

    @ContributesAndroidInjector
    fun bindRegisterParticipantFlowParticipantDetailsFragment(): RegisterParticipantParticipantDetailsFragment

    @ContributesAndroidInjector
    fun bindHomeLocationPickerDialog(): HomeLocationPickerDialog

    @ContributesAndroidInjector
    fun bindRegisterParticipantSuccessfulDialog(): RegisterParticipantSuccessfulDialog

    @ContributesAndroidInjector
    fun bindRegisterParticipantConfirmNoTelephoneDialog(): RegisterParticipantConfirmNoTelephoneDialog

    @ContributesAndroidInjector
    fun bindRegisterParticipantIdNotMatchingDialog(): RegisterParticipantIdNotMatchingDialog

    @ContributesAndroidInjector
    fun bindParticipantFlowCancelWorkflowDialog(): ParticipantFlowCancelWorkflowDialog

    @ContributesAndroidInjector
    fun bindParticipantFlowMissingIdentifiersDialog(): ParticipantFlowMissingIdentifiersDialog

    @ContributesAndroidInjector
    fun bindParticipantFlowMandatoryIrisDialog(): ParticipantFlowMandatoryIrisDialog

    @ContributesAndroidInjector
    fun bindVisitActivity(): VisitActivity

    @ContributesAndroidInjector
    fun bindVisitDosageFragment(): VisitDosingFragment

    @ContributesAndroidInjector
    fun bindVisitOtherFragment(): VisitOtherFragment

    @ContributesAndroidInjector
    fun bindSuccessDialog(): SuccessDialog

    @ContributesAndroidInjector
    fun bindDosingOutOfWindowDialog(): DosingOutOfWindowDialog

    @ContributesAndroidInjector
    fun bindVisitRegisteredSuccessDialog(): VisitRegisteredSuccessDialog

    @ContributesAndroidInjector
    fun bindDifferentManufacturerExpectedDialog(): DifferentManufacturerExpectedDialog

    @ContributesAndroidInjector
    fun bindVaccineTrackerSyncAndroidService(): SyncAndroidService

    @ContributesAndroidInjector
    fun bindSyncErrorDialog(): SyncErrorDialog

    @ContributesAndroidInjector
    fun bindSetupMainMenuFragment(): SetupMainMenuFragment

    @ContributesAndroidInjector
    fun bindSetupP2pDeviceClientTransferFragment(): SetupP2pDeviceClientTransferFragment

    @ContributesAndroidInjector
    fun bindSetupP2pDeviceServerTransferFragment(): SetupP2pDeviceServerTransferFragment

    @ContributesAndroidInjector
    fun bindSetupLicensesFragment(): SetupLicensesFragment

    @ContributesAndroidInjector
    fun bindSetupP2pDeviceRoleFragment(): SetupP2pDeviceRoleFragment

    @ContributesAndroidInjector
    fun bindConfirmBackPressDialog(): ConfirmStopServiceDialog

}
