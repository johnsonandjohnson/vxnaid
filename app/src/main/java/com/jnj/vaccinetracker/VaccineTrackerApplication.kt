package com.jnj.vaccinetracker

import com.jnj.vaccinetracker.common.di.DaggerAppComponent
import com.jnj.vaccinetracker.common.helpers.logDebug
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import dagger.android.HasAndroidInjector
import javax.inject.Inject


class VaccineTrackerApplication : DaggerApplication(), HasAndroidInjector {

    @Inject
    lateinit var appController: AppController

    override fun onCreate() {
        super.onCreate()
        logDebug("onCreate")
        appController.initState()
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerAppComponent.builder()
            .application(this)
            .build()
    }

}