package com.jnj.vaccinetracker.common.data.database.entities.base

import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity

interface UploadableDraftWithDate : UploadableDraft {
    val dateLastUploadAttempt: DateEntity?
}