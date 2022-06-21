package com.jnj.vaccinetracker.common.data.database.entities.base

interface ImageBase {
    val imageFileName: String?

    companion object {
        const val COL_IMAGE_FILE_NAME = "imageFileName"
    }
}