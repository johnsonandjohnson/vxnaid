package com.jnj.vaccinetracker.common.di

import com.jnj.vaccinetracker.settings.mock.MockSettingsDialog
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class MockSettingsDialogModule {
    @ContributesAndroidInjector
    abstract fun buildMockSettingsDialog(): MockSettingsDialog
}