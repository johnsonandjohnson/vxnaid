package com.jnj.vaccinetracker.sync.domain.entities

private const val DEFAULT_LIMIT = 50

data class FindAllSyncErrorMetadata(val offset: Int, val limit: Int = DEFAULT_LIMIT)