package com.jnj.vaccinetracker.common.di

import com.jnj.vaccinetracker.common.data.database.ParticipantRoomDatabase
import com.jnj.vaccinetracker.common.data.database.daos.*
import com.jnj.vaccinetracker.common.data.database.daos.draft.*
import com.jnj.vaccinetracker.common.data.database.transaction.ParticipantDbTransactionRunner
import com.jnj.vaccinetracker.common.data.database.transaction.ParticipantRoomDbTransactionRunnerImpl
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
class DatabaseModule {

    @Provides
    fun provideTransactionRunner(impl: ParticipantRoomDbTransactionRunnerImpl): ParticipantDbTransactionRunner = impl

    @Provides
    @Singleton
    fun provideDb(factory: ParticipantRoomDatabase.Factory): ParticipantRoomDatabase {
        return runBlocking {
            factory.createDatabaseWithDefaultPassphrase(deleteDatabaseIfCorrupt = true)
        }
    }

    @Provides
    fun provideParticipantDao(db: ParticipantRoomDatabase): ParticipantDao = db.participantDao()

    @Provides
    fun provideParticipantBiometricsTemplateDao(db: ParticipantRoomDatabase): ParticipantBiometricsTemplateDao = db.participantBiometricsTemplateDao()

    @Provides
    fun provideParticipantImageDao(db: ParticipantRoomDatabase): ParticipantImageDao = db.participantImageDao()

    @Provides
    fun provideDraftParticipantBiometricsTemplateDao(db: ParticipantRoomDatabase): DraftParticipantBiometricsTemplateDao = db.draftParticipantBiometricsTemplateDao()

    @Provides
    fun provideDraftParticipantImageDao(db: ParticipantRoomDatabase): DraftParticipantImageDao = db.draftParticipantImageDao()

    @Provides
    fun provideParticipantAddressDao(db: ParticipantRoomDatabase): ParticipantAddressDao = db.participantAddressDao()

    @Provides
    fun provideParticipantAttributeDao(db: ParticipantRoomDatabase): ParticipantAttributeDao = db.participantAttributeDao()

    @Provides
    fun provideVisitDao(db: ParticipantRoomDatabase): VisitDao = db.visitDao()

    @Provides
    fun provideVisitAttributeDao(db: ParticipantRoomDatabase): VisitAttributeDao = db.visitAttributeDao()

    @Provides
    fun provideVisitObservationDao(db: ParticipantRoomDatabase): VisitObservationDao = db.visitObservationDao()

    @Provides
    fun provideDraftParticipantDao(db: ParticipantRoomDatabase): DraftParticipantDao = db.draftParticipantDao()

    @Provides
    fun provideDraftParticipantAddressDao(db: ParticipantRoomDatabase): DraftParticipantAddressDao = db.draftParticipantAddressDao()

    @Provides
    fun provideDraftParticipantAttributeDao(db: ParticipantRoomDatabase): DraftParticipantAttributeDao = db.draftParticipantAttributeDao()

    @Provides
    fun provideDraftVisitDao(db: ParticipantRoomDatabase): DraftVisitDao = db.draftVisitDao()

    @Provides
    fun provideDraftVisitAttributeDao(db: ParticipantRoomDatabase): DraftVisitAttributeDao = db.draftVisitAttributeDao()

    @Provides
    fun provideDraftVisitEncounterDao(db: ParticipantRoomDatabase): DraftVisitEncounterDao = db.draftVisitEncounterDao()

    @Provides
    fun provideDraftVisitEncounterObservationDao(db: ParticipantRoomDatabase): DraftVisitEncounterObservationDao = db.draftVisitEncounterObservationDao()

    @Provides
    fun provideDraftVisitEncounterAttributeDao(db: ParticipantRoomDatabase): DraftVisitEncounterAttributeDao = db.draftVisitEncounterAttributeDao()

    @Provides
    fun provideSyncScopeDao(db: ParticipantRoomDatabase): SyncScopeDao = db.syncScopeDao()

    @Provides
    fun provideOperatorCredentialsDao(db: ParticipantRoomDatabase): OperatorCredentialsDao = db.operatorCredentialsDao()

    @Provides
    fun provideSyncErrorDao(db: ParticipantRoomDatabase): SyncErrorDao = db.syncErrorDao()

    @Provides
    fun provideFailedParticipantSyncRecordDownloadDao(db: ParticipantRoomDatabase): FailedParticipantSyncRecordDownloadDao = db.failedParticipantDownloadDao()

    @Provides
    fun provideFailedBiometricsTemplateSyncRecordDownloadDao(db: ParticipantRoomDatabase): FailedBiometricsTemplateSyncRecordDownloadDao = db.failedBiometricsTemplateDownloadDao()

    @Provides
    fun provideFailedImageSyncRecordDownloadDao(db: ParticipantRoomDatabase): FailedImageSyncRecordDownloadDao = db.failedImageDownloadDao()

    @Provides
    fun provideFailedVisitSyncRecordDownloadDao(db: ParticipantRoomDatabase): FailedVisitSyncRecordDownloadDao = db.failedVisitDownloadDao()


    @Provides
    fun provideDeletedParticipantDao(db: ParticipantRoomDatabase): DeletedParticipantDao = db.deletedParticipantDownloadDao()

    @Provides
    fun provideDeletedBiometricsTemplateDao(db: ParticipantRoomDatabase): DeletedBiometricsTemplateDao = db.deletedBiometricsTemplateDownloadDao()

    @Provides
    fun provideDeletedImageDao(db: ParticipantRoomDatabase): DeletedImageDao = db.deletedImageDownloadDao()

    @Provides
    fun provideDeletedVisitDao(db: ParticipantRoomDatabase): DeletedVisitDao = db.deletedVisitDownloadDao()
}