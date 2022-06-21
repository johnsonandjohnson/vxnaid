package com.jnj.vaccinetracker.common.data.database.transaction

import androidx.room.withTransaction
import com.jnj.vaccinetracker.common.data.database.ParticipantRoomDatabase
import javax.inject.Inject

interface ParticipantDbTransactionRunner : TransactionRunner

class ParticipantRoomDbTransactionRunnerImpl @Inject constructor(private val db: ParticipantRoomDatabase) : ParticipantDbTransactionRunner {
    override suspend fun <R> withTransaction(block: suspend () -> R): R {
        return db.withTransaction(block)
    }
}