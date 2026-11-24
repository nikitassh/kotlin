package ru.mirea.soshnin.storage.gallery

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GalleryRepository(private val context: Context) {
    // приватная папка приложения на внешнем хранилище
    private val dir: File =
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!

    suspend fun scan(): List<File> = withContext(Dispatchers.IO) {
        dir.listFiles { f -> f.extension == "jpg" && f.length() > 0 }
            .orEmpty()
            .sortedByDescending { it.name }
    }

    // пустой файл для камеры: IMG_yyyyMMdd_HHmmss.jpg
    fun newPhotoFile(): File {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(dir, "IMG_$stamp.jpg")
    }

    fun uriFor(file: File): Uri = FileProvider.getUriForFile(
        context, context.packageName + ".fileprovider", file
    )

    // Задание 3: копия фото в общую галерею через MediaStore
    suspend fun exportToGallery(file: File): Boolean = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/StoragePractice")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            ) ?: return@withContext false
            resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } else {
            // Android 9 и ниже: нужен WRITE_EXTERNAL_STORAGE
            @Suppress("DEPRECATION")
            val pictures = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            )
            val target = File(pictures, "StoragePractice/" + file.name)
            target.parentFile?.mkdirs()
            file.copyTo(target, overwrite = true)
            MediaScannerConnection.scanFile(
                context, arrayOf(target.path), arrayOf("image/jpeg"), null
            )
            true
        }
    }
}
