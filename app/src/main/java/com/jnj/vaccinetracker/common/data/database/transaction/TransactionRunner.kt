package com.jnj.vaccinetracker.common.data.database.transaction

interface TransactionRunner {
    suspend fun <R> withTransaction(block: suspend () -> R): R
}