package com.jnj.vaccinetracker.common.di

import android.content.Context
import android.content.SharedPreferences
import com.jnj.vaccinetracker.common.di.qualifiers.SyncLoggerPrefs
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class SyncLoggerModule {

    companion object {
        const val SYNC_LOG_PREFS = "sync_log.prefs"
    }

    @Provides
    @Singleton
    @SyncLoggerPrefs
    fun provideSyncLogPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(SYNC_LOG_PREFS, Context.MODE_PRIVATE)
    }

}