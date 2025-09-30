package com.francotte.workmanagerplayground

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
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
import androidx.core.graphics.createBitmap

@HiltWorker
class RedFilterWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(appContext, params) {


    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        val path = inputData.getString(Keys.KEY_DOWNLOADED_PATH)
            ?: return@withContext Result.failure()
        try {
            val original = BitmapFactory.decodeFile(path) ?: return@withContext Result.failure()


            // Crée une copie et applique un voile rouge
            val filtered = createBitmap(original.width, original.height)
            val canvas = Canvas(filtered)
            canvas.drawBitmap(original, 0f, 0f, null)


            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.RED
                alpha = 96
            }
            canvas.drawRect(0f, 0f, original.width.toFloat(), original.height.toFloat(), paint)

            val fileName = "filtered_${System.currentTimeMillis()}.jpg"
            val outFile = File(applicationContext.filesDir, fileName)
            FileOutputStream(outFile).use { fos ->
                filtered.compress(Bitmap.CompressFormat.JPEG, 95, fos)
            }


            Result.success(workDataOf(
                Keys.KEY_FILTERED_PATH to outFile.absolutePath,
                Keys.KEY_DOWNLOADED_PATH to path
            ))
        } catch (e: Exception) {
            Result.retry()
        }
    }
}