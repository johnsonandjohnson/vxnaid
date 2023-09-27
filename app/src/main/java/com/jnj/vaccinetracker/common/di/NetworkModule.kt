package com.jnj.vaccinetracker.common.di

import android.app.Application
import com.icapps.niddler.core.AndroidNiddler
import com.icapps.niddler.core.Niddler
import com.icapps.niddler.interceptor.okhttp.NiddlerOkHttpInterceptor
import com.jnj.vaccinetracker.common.data.network.BaseUrlInterceptor
import com.jnj.vaccinetracker.common.data.network.DeviceIdInterceptor
import com.jnj.vaccinetracker.common.data.network.VaccineTrackerApiService
import com.jnj.vaccinetracker.common.data.network.apiexceptioninterceptor.MainApiExceptionInterceptor
import com.jnj.vaccinetracker.common.data.network.apiexceptioninterceptor.SyncApiExceptionInterceptor
import com.jnj.vaccinetracker.common.data.repositories.CookieRepository
import com.jnj.vaccinetracker.common.data.repositories.MainCookieRepository
import com.jnj.vaccinetracker.common.data.repositories.SyncCookieRepository
import com.jnj.vaccinetracker.common.di.qualifiers.MainApi
import com.jnj.vaccinetracker.common.di.qualifiers.SyncApi
import com.jnj.vaccinetracker.common.helpers.seconds
import com.jnj.vaccinetracker.config.appSettings
import com.jnj.vaccinetracker.sync.data.json.adapters.SyncDateJsonAdapter
import com.jnj.vaccinetracker.sync.data.models.SyncDate
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import dagger.Module
import dagger.Provides
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * @author maartenvangiel
 * @version 1
 */
@Module(includes = [ApiDataSourceModule::class])
class NetworkModule {

    private fun loggingInterceptor() =
        HttpLoggingInterceptor().also { it.level = HttpLoggingInterceptor.Level.BODY }

    @Provides
    @Singleton
    fun provideNiddler(application: Application): Niddler {
        val niddler = AndroidNiddler.Builder()
            .setPort(0)
            .setNiddlerInformation(AndroidNiddler.fromApplication(application))
            .build()
        niddler.attachToApplication(application)
        return niddler
    }

    @Provides
    @Singleton
    @Named("NiddlerInterceptor")
    fun provideNiddlerInterceptor(niddler: Niddler): Interceptor {
        return NiddlerOkHttpInterceptor(niddler, "NiddlerInterceptor")
    }

    private inline fun <T : Any> T.runIfLoggingEnabled(block: T.() -> T): T {
        return if (appSettings.logConfig.consoleLoggingEnabled)
            return block()
        else
            this
    }


    @Provides
    @Singleton
    @MainApi
    fun provideOkHttpClientMain(
        @Named("NiddlerInterceptor") niddlerInterceptor: Interceptor,
        apiExceptionInterceptor: MainApiExceptionInterceptor,
        baseUrlInterceptor: BaseUrlInterceptor,
        deviceIdInterceptor: DeviceIdInterceptor,
        @MainApi
        cookieRepository: CookieRepository,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(baseUrlInterceptor)
            .addInterceptor(deviceIdInterceptor)
            .runIfLoggingEnabled {
                addInterceptor(loggingInterceptor())
                    .addInterceptor(niddlerInterceptor)
            }
            .addInterceptor(apiExceptionInterceptor)
            .cookieJar(cookieRepository)
            .build()
    }

    @Provides
    @Singleton
    @SyncApi
    fun provideOkHttpClientSync(
        @Named("NiddlerInterceptor") niddlerInterceptor: Interceptor,
        apiExceptionInterceptor: SyncApiExceptionInterceptor,
        baseUrlInterceptor: BaseUrlInterceptor,
        deviceIdInterceptor: DeviceIdInterceptor,
        @SyncApi
        cookieRepository: CookieRepository,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(baseUrlInterceptor)
            .addInterceptor(deviceIdInterceptor)
            .connectTimeout(SYNC_READ_TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(SYNC_READ_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(SYNC_READ_TIMEOUT, TimeUnit.MILLISECONDS)
            .runIfLoggingEnabled {
                addInterceptor(loggingInterceptor())
                    .addInterceptor(niddlerInterceptor)
            }
            .addInterceptor(apiExceptionInterceptor)
            .cookieJar(cookieRepository)
            .build()
    }


    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(Date::class.java, Rfc3339DateJsonAdapter())
            .add(SyncDate::class.java, SyncDateJsonAdapter().serializeNulls())
            .build()
    }

    @Provides
    @Singleton
    fun provideVaccineTrackerApiServiceService(
        @MainApi
        okHttpClient: OkHttpClient,
    ): VaccineTrackerApiService {
        return createRetrofit(provideMoshi(), okHttpClient).create(VaccineTrackerApiService::class.java)
    }


    @Provides
    @Singleton
    fun provideVaccineTrackerSyncApiService(
        @SyncApi
        okHttpClient: OkHttpClient,
    ): VaccineTrackerSyncApiService {
        return createRetrofit(provideMoshi(), okHttpClient).create(VaccineTrackerSyncApiService::class.java)
    }

    @Provides
    @SyncApi
    fun provideSyncCookieRepository(
        impl: SyncCookieRepository,
    ): CookieRepository = impl

    @Provides
    @MainApi
    fun provideMainCookieRepository(
        impl: MainCookieRepository,
    ): CookieRepository = impl

    companion object {
        private fun createRetrofit(moshi: Moshi, okHttpClient: OkHttpClient): Retrofit {
            return Retrofit.Builder()
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .baseUrl(BaseUrlInterceptor.BASE_URL)
                .client(okHttpClient)
                .build()
        }

        private val SYNC_READ_TIMEOUT = 30.seconds
    }
}

