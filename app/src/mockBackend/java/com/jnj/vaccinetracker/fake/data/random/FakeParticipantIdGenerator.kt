package com.jnj.vaccinetracker.fake.data.random

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeParticipantIdGenerator @Inject constructor() {
    var count = 1

    @Synchronized
    fun generateId(): String = "pa-${count++}"
}