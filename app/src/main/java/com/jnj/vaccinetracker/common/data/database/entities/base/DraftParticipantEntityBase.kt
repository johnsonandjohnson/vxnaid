package com.jnj.vaccinetracker.common.data.database.entities.base

import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity

interface DraftParticipantEntityBase : ParticipantEntityBase, UploadableDraft {
    val registrationDate: DateEntity
}