package com.jnj.vaccinetracker.common.di

import com.jnj.vaccinetracker.VaccineTrackerApplication
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import javax.inject.Singleton

/**
 * @author maartenvangiel
 * @version 1
 */
@Singleton
@Component(
    modules = [
        AppModule::class,
        NetworkModule::class,
        BiometricsModule::class,
        DatabaseModule::class,
        EncryptionModule::class,
        AndroidInjectionModule::class,
        ViewModelFactoryModule::class,
        AndroidModule::class,
        ViewModelModule::class
    ]
)
interface AppComponent : AndroidInjector<VaccineTrackerApplication> {
    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: VaccineTrackerApplication): Builder

        fun build(): AppComponent
    }
}