package com.qabas.app

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

data class ImageAdjustmentParams(
    val brightness: Float = 0f, // -100 to 100
    val contrast: Float = 1f,    // 0.5 to 1.5
    val saturation: Float = 1f,  // 0 to 2
    val cropRatio: String = "9:16", // "Original", "9:16", "1:1", "16:9", "4:5"
    val textOverlay: String = "",
    val textStyleTheme: String = "GOLD_DEEP", // "GOLD_DEEP", "ROYAL_WHITE", "NEON_EMERALD"
    val showBrandWatermark: Boolean = true
)

object ImageEditorEngine {
    private const val TAG = "ImageEditorEngine"

    suspend fun processAndExportImage(
        context: Context,
        inputUri: Uri,
        params: ImageAdjustmentParams,
        targetWidth: Int = 1080,
        targetHeight: Int = 1920
    ): File? = withContext(Dispatchers.IO) {
        try {
            // 1. Decode original Bitmap
            val inputStream = context.contentResolver.openInputStream(inputUri) ?: return@withContext null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) return@withContext null

            // 2. Crop according to aspect ratio
            val croppedBitmap = applyCrop(originalBitmap, params.cropRatio)

            // 3. Scale to target output size
            val (finalW, finalH) = calculateTargetDimensions(params.cropRatio, targetWidth, targetHeight)
            val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, finalW, finalH, true)

            // 4. Create mutable canvas
            val outputBitmap = Bitmap.createBitmap(finalW, finalH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(outputBitmap)

            // 5. Apply Color Matrix (Brightness, Contrast, Saturation)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                isFilterBitmap = true
                colorFilter = createColorFilter(params.brightness, params.contrast, params.saturation)
            }
            canvas.drawBitmap(scaledBitmap, 0f, 0f, paint)

            // 6. Draw subtle Islamic / Qabas gradient vignette for readability
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, finalH * 0.45f, 0f, finalH.toFloat(),
                    intArrayOf(android.graphics.Color.TRANSPARENT, android.graphics.Color.argb(210, 11, 15, 25)),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, finalW.toFloat(), finalH.toFloat(), vignettePaint)

            // 7. Draw Decorative Frame if 9:16 or 1:1
            if (params.cropRatio == "9:16" || params.cropRatio == "1:1") {
                val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.argb(70, 232, 197, 71) // Gold #E8C547 with alpha
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                }
                val inset = 24f
                canvas.drawRoundRect(RectF(inset, inset, finalW - inset, finalH - inset), 20f, 20f, framePaint)
            }

            // 8. Render Arabic Text with auto-wrapping & proper typography
            if (params.textOverlay.isNotBlank()) {
                drawArabicTextOverlay(canvas, params.textOverlay, finalW, finalH, params.textStyleTheme)
            }

            // 9. Draw Qabas Official Watermark
            if (params.showBrandWatermark) {
                drawBrandWatermark(canvas, finalW, finalH)
            }

            // 10. Save to Cache and/or MediaStore
            val outFile = File(context.cacheDir, "qabas_edited_${System.currentTimeMillis()}.jpg")
            val fos = FileOutputStream(outFile)
            outputBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
            fos.flush()
            fos.close()

            // Save copy to MediaStore Gallery as well
            saveImageToGallery(context, outputBitmap, "Qabas_Studio_${System.currentTimeMillis()}")

            return@withContext outFile
        } catch (e: Exception) {
            Log.e(TAG, "Error editing image: ${e.message}", e)
            return@withContext null
        }
    }

    private fun applyCrop(src: Bitmap, cropRatio: String): Bitmap {
        val srcW = src.width
        val srcH = src.height

        val targetRatio = when (cropRatio) {
            "9:16" -> 9f / 16f
            "1:1" -> 1f
            "16:9" -> 16f / 9f
            "4:5" -> 4f / 5f
            else -> return src // Original
        }

        val srcRatio = srcW.toFloat() / srcH.toFloat()
        var cropW = srcW
        var cropH = srcH
        var cropX = 0
        var cropY = 0

        if (srcRatio > targetRatio) {
            // Source is wider than target ratio
            cropW = (srcH * targetRatio).toInt()
            cropX = (srcW - cropW) / 2
        } else {
            // Source is taller than target ratio
            cropH = (srcW / targetRatio).toInt()
            cropY = (srcH - cropH) / 2
        }

        cropW = cropW.coerceAtLeast(1).coerceAtMost(srcW)
        cropH = cropH.coerceAtLeast(1).coerceAtMost(srcH)

        return Bitmap.createBitmap(src, cropX, cropY, cropW, cropH)
    }

    private fun calculateTargetDimensions(cropRatio: String, defaultW: Int, defaultH: Int): Pair<Int, Int> {
        return when (cropRatio) {
            "9:16" -> Pair(1080, 1920)
            "1:1" -> Pair(1080, 1080)
            "16:9" -> Pair(1920, 1080)
            "4:5" -> Pair(1080, 1350)
            else -> Pair(defaultW, defaultH)
        }
    }

    private fun createColorFilter(brightness: Float, contrast: Float, saturation: Float): ColorFilter {
        // Brightness matrix (-100 to 100)
        val cmBrightness = ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, brightness,
            0f, 1f, 0f, 0f, brightness,
            0f, 0f, 1f, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))

        // Contrast matrix (0.5 to 1.5)
        val scale = contrast
        val translate = (-0.5f * scale + 0.5f) * 255f
        val cmContrast = ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))

        // Saturation matrix (0 to 2)
        val cmSat = ColorMatrix().apply { setSaturation(saturation) }

        val finalMatrix = ColorMatrix()
        finalMatrix.postConcat(cmBrightness)
        finalMatrix.postConcat(cmContrast)
        finalMatrix.postConcat(cmSat)

        return ColorMatrixColorFilter(finalMatrix)
    }

    private fun drawArabicTextOverlay(
        canvas: Canvas,
        text: String,
        width: Int,
        height: Int,
        theme: String
    ) {
        val textColor = when (theme) {
            "GOLD_DEEP" -> android.graphics.Color.parseColor("#E8C547")
            "ROYAL_WHITE" -> android.graphics.Color.parseColor("#F8FAFC")
            "NEON_EMERALD" -> android.graphics.Color.parseColor("#10B981")
            else -> android.graphics.Color.WHITE
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = (width * 0.052f).coerceIn(32f, 64f)
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            setShadowLayer(8f, 2f, 2f, android.graphics.Color.argb(180, 0, 0, 0))
        }

        val maxLineWidth = width * 0.85f
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (w in words) {
            val testLine = if (currentLine.isEmpty()) w else "$currentLine $w"
            if (textPaint.measureText(testLine) < maxLineWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                currentLine = w
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)

        val lineHeight = textPaint.textSize * 1.4f
        val totalTextHeight = lines.size * lineHeight
        val startY = (height / 2f) - (totalTextHeight / 2f) + (textPaint.textSize * 0.8f)

        for ((index, line) in lines.withIndex()) {
            canvas.drawText(line, width / 2f, startY + (index * lineHeight), textPaint)
        }
    }

    private fun drawBrandWatermark(canvas: Canvas, width: Int, height: Int) {
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(200, 232, 197, 71) // Gold
            textSize = (width * 0.028f).coerceIn(18f, 32f)
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            setShadowLayer(4f, 1f, 1f, android.graphics.Color.BLACK)
        }
        canvas.drawText("✦ قَبَس | QABAS STUDIO ✦", width / 2f, height - 40f, brandPaint)
    }

    private fun saveImageToGallery(context: Context, bitmap: Bitmap, title: String) {
        val filename = "$title.jpg"
        var fos: OutputStream? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Qabas")
            }
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.let { uri ->
                fos = context.contentResolver.openOutputStream(uri)
            }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/Qabas"
            val file = File(imagesDir)
            if (!file.exists()) file.mkdir()
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
        }
        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
        }
    }
}
