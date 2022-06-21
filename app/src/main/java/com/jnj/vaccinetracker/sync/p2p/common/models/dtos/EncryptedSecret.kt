package com.jnj.vaccinetracker.sync.p2p.common.models.dtos

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EncryptedSecret(
    /**
     * base64
     */
    val secret: String,
    /**
     * base64
     */
    val salt: String,
    /**
     * base64
     */
    val iv: String
)