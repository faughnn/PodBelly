package com.podbelly.core.common.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Writes [image] to a cache file and fires the system share sheet with the PNG
 * attached and [text] as the accompanying message (the episode / series links).
 */
suspend fun shareCardImage(context: Context, image: ImageBitmap, text: String, chooserTitle: String) {
    val uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "shares").apply { mkdirs() }
        val file = File(dir, "podbelly-share.png")
        file.outputStream().use { out ->
            image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, chooserTitle))
}
