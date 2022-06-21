package com.jnj.vaccinetracker.barcode

fun formatBarcode(barcode: String): String {
    return barcode.filter { !it.isWhitespace() }.toCharArray().let { String(it) }
}

fun formatParticipantId(participantId: String): String = formatBarcode(participantId)