package com.agrogem.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Downscales an image down to `maxDim` on its longest side and re-encodes as JPEG.
 *
 * Why this exists: phone photos are 4-12MB at 12MP+. Handing those raw to LiteRT-LM
 * blows the input token count toward our `maxNumTokens` cap and pushes vision-prefill
 * memory into LMK territory. 768px JPEG q85 keeps Gemma's diagnosis quality intact
 * while shrinking the file ~30-50× and the input-token footprint roughly in half.
 *
 * EXIF rotation matters: phones store portrait shots as landscape pixels + a rotation
 * tag. A naive decode-then-scale loses the rotation; the model then sees a side-lying
 * plant. We read the EXIF orientation and apply it via Matrix before compressing.
 */
internal object ImageDownscaler {
    private const val TAG = "ImageDownscaler"
    private const val MAX_DIM_PX = 768
    private const val JPEG_QUALITY = 85
    private const val SKIP_FAST_PATH_BYTES = 200 * 1024L

    /**
     * Reads from [sourceUri] (content:// or file://) and writes a downscaled JPEG
     * into [cacheDir]. Returns the resulting File, or falls back to a raw byte copy
     * if anything goes wrong — the LiteRT-LM input must always be a valid file.
     */
    fun downscaleToCache(context: Context, sourceUri: Uri, cacheDir: File): File {
        val outFile = File(cacheDir, "gemma_input_${System.currentTimeMillis()}.jpg")
        return runCatching { downscale(context, sourceUri, outFile) }
            .getOrElse { error ->
                Log.w(TAG, "Downscale failed (${error.message}); falling back to raw copy")
                runCatching { copyRaw(context, sourceUri, outFile) }
                    .getOrElse { rawError ->
                        throw java.io.IOException(
                            "No se pudo procesar la imagen: ${rawError.message}",
                            rawError,
                        )
                    }
            }
    }

    private fun downscale(context: Context, sourceUri: Uri, outFile: File): File {
        // Buffer once to a ByteArray — content:// streams are single-shot, and we need
        // to read it three times (bounds, EXIF, decode). Memory cost is the source size,
        // which is bounded by what the user picked.
        val bytes = readAllBytes(context, sourceUri)

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(ByteArrayInputStream(bytes), null, bounds)
        val srcWidth = bounds.outWidth
        val srcHeight = bounds.outHeight
        if (srcWidth <= 0 || srcHeight <= 0) {
            throw java.io.IOException("Bounds decode failed (w=$srcWidth h=$srcHeight)")
        }

        val rotationDegrees = readExifRotation(bytes)

        // Skip-fast path: image already small enough. Just copy bytes verbatim
        // (preserving original quality) but rotate if EXIF says so.
        if (
            srcWidth <= MAX_DIM_PX &&
            srcHeight <= MAX_DIM_PX &&
            bytes.size.toLong() <= SKIP_FAST_PATH_BYTES &&
            rotationDegrees == 0
        ) {
            outFile.writeBytes(bytes)
            Log.d(TAG, "Skip-fast: ${srcWidth}x${srcHeight} ${bytes.size / 1024} KB, no rotation")
            return outFile
        }

        val sampleSize = computeInSampleSize(srcWidth, srcHeight, MAX_DIM_PX)
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val sampledBitmap = BitmapFactory.decodeStream(
            ByteArrayInputStream(bytes), null, decodeOpts,
        ) ?: throw java.io.IOException("Decode returned null")

        val scaledBitmap = scaleToMaxDim(sampledBitmap, MAX_DIM_PX)
        val rotatedBitmap = if (rotationDegrees != 0) rotate(scaledBitmap, rotationDegrees) else scaledBitmap

        try {
            FileOutputStream(outFile).use { out ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                out.fd.sync()
            }
        } finally {
            // Recycle every distinct Bitmap we created. createScaledBitmap and Matrix
            // rotate may return either a new bitmap or the same one we passed in.
            if (rotatedBitmap !== scaledBitmap) rotatedBitmap.recycle()
            if (scaledBitmap !== sampledBitmap) scaledBitmap.recycle()
            sampledBitmap.recycle()
        }

        Log.d(
            TAG,
            "Downscaled ${srcWidth}x${srcHeight} (${bytes.size / 1024} KB) -> " +
                "${rotatedBitmap.width}x${rotatedBitmap.height} (${outFile.length() / 1024} KB) " +
                "rot=${rotationDegrees}deg sample=$sampleSize",
        )
        return outFile
    }

    private fun copyRaw(context: Context, sourceUri: Uri, outFile: File): File {
        val input = context.contentResolver.openInputStream(sourceUri)
            ?: throw java.io.IOException("Cannot open $sourceUri")
        input.use { stream ->
            outFile.outputStream().use { out -> stream.copyTo(out) }
        }
        if (outFile.length() == 0L) throw java.io.IOException("Raw copy produced empty file")
        return outFile
    }

    private fun readAllBytes(context: Context, sourceUri: Uri): ByteArray {
        val input = context.contentResolver.openInputStream(sourceUri)
            ?: throw java.io.IOException("Cannot open $sourceUri")
        return input.use { it.readBytes() }
    }

    private fun readExifRotation(bytes: ByteArray): Int {
        return runCatching {
            val exif = ExifInterface(ByteArrayInputStream(bytes))
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        }.getOrDefault(0)
    }

    private fun computeInSampleSize(srcW: Int, srcH: Int, target: Int): Int {
        var sampleSize = 1
        var w = srcW
        var h = srcH
        while (w / 2 >= target && h / 2 >= target) {
            sampleSize *= 2
            w /= 2
            h /= 2
        }
        return sampleSize
    }

    private fun scaleToMaxDim(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val longSide = maxOf(w, h)
        if (longSide <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / longSide
        val newW = (w * ratio).toInt().coerceAtLeast(1)
        val newH = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, /* filter = */ true)
    }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
