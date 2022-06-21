package com.jnj.vaccinetracker.common.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.jnj.vaccinetracker.VaccineTrackerApplication
import com.jnj.vaccinetracker.common.data.helpers.Base64
import com.jnj.vaccinetracker.common.data.helpers.Base64Impl
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.tfcporciuncula.flow.FlowSharedPreferences
import dagger.Module
import dagger.Provides
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import javax.inject.Singleton

/**
 * @author maartenvangiel
 * @version 1
 */
@Module(includes = [SyncLoggerModule::class])
class AppModule {

    @Provides
    @Singleton
    fun provideApplication(app: VaccineTrackerApplication): Application = app

    @Provides
    @Singleton
    fun provideApplicationContext(app: Application): Context = app.applicationContext

    @Provides
    @Singleton
    fun provideSharedPreferences(app: Application): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(app.applicationContext)

    @Provides
    @Singleton
    fun providePhoneNumberUtil(app: Application): PhoneNumberUtil = PhoneNumberUtil.createInstance(app.applicationContext)

    @Provides
    @Singleton
    fun provideAppResources(app: Application): ResourcesWrapper = AppResources(app.applicationContext)

    @Provides
    @Singleton
    fun provideDispatchers(): AppCoroutineDispatchers = AppCoroutineDispatchers.DEFAULT

    @Provides
    fun provideFlowSharedPreferences(prefs: SharedPreferences, dispatchers: AppCoroutineDispatchers): FlowSharedPreferences = FlowSharedPreferences(prefs, dispatchers.io)

    @Provides
    @Singleton
    fun provideBase64(impl: Base64Impl): Base64 = impl
}