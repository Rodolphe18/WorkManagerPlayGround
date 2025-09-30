package com.francotte.workmanagerplayground

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import androidx.core.net.toUri

// DownloadImageWorker.kt
@HiltWorker
class DownloadImageWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val source = inputData.getString(Keys.KEY_IMAGE_URL) ?: return@withContext Result.failure()
        try {
            val uri = source.toUri()
            val bitmap: Bitmap? = when (uri.scheme?.lowercase()) {
                // HTTP(S) avec timeouts + User-Agent
                "http", "https" -> downloadBitmapFromHttp(source)
                // Partage depuis galerie / fichiers
                "content", "file" -> applicationContext.contentResolver
                    .openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                else -> null
            }

            bitmap ?: return@withContext Result.failure()

            val fileName = "shared_${System.currentTimeMillis()}.jpg"
            val outFile = File(applicationContext.filesDir, fileName)
            FileOutputStream(outFile).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }

            Result.success(
                workDataOf(Keys.KEY_DOWNLOADED_PATH to outFile.absolutePath)
            )
        } catch (t: Throwable) {
            t.printStackTrace()
            // Si c’est une URL en cleartext (http) non autorisée, ça plantera ici.
            // Tu peux soit passer à https, soit autoriser le cleartext (voir note plus bas).
            Result.retry()
        }
    }

    private fun downloadBitmapFromHttp(urlStr: String): Bitmap? {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "WorkManager-Image/1.0")
        }
        return try {
            conn.inputStream.use { BitmapFactory.decodeStream(it) }
        } finally {
            conn.disconnect()
        }
    }
}