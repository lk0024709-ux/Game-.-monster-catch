package com.example.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.InputStream

/**
 * Utility to safely decode Bitmaps from Assets or general InputStreams,
 * preventing OutOfMemoryError (OOM) by calculating conservative sample sizes.
 */
object SafeBitmapLoader {
    private const val TAG = "SafeBitmapLoader"

    /**
     * Calculates the sample size configuration to downscale high resolution assets
     * dynamically matching target boundary constraints.
     */
    fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Safely decodes a bitmap from the app's assets folder with dynamic down-sampling.
     */
    fun decodeSampledBitmapFromAsset(
        context: Context,
        assetName: String,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        return try {
            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            
            context.assets.open(assetName).use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false
            
            context.assets.open(assetName).use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to safely decode asset: $assetName", e)
            null
        }
    }

    /**
     * Safely decodes a bitmap from an InputStream supplier function.
     */
    fun decodeSampledBitmapFromStream(
        reqWidth: Int,
        reqHeight: Int,
        streamSupplier: () -> InputStream?
    ): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            
            streamSupplier()?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return null

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false

            streamSupplier()?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to safely decode from custom stream", e)
            null
        }
    }
}
