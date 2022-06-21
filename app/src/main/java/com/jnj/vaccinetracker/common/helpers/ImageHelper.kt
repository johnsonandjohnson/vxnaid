package com.jnj.vaccinetracker.common.helpers

import com.jnj.vaccinetracker.common.domain.entities.ImageBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext


/**
 * @author maartenvangiel
 * @version 1
 */
object ImageHelper {

    private const val MAX_IMAGE_SIZE_PX = 200

    suspend fun compressRawImage(rawImageBytes: ImageBytes, context: CoroutineContext = Dispatchers.IO): ImageBytes = withContext(context) {
        // Scale down and rotate the image (because backend strips exif information without fixing the rotation)
        ImageBytes(ImageScaler.scaleDownAndRotate(rawImageBytes.bytes, MAX_IMAGE_SIZE_PX, MAX_IMAGE_SIZE_PX))
    }

}