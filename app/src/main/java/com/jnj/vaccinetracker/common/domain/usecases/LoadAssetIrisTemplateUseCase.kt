package com.jnj.vaccinetracker.common.domain.usecases

import com.jnj.vaccinetracker.common.data.helpers.AndroidFiles
import com.jnj.vaccinetracker.common.data.models.IrisPosition
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.neurotec.biometrics.NEPosition
import com.neurotec.biometrics.NIris
import com.neurotec.images.NImage
import com.neurotec.images.NImageFormat
import com.neurotec.io.NBuffer
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject

class LoadAssetIrisTemplateUseCase @Inject constructor(private val androidFiles: AndroidFiles, private val dispatchers: AppCoroutineDispatchers) {
    companion object {
        private const val FILE_NAME_FORMAT = "iris_image_b%s.jpg"
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun loadIris(irisPosition: IrisPosition): NIris = withContext(dispatchers.io) {
        val fileName = FILE_NAME_FORMAT.format(irisPosition.position)
        val ims: InputStream = androidFiles.openAsset(fileName)
        val bytes = ims.use { it.readBytes() }
        createIrisFromImage(bytes)
    }

    /**
     * Creates an NIris from a JPEG ByteArray and passes this object to a callback function.
     *
     * @param   data                ByteArray representing a JPEG image
     */
    private suspend fun createIrisFromImage(data: ByteArray): NIris {
        return imageFromJPEG(data).let { image ->
            NIris().also {
                it.image = image
                it.position = NEPosition.UNKNOWN
            }
        }
    }

    /**
     * Creates an NImage from a JPEG ByteArray
     *
     * @param   data    ByteArray representing a JPEG image
     * @return          NImage from the ByteArray
     */
    private suspend fun imageFromJPEG(data: ByteArray): NImage = withContext(dispatchers.io) {
        val srcPixels = NBuffer.fromArray(data)
        NImage.fromMemory(srcPixels, NImageFormat.getJPEG())
    }

}