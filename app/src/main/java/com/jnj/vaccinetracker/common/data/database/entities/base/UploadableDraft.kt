package com.jnj.vaccinetracker.common.data.database.entities.base

import com.jnj.vaccinetracker.common.domain.entities.DraftState

interface UploadableDraft {
    val draftState: DraftState

    companion object {
        const val COL_DRAFT_STATE = "draftState"
    }
}