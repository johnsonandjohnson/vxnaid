package com.jnj.vaccinetracker.sync.domain.usecases

import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.logWarn
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class MakeParticipantIdUniqueUseCase @Inject constructor() {

    companion object {
        private const val SEP = "~"
        private val DATE_FORMAT = SimpleDateFormat("ddHHmm", Locale.ENGLISH)
        private const val MAX_LENGTH = 50
    }

    fun makeParticipantIdUnique(participantId: String): String {
        val dateSuffix = DATE_FORMAT.format(Date())
        val newParticipantId = "$participantId$SEP$dateSuffix"
        if (newParticipantId.length > MAX_LENGTH) {
            logWarn("makeParticipantIdUnique participantId $newParticipantId is too long (${newParticipantId.length}). Max length is $MAX_LENGTH. Truncating last char")
            return makeParticipantIdUnique(participantId.dropLast(1))
        }
        logInfo("makeParticipantIdUnique: $participantId -> $newParticipantId")
        return newParticipantId
    }
}