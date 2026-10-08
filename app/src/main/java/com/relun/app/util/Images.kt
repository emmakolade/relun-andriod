package com.relun.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

private const val MAX_EDGE = 1440
private const val JPEG_QUALITY = 85

/**
 * Reads a picked photo and returns a JPEG no larger than 1440px on its long
 * edge. Keeps uploads small on mobile data; the server stores 1080px anyway.
 */
suspend fun compressForUpload(context: Context, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
    val bitmap = if (Build.VERSION.SDK_INT >= 28) {
        // ImageDecoder applies EXIF rotation, so portrait photos stay upright.
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            val longEdge = max(info.size.width, info.size.height)
            if (longEdge > MAX_EDGE) {
                val scale = MAX_EDGE.toFloat() / longEdge
                decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Couldn’t read that photo")
    }

    ByteArrayOutputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        bitmap.recycle()
        out.toByteArray()
    }
}
