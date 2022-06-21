package com.jnj.vaccinetracker.common.data.helpers

import android.content.Context
import android.net.Uri
import android.os.StatFs
import androidx.core.content.FileProvider
import com.jnj.vaccinetracker.common.helpers.mb
import java.io.File
import java.io.InputStream
import javax.inject.Inject

class AndroidFiles @Inject constructor(private val app: Context) {
    val externalFiles: File by lazy {
        requireNotNull(app.getExternalFilesDir(null)) { "app externalFilesDir must not be null" }
    }
    val cacheDir: File get() = requireNotNull(app.cacheDir) { "app cacheDir must not be null" }
    fun openAsset(fileName: String): InputStream = app.resources.assets.open(fileName)
    fun getUriForFile(filePath: File): Uri = FileProvider.getUriForFile(app, "${app.packageName}.provider", filePath)!!

    private fun calcFreeSpace(): Long {
        return StatFs(externalFiles.path).availableBytes
    }

    fun isOutOfDiskSpace(): Boolean {
        return calcFreeSpace() < OUT_OF_DISK_SPACE_THRESHOLD
    }

    companion object {
        private val OUT_OF_DISK_SPACE_THRESHOLD = 10.mb
    }
}