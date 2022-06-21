package com.jnj.vaccinetracker.common.data.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import com.jnj.vaccinetracker.common.data.database.daos.base.DaoBase
import com.jnj.vaccinetracker.common.data.database.daos.base.ObservableDao
import com.jnj.vaccinetracker.common.data.database.daos.base.SyncDao
import com.jnj.vaccinetracker.common.data.database.daos.base.VisitDaoBase
import com.jnj.vaccinetracker.common.data.database.entities.VisitAttributeEntity
import com.jnj.vaccinetracker.common.data.database.entities.VisitEntity
import com.jnj.vaccinetracker.common.data.database.entities.VisitObservationEntity
import com.jnj.vaccinetracker.common.data.database.models.RoomDateModifiedOccurrenceModel
import com.jnj.vaccinetracker.common.data.database.models.RoomVisitModel
import com.jnj.vaccinetracker.common.data.database.models.delete.RoomDeleteVisitModel
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao : VisitDaoBase<VisitEntity, RoomVisitModel>, ObservableDao, SyncDao {

    @Query("select * from visit where participantUuid=:participantUuid")
    @Transaction
    override suspend fun findAllByParticipantUuid(participantUuid: String): List<RoomVisitModel>

    @Query("select * from visit where visitUuid=:visitUuid")
    @Transaction
    override suspend fun findByVisitUuid(visitUuid: String): RoomVisitModel?

    @Query("select visitUuid as uuid, dateModified from visit where dateModified = (select max(dateModified) from visit)")
    override suspend fun findMostRecentDateModifiedOccurrence(): List<RoomDateModifiedOccurrenceModel>

    @Query("select count(*) from visit")
    override fun observeChanges(): Flow<Long>

    @Query("select count(*) from visit where visitType=:visitType")
    suspend fun countByVisitType(visitType: String): Long

    @Delete(entity = VisitEntity::class)
    override suspend fun delete(deleteVisitModel: RoomDeleteVisitModel): Int

    @Query("delete from visit")
    override suspend fun deleteAll()
}

@Dao
interface VisitAttributeDao : DaoBase<VisitAttributeEntity>, ObservableDao {

    @Query("select count(*) from visit_attribute")
    override fun observeChanges(): Flow<Long>
}

@Dao
interface VisitObservationDao : DaoBase<VisitObservationEntity>, ObservableDao {

    @Query("select count(*) from visit_observation")
    override fun observeChanges(): Flow<Long>
}