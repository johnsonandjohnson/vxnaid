package com.jnj.vaccinetracker.common.di

import androidx.lifecycle.ViewModel
import com.jnj.vaccinetracker.barcode.ScanBarcodeViewModel
import com.jnj.vaccinetracker.common.ui.BaseActivityViewModel
import com.jnj.vaccinetracker.common.ui.dialog.SyncErrorViewModel
import com.jnj.vaccinetracker.irisscanner.ScannerConnectedViewModel
import com.jnj.vaccinetracker.login.LoginViewModel
import com.jnj.vaccinetracker.participantflow.ParticipantFlowViewModel
import com.jnj.vaccinetracker.participantflow.screens.ParticipantFlowIrisScanViewModel
import com.jnj.vaccinetracker.participantflow.screens.ParticipantFlowMatchingViewModel
import com.jnj.vaccinetracker.participantflow.screens.ParticipantFlowParticipantIdViewModel
import com.jnj.vaccinetracker.participantflow.screens.ParticipantFlowPhoneNumberViewModel
import com.jnj.vaccinetracker.register.RegisterParticipantFlowViewModel
import com.jnj.vaccinetracker.register.dialogs.HomeLocationPickerViewModel
import com.jnj.vaccinetracker.register.screens.RegisterParticipantParticipantDetailsViewModel
import com.jnj.vaccinetracker.settings.SettingsViewModel
import com.jnj.vaccinetracker.setup.SetupFlowViewModel
import com.jnj.vaccinetracker.setup.screens.SetupBackendConfigViewModel
import com.jnj.vaccinetracker.setup.screens.SetupPermissionsViewModel
import com.jnj.vaccinetracker.setup.screens.SetupSyncConfigViewModel
import com.jnj.vaccinetracker.setup.screens.licenses.SetupLicensesViewModel
import com.jnj.vaccinetracker.setup.screens.mainmenu.SetupMainMenuViewModel
import com.jnj.vaccinetracker.setup.screens.p2p.device_role.SetupP2pDeviceRoleViewModel
import com.jnj.vaccinetracker.setup.screens.p2p.transfer.client.SetupP2pDeviceClientTransferViewModel
import com.jnj.vaccinetracker.setup.screens.p2p.transfer.server.SetupP2pDeviceServerTransferViewModel
import com.jnj.vaccinetracker.splash.SplashViewModel
import com.jnj.vaccinetracker.update.UpdateViewModel
import com.jnj.vaccinetracker.visit.VisitViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
@SuppressWarnings("TooManyFunctions")
interface ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(SplashViewModel::class)
    fun bindSplashViewModel(splashViewModel: SplashViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SetupFlowViewModel::class)
    fun bindSetupViewModel(setupFlowViewModel: SetupFlowViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SetupSyncConfigViewModel::class)
    fun bindSetupSyncConfigViewModel(setupSyncConfigViewModel: SetupSyncConfigViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SetupBackendConfigViewModel::class)
    fun bindSetupBackendConfigViewModel(setupBackendConfigViewModel: SetupBackendConfigViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SetupPermissionsViewModel::class)
    fun bindSetupPermissionsViewModel(setupPermissionsViewModel: SetupPermissionsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ScannerConnectedViewModel::class)
    fun bindScannerConnectedViewModel(scannerConnectedViewModel: ScannerConnectedViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ParticipantFlowViewModel::class)
    fun bindParticipantFlowViewModel(participantFlowViewModel: ParticipantFlowViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(BaseActivityViewModel::class)
    fun bindBaseActivityViewModel(baseActivityViewModel: BaseActivityViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LoginViewModel::class)
    fun bindLoginViewModel(loginViewModel: LoginViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SettingsViewModel::class)
    fun bindSettingsViewModel(model: SettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(UpdateViewModel::class)
    fun bindUpdateViewModel(model: UpdateViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ParticipantFlowParticipantIdViewModel::class)
    fun bindParticipantFlowParticipantIdViewModel(participantFlowParticipantIdViewModel: ParticipantFlowParticipantIdViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ParticipantFlowPhoneNumberViewModel::class)
    fun bindParticipantFlowPhoneNumberViewModel(participantFlowPhoneNumberViewModel: ParticipantFlowPhoneNumberViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ParticipantFlowIrisScanViewModel::class)
    fun bindParticipantFlowIrisScanViewModel(participantFlowIrisScanViewModel: ParticipantFlowIrisScanViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ScanBarcodeViewModel::class)
    fun bindScanBarcodeViewModel(scanBarcodeViewModel: ScanBarcodeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ParticipantFlowMatchingViewModel::class)
    fun bindParticipantFlowMatchingViewModel(model: ParticipantFlowMatchingViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RegisterParticipantFlowViewModel::class)
    fun bindRegisterParticipantFlowViewModel(model: RegisterParticipantFlowViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RegisterParticipantParticipantDetailsViewModel::class)
    fun bindRegisterParticipantParticipantDetailsViewModel(model: RegisterParticipantParticipantDetailsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(HomeLocationPickerViewModel::class)
    fun bindHomeLocationPickerViewModel(model: HomeLocationPickerViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(VisitViewModel::class)
    fun bindVisitViewModel(model: VisitViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SyncErrorViewModel::class)
    fun bindSyncErrorViewModel(model: SyncErrorViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SetupMainMenuViewModel::class)
    fun bindSetupMainMenuViewModel(model: SetupMainMenuViewModel): ViewModel


    @Binds
    @IntoMap
    @ViewModelKey(SetupP2pDeviceRoleViewModel::class)
    fun bindSetupP2pDeviceRoleViewModel(model: SetupP2pDeviceRoleViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SetupP2pDeviceClientTransferViewModel::class)
    fun bindSetupP2pDeviceClientTransferViewModel(model: SetupP2pDeviceClientTransferViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SetupP2pDeviceServerTransferViewModel::class)
    fun bindSetupP2pDeviceServerTransferViewModel(model: SetupP2pDeviceServerTransferViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SetupLicensesViewModel::class)
    fun bindSetupLicensesViewModel(model: SetupLicensesViewModel): ViewModel
}