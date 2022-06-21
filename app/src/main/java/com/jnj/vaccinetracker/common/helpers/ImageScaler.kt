package com.jnj.vaccinetracker.common.helpers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.min

object ImageScaler {

    private const val JPEG_COMPRESSION_QUALITY = 40

    fun scaleDownAndRotate(photo: ByteArray, maxHeight: Int, maxWidth: Int): ByteArray {
        val decodedBitmap = BitmapFactory.decodeByteArray(photo, 0, photo.size)
        val bmOriginalWidth = decodedBitmap.width
        val bmOriginalHeight = decodedBitmap.height
        val originalWidthToHeightRatio = 1.0 * bmOriginalWidth / bmOriginalHeight
        val originalHeightToWidthRatio = 1.0 * bmOriginalHeight / bmOriginalWidth
        val scaledBitmap = scaleBitmap(decodedBitmap, bmOriginalWidth, bmOriginalHeight, originalWidthToHeightRatio, originalHeightToWidthRatio, maxHeight, maxWidth)
        val rotatedBitmap = rotateBitmap(photo, scaledBitmap)
        val bytes = ByteArrayOutputStream()
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_COMPRESSION_QUALITY, bytes)
        return bytes.toByteArray()
    }

    @SuppressWarnings("LongParameterList")
    private fun scaleBitmap(
        bm: Bitmap, bmOriginalWidth: Int, bmOriginalHeight: Int, originalWidthToHeightRatio: Double, originalHeightToWidthRatio: Double, maxHeight: Int,
        maxWidth: Int,
    ): Bitmap {
        if (bmOriginalWidth > maxWidth || bmOriginalHeight > maxHeight) {
            if (bmOriginalWidth > bmOriginalHeight) {
                return scaleDownFromWidth(bm, maxWidth, bmOriginalWidth, originalHeightToWidthRatio)
            } else if (bmOriginalHeight > bmOriginalWidth) {
                return scaleDownFromHeight(bm, maxHeight, bmOriginalHeight, originalWidthToHeightRatio)
            }
        }
        return bm
    }

    private fun scaleDownFromHeight(bm: Bitmap, maxHeight: Int, bmOriginalHeight: Int, originalWidthToHeightRatio: Double): Bitmap {
        val newHeight = min(maxHeight, bmOriginalHeight)
        val newWidth = (newHeight * originalWidthToHeightRatio).toInt()
        return Bitmap.createScaledBitmap(bm, newWidth, newHeight, true)
    }

    private fun scaleDownFromWidth(bm: Bitmap, maxWidth: Int, bmOriginalWidth: Int, originalHeightToWidthRatio: Double): Bitmap {
        val newWidth = min(maxWidth, bmOriginalWidth)
        val newHeight = (newWidth * originalHeightToWidthRatio).toInt()
        return Bitmap.createScaledBitmap(bm, newWidth, newHeight, true)
    }

    private fun rotateBitmap(photoBytes: ByteArray, scaledBitmap: Bitmap): Bitmap {
        val exifInterface: ExifInterface
        exifInterface = try {
            ExifInterface(ByteArrayInputStream(photoBytes, 0, photoBytes.size))
        } catch (e: IOException) {
            logError("Failed to initialize exif interface", e)
            return scaledBitmap
        }
        val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        return rotateBitmapUsingExifOrientation(scaledBitmap, orientation)
    }

    @SuppressWarnings("MagicNumber")
    private fun rotateBitmapUsingExifOrientation(originalBitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            ExifInterface.ORIENTATION_NORMAL -> return originalBitmap
            else -> return originalBitmap
        }
        return try {
            val bmRotated = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
            originalBitmap.recycle()
            bmRotated
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            originalBitmap
        }
    }
}