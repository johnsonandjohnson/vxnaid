package com.jnj.vaccinetracker.di

import com.jnj.vaccinetracker.VaccineTrackerApplication
import com.jnj.vaccinetracker.common.data.database.ParticipantRoomDatabaseTest
import com.jnj.vaccinetracker.common.di.*
import com.jnj.vaccinetracker.common.domain.usecases.GetServerHealthUseCaseTest
import com.jnj.vaccinetracker.performancetest.GetVisitDetailsPerformanceTest
import com.jnj.vaccinetracker.performancetest.MatchParticipantsPerformanceTest
import com.jnj.vaccinetracker.syncexecutionscript.DraftParticipantScript
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import javax.inject.Singleton

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
interface TestDaggerComponent : AppComponent {
    fun inject(testScript: DraftParticipantScript)
    fun inject(testScript: GetVisitDetailsPerformanceTest)
    fun inject(testScript: MatchParticipantsPerformanceTest)
    fun inject(test: GetServerHealthUseCaseTest)
    fun inject(test: ParticipantRoomDatabaseTest)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: VaccineTrackerApplication): Builder

        fun build(): TestDaggerComponent
    }
}