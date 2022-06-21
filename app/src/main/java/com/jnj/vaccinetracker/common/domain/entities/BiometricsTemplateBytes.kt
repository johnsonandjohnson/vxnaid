package com.jnj.vaccinetracker.common.domain.entities

import com.neurotec.io.NBuffer

class BiometricsTemplateBytes(val bytes: ByteArray)

fun BiometricsTemplateBytes.toNBuffer(): NBuffer = NBuffer.fromArray(bytes)