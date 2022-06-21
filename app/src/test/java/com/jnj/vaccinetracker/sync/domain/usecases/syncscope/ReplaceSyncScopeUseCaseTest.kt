package com.jnj.vaccinetracker.sync.domain.usecases.syncscope

import FakeTransactionRunner
import com.jnj.vaccinetracker.common.data.database.repositories.SyncScopeRepository
import com.jnj.vaccinetracker.common.data.database.transaction.ParticipantDbTransactionRunner
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.domain.usecases.masterdata.GetSitesUseCase
import com.jnj.vaccinetracker.common.ui.plus
import com.jnj.vaccinetracker.sync.data.models.SyncScopeLevel
import com.jnj.vaccinetracker.sync.domain.entities.SyncScope
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.util.*

class ReplaceSyncScopeUseCaseTest : FunSpec({

    val syncScopeRepository: SyncScopeRepository = syncScopeRegistry()
    val deleteAllImagesUseCase: DeleteAllImagesUseCase = mockk(relaxUnitFun = true)
    val deleteAllParticipantsUseCase: DeleteAllParticipantsUseCase = mockk(relaxUnitFun = true)
    val deleteAllBiometricsTemplatesUseCase: DeleteAllBiometricsTemplatesUseCase = mockk(relaxUnitFun = true)
    val deleteAllVisitsUseCase: DeleteAllVisitsUseCase = mockk(relaxUnitFun = true)
    val getSitesUseCase: GetSitesUseCase = mockk()
    val transactionRunner: ParticipantDbTransactionRunner = FakeTransactionRunner()
    val replaceSyncScopeUseCase = ReplaceSyncScopeUseCase(
        syncScopeRepository,
        deleteAllImagesUseCase,
        deleteAllBiometricsTemplatesUseCase,
        deleteAllVisitsUseCase,
        deleteAllParticipantsUseCase,
        transactionRunner,
    )

    lateinit var newSyncScope: SyncScope
    // to make sync scope date created unique
    var dateCreatedCounter = 1L

    fun syncScope(siteUuid: String, level: SyncScopeLevel, country: String? = null, cluster: String? = null, dateCreated: Date = dateNow() + (++dateCreatedCounter)) =
        SyncScope(siteUuid, level, country, cluster, dateCreated)

    suspend fun initSyncScopes(a: SyncScope, b: SyncScope) {
        println("initSyncScopes")
        syncScopeRepository.insert(a)
        syncScopeRepository.findOne() shouldBe a
        newSyncScope = b
    }

    fun verifyWipedLocalData(deleteUploadedDrafts: Boolean = true, deleteImages: Boolean, deleteImagesOnly: Boolean = false) {
        if (deleteImages) {
            coVerify {
                deleteAllImagesUseCase.deleteAllImages(deleteUploadedDrafts)
            }
        }
        if (!deleteImagesOnly) {
            coVerifyAll {
                deleteAllBiometricsTemplatesUseCase.deleteAllBiometricsTemplates(deleteUploadedDrafts)
                deleteAllParticipantsUseCase.deleteAllParticipants(deleteUploadedDrafts)
                deleteAllVisitsUseCase.deleteAllVisits(deleteUploadedDrafts)
            }
        }

        confirmVerified(deleteAllImagesUseCase, deleteAllBiometricsTemplatesUseCase, deleteAllParticipantsUseCase, deleteAllVisitsUseCase)
    }

    fun verifyNothingWiped() = verifyWipedLocalData(deleteImages = false, deleteImagesOnly = true)

    suspend fun verifyInsertedSyncScope() {
        syncScopeRepository.findOne() shouldBe newSyncScope
    }

    fun syncScopeMatrix(changesOnly: Boolean) = SyncScopeLevel.values().map { currentLevel ->
        SyncScopeLevel.values().filter {
            if (changesOnly) {
                it != currentLevel
            } else true
        }.map { newLevel ->
            currentLevel to newLevel
        }
    }.flatten()

    context("given existing sync scope exists") {
        context("given sync scope level changed") {
            syncScopeMatrix(changesOnly = true).forEach { (currentLevel, newLevel) ->
                test("from $currentLevel to $newLevel and siteUuid not changed then wipe all local data expect images") {
                    // Arrange
                    initSyncScopes(syncScope("1", currentLevel), syncScope("1", newLevel))
                    //Act
                    replaceSyncScopeUseCase.replaceSyncScope(newSyncScope)
                    // Assert
                    verifyWipedLocalData(deleteImages = false)
                }

                test("from $currentLevel to $newLevel and siteUuid changed then wipe all local data") {
                    // Arrange
                    initSyncScopes(syncScope("1", currentLevel), syncScope("2", newLevel))
                    //Act
                    replaceSyncScopeUseCase.replaceSyncScope(newSyncScope)
                    // Assert
                    verifyWipedLocalData(deleteImages = true)
                }
            }
        }
        context("given sync scope level not changed") {
            SyncScopeLevel.values().forEach { level ->
                data class TestData(
                    val existingSyncScope: SyncScope, val newSyncScope: SyncScope,
                    val description: String, val siteUuidChanged: Boolean,
                )
                listOf(
                    TestData(
                        existingSyncScope = syncScope("1", level = level, country = "A", cluster = "cluster-b"),
                        newSyncScope = syncScope("1", level = level, country = "B", cluster = "cluster-b"),
                        description = "different country", siteUuidChanged = false,
                    ),
                    TestData(
                        existingSyncScope = syncScope("1", level = level, country = "A", cluster = "cluster-a"),
                        newSyncScope = syncScope("1", level = level, country = "A", cluster = "cluster-b"),
                        description = "different cluster", siteUuidChanged = false,
                    ),
                    TestData(
                        existingSyncScope = syncScope("1", level = level, country = "A", cluster = "cluster-a"),
                        newSyncScope = syncScope("1", level = level, country = "B", cluster = "cluster-b"),
                        description = "different cluster and different country", siteUuidChanged = false,
                    ),
                    TestData(
                        existingSyncScope = syncScope("1", level = level, country = "A", cluster = "cluster-b"),
                        newSyncScope = syncScope("2", level = level, country = "B", cluster = "cluster-b"),
                        description = "different country and different siteUuid", siteUuidChanged = true,
                    ),
                    TestData(
                        existingSyncScope = syncScope("1", level = level, country = "A", cluster = "cluster-a"),
                        newSyncScope = syncScope("2", level = level, country = "A", cluster = "cluster-b"),
                        description = "different cluster and different siteUuid", siteUuidChanged = true,
                    ),
                ).forEach { (existingSyncScope, newSyncScope, description, siteUuidChanged) ->
                    context("and sync scope level is $level with $description") {
                        val syncScopeWithinBounds = existingSyncScope.isWithinBounds(newSyncScope)
                        if (syncScopeWithinBounds) {
                            context("and sync scope within bounds") {
                                if (siteUuidChanged) {
                                    test("then only delete images") {
                                        // Arrange
                                        initSyncScopes(existingSyncScope, newSyncScope)
                                        // Act
                                        replaceSyncScopeUseCase.replaceSyncScope(newSyncScope)
                                        // Assert
                                        verifyWipedLocalData(deleteImages = true, deleteImagesOnly = siteUuidChanged)
                                        verifyInsertedSyncScope()
                                    }
                                } else {
                                    test("then don't do any migrations except sync scope with old date created") {
                                        // Arrange
                                        initSyncScopes(existingSyncScope, newSyncScope)
                                        // Act
                                        replaceSyncScopeUseCase.replaceSyncScope(newSyncScope)
                                        // Assert
                                        // verify nothing was wiped
                                        verifyNothingWiped()
                                        // verify new sync scope with existing data created was inserted
                                        syncScopeRepository.findOne() shouldBe newSyncScope.copy(dateCreated = existingSyncScope.dateCreated)
                                    }
                                }
                            }
                        } else {
                            test("and sync scope not within bounds then wipe all local data except images") {
                                // Arrange
                                initSyncScopes(existingSyncScope, newSyncScope)
                                // Act
                                replaceSyncScopeUseCase.replaceSyncScope(newSyncScope)
                                // Assert
                                verifyWipedLocalData(deleteImages = siteUuidChanged)
                                verifyInsertedSyncScope()
                            }
                        }

                    }
                }
            }
        }
        context("given existing sync scope does not exists") {
            test("then insert sync scope without migration") {
                // Arrange
                newSyncScope = syncScope("1", SyncScopeLevel.SITE)
                // Act
                replaceSyncScopeUseCase.replaceSyncScope(newSyncScope)
                // Assert
                coVerifyAll(inverse = true) {
                    deleteAllImagesUseCase.deleteAllImages(any())
                    deleteAllBiometricsTemplatesUseCase.deleteAllBiometricsTemplates(any())
                    deleteAllParticipantsUseCase.deleteAllParticipants(any())
                    deleteAllVisitsUseCase.deleteAllVisits(any())
                    getSitesUseCase.getMasterData()
                }
                confirmVerified(deleteAllImagesUseCase, deleteAllBiometricsTemplatesUseCase, deleteAllParticipantsUseCase, deleteAllVisitsUseCase, getSitesUseCase)
            }
            afterTest {
                verifyInsertedSyncScope()
            }
        }
    }


})

private fun syncScopeRegistry(): SyncScopeRepository {
    val repo: SyncScopeRepository = mockk()
    var syncScopeRecord: SyncScope? = null
    coEvery { repo.findOne() } coAnswers {
        syncScopeRecord
    }
    coEvery { repo.deleteExisting() } coAnswers {
        println("deleteExisting")
        require(syncScopeRecord != null) { "syncScopeRecord must not be null when deleting existing" }
        syncScopeRecord = null
        1
    }
    coEvery { repo.insert(any()) } coAnswers { inv ->
        require(syncScopeRecord == null) { "syncScopeRecord must be null before inserting syncScope" }
        val syncScope = inv.invocation.args[0] as SyncScope
        println("insert $syncScope")
        syncScopeRecord = syncScope
    }
    return repo
}

