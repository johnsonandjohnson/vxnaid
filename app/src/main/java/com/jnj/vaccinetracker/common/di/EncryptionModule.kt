package com.jnj.vaccinetracker.common.di

import android.app.Application
import androidx.security.crypto.MasterKey
import dagger.Module
import dagger.Provides

@Module
class EncryptionModule {

    @Provides
    fun provideMasterKey(app: Application) = MasterKey.Builder(app)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .setRequestStrongBoxBacked(true)
        .build()

}