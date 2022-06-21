package com.jnj.vaccinetracker.common.domain.entities

enum class DraftState(val code: String) {
    UPLOAD_PENDING("pending_upload"), UPLOADED("uploaded");

    companion object {
        fun fromCode(code: String) = values().find { it.code == code }
        fun initialState() = UPLOAD_PENDING
    }

    fun isPendingUpload() = this == UPLOAD_PENDING
    fun isUploaded() = this == UPLOADED
}