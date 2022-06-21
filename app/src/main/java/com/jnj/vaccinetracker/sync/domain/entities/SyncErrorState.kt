package com.jnj.vaccinetracker.sync.domain.entities

enum class SyncErrorState(val code: Int) {
    /**
     * [initialState] must be uploaded to the backend
     */
    PENDING_UPLOAD(0),

    /**
     * means the error is not yet resolved but the backend received it
     */
    UPLOADED(1),

    /**
     * means the error should be send to the backend as resolved. After the backend received it, it can be safely deleted.
     */
    RESOLVED(2);

    companion object {
        fun initialState() = PENDING_UPLOAD
        fun statesNotResolved() = values().filter { it != RESOLVED }
        fun fromCode(code: Int) = values().find { it.code == code }
    }
}