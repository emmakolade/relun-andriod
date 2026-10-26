package com.relun.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

private const val MAX_EDGE = 1440
private const val JPEG_QUALITY = 85

/** Matches the server's per-file limit. */
private const val MAX_RAW_BYTES = 20 * 1024 * 1024

/** Bytes ready to upload, with their real type. */
class UploadImage(val bytes: ByteArray, val mimeType: String, val fileName: String)

/** A picked photo that can't be uploaded; the message is shown to the user as-is. */
class PhotoReadException(message: String) : Exception(message)

/**
 * Reads a picked photo and returns a JPEG no larger than 1440px on its long
 * edge. Keeps uploads small on mobile data; the server stores 1080px anyway.
 *
 * Tries ImageDecoder, then BitmapFactory. If this device can decode neither
 * (some HEIC, AVIF or RAW files), the original file is sent as-is and the
 * server converts it to JPEG.
 */
suspend fun compressForUpload(context: Context, uri: Uri): UploadImage = withContext(Dispatchers.IO) {
    val bitmap = decodeWithImageDecoder(context, uri) ?: decodeWithBitmapFactory(context, uri)
    if (bitmap != null) {
        return@withContext ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            bitmap.recycle()
            UploadImage(out.toByteArray(), "image/jpeg", "photo.jpg")
        }
    }

    val bytes = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        ?: throw PhotoReadException("We couldn’t read that photo. Try another one.")
    if (bytes.size > MAX_RAW_BYTES) throw PhotoReadException("That photo is too large. Try one under 20 MB.")
    val mime = context.contentResolver.getType(uri)?.takeIf { it.startsWith("image/") } ?: "application/octet-stream"
    val extension = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
    UploadImage(bytes, mime, "photo.$extension")
}

private fun decodeWithImageDecoder(context: Context, uri: Uri): Bitmap? {
    if (Build.VERSION.SDK_INT < 28) return null
    return try {
        // ImageDecoder applies EXIF rotation, so portrait photos stay upright.
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            val longEdge = max(info.size.width, info.size.height)
            if (longEdge > MAX_EDGE) {
                val scale = MAX_EDGE.toFloat() / longEdge
                decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            // Partial or slightly corrupt files still give a usable picture.
            decoder.setOnPartialImageListener { true }
        }.let { bitmap ->
            // JPEG can't hold wide-gamut or 16-bit pixels; normalise before compressing.
            if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap
            else bitmap.copy(Bitmap.Config.ARGB_8888, false).also { bitmap.recycle() }
        }
    } catch (e: Exception) {
        null
    } catch (e: OutOfMemoryError) {
        null
    }
}

private fun decodeWithBitmapFactory(context: Context, uri: Uri): Bitmap? = try {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    decoded?.let { bitmap ->
        // BitmapFactory ignores EXIF, so rotate portrait photos ourselves.
        val degrees = resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
        if (degrees == 0) bitmap
        else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees.toFloat()) }, true)
            .also { if (it !== bitmap) bitmap.recycle() }
    }
} catch (e: Exception) {
    null
} catch (e: OutOfMemoryError) {
    null
}
