package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.InputStream

data class AttachmentResult(
    val uri: Uri?,
    val fileName: String,
    val isPdf: Boolean,
    val pageCount: Int = 1,
    val currentPage: Int = 0,
    val bitmap: Bitmap,
    val fileSizeFormatted: String = ""
)

object DocumentAttachmentHelper {

    fun isPdf(context: Context, uri: Uri): Boolean {
        val type = context.contentResolver.getType(uri)?.lowercase() ?: ""
        if (type.contains("pdf")) return true
        val name = getDisplayName(context, uri).lowercase()
        return name.endsWith(".pdf")
    }

    fun getDisplayName(context: Context, uri: Uri): String {
        var name = "drawing_attachment"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    val str = cursor.getString(nameIndex)
                    if (!str.isNullOrBlank()) name = str
                }
            }
        } catch (e: Exception) {
            val lastSegment = uri.lastPathSegment
            if (!lastSegment.isNullOrBlank()) name = lastSegment
        }
        return name
    }

    fun getFileSizeFormatted(context: Context, uri: Uri): String {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst() && sizeIndex >= 0) {
                    val bytes = cursor.getLong(sizeIndex)
                    return when {
                        bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
                        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
                        bytes > 0 -> "$bytes B"
                        else -> ""
                    }
                }
            }
        } catch (e: Exception) { }
        return ""
    }

    fun loadAttachment(context: Context, uri: Uri, pageIndex: Int = 0): Result<AttachmentResult> {
        val fileName = getDisplayName(context, uri)
        val fileSize = getFileSizeFormatted(context, uri)
        val isPdfFile = isPdf(context, uri)

        return if (isPdfFile) {
            loadPdfAttachment(context, uri, fileName, fileSize, pageIndex)
        } else {
            loadImageAttachment(context, uri, fileName, fileSize)
        }
    }

    private fun loadPdfAttachment(
        context: Context,
        uri: Uri,
        fileName: String,
        fileSize: String,
        pageIndex: Int
    ): Result<AttachmentResult> {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r")
                ?: return Result.failure(IllegalStateException("Unable to open PDF descriptor for $fileName"))

            renderer = PdfRenderer(pfd)
            val totalPages = renderer.pageCount
            if (totalPages <= 0) {
                return Result.failure(IllegalStateException("PDF contains no readable pages."))
            }

            val targetPage = pageIndex.coerceIn(0, totalPages - 1)
            page = renderer.openPage(targetPage)

            // Calculate high-resolution scale for blueprint legibility (minimum 1400px width/height)
            val baseW = page.width
            val baseH = page.height
            val scale = (1600f / maxOf(baseW, baseH)).coerceIn(1.5f, 3.0f)
            val targetW = (baseW * scale).toInt().coerceAtLeast(1)
            val targetH = (baseH * scale).toInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE) // Ensure white background for blueprints

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            return Result.success(
                AttachmentResult(
                    uri = uri,
                    fileName = fileName,
                    isPdf = true,
                    pageCount = totalPages,
                    currentPage = targetPage,
                    bitmap = bitmap,
                    fileSizeFormatted = fileSize
                )
            )
        } catch (e: Exception) {
            return Result.failure(e)
        } finally {
            try { page?.close() } catch (ignored: Exception) {}
            try { renderer?.close() } catch (ignored: Exception) {}
            try { pfd?.close() } catch (ignored: Exception) {}
        }
    }

    private fun loadImageAttachment(
        context: Context,
        uri: Uri,
        fileName: String,
        fileSize: String
    ): Result<AttachmentResult> {
        return try {
            // Check dimensions first to avoid OutOfMemoryError on large photos
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            var stream: InputStream? = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(stream, null, options)
            stream?.close()

            var inSampleSize = 1
            val maxDimension = 1800
            while (options.outWidth / inSampleSize > maxDimension || options.outHeight / inSampleSize > maxDimension) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            stream = context.contentResolver.openInputStream(uri)
            val decodedBitmap = BitmapFactory.decodeStream(stream, null, decodeOptions)
            stream?.close()

            if (decodedBitmap == null) {
                return Result.failure(IllegalStateException("Could not decode image from $fileName"))
            }

            Result.success(
                AttachmentResult(
                    uri = uri,
                    fileName = fileName,
                    isPdf = false,
                    pageCount = 1,
                    currentPage = 0,
                    bitmap = decodedBitmap,
                    fileSizeFormatted = fileSize
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates a high-resolution, photorealistic engineering blueprint bitmap
     * with crisp CAD callouts, title block, and dimension lines.
     * Allows 1-tap testing of the AI Scanner with real blueprints.
     */
    fun createSampleBlueprintBitmap(sampleType: SampleBlueprintType): AttachmentResult {
        val width = 1400
        val height = 900
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Classic blueprint dark blue / industrial cad dark background
        canvas.drawColor(Color.rgb(15, 23, 42)) // Slate 900

        val gridPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        // Draw engineering grid
        val gridSize = 40f
        var x = 0f
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            x += gridSize
        }
        var y = 0f
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += gridSize
        }

        // Border
        val borderPaint = Paint().apply {
            color = Color.rgb(56, 189, 248) // Sky blue
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }
        canvas.drawRect(20f, 20f, width - 20f, height - 20f, borderPaint)
        canvas.drawRect(24f, 24f, width - 24f, height - 24f, borderPaint.apply { strokeWidth = 1f })

        // Title Block in bottom right corner
        val tbPaint = Paint().apply {
            color = Color.rgb(30, 58, 138)
            style = Paint.Style.FILL
        }
        val tbX = (width - 480).toFloat()
        val tbY = (height - 180).toFloat()
        canvas.drawRect(tbX, tbY, width - 24f, height - 24f, tbPaint)
        canvas.drawRect(tbX, tbY, width - 24f, height - 24f, borderPaint)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            isAntiAlias = true
            isFakeBoldText = true
        }
        val subPaint = Paint().apply {
            color = Color.rgb(186, 230, 253)
            textSize = 15f
            isAntiAlias = true
        }
        val notePaint = Paint().apply {
            color = Color.rgb(253, 224, 71) // Yellow callouts
            textSize = 24f
            isAntiAlias = true
            isFakeBoldText = true
        }

        canvas.drawText("AEROSPACE FASTENER SPECIFICATION", tbX + 16f, tbY + 32f, textPaint)
        canvas.drawText("PART: ${sampleType.title}", tbX + 16f, tbY + 62f, notePaint)
        canvas.drawText("STD: ${sampleType.standard} | ALLOY: ${sampleType.materialKey}", tbX + 16f, tbY + 92f, subPaint)
        canvas.drawText("CLASS: ${sampleType.propertyClass} | FINISH: ${sampleType.coating}", tbX + 16f, tbY + 120f, subPaint)
        canvas.drawText("DWG NO: AF-${sampleType.nominalDiaMm.toInt()}-${sampleType.materialKey} | SHT 1 OF 1", tbX + 16f, tbY + 146f, subPaint)

        // Draw Part Geometry
        val partPaint = Paint().apply {
            color = Color.rgb(224, 231, 255)
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val fillPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            style = Paint.Style.FILL
        }
        val dimPaint = Paint().apply {
            color = Color.rgb(56, 189, 248)
            strokeWidth = 2f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val cy = 400f
        val startX = 200f
        val headW = 140f
        val headH = sampleType.nominalDiaMm.toFloat() * 14f
        val shankL = sampleType.lengthMm.toFloat() * 12f
        val shankH = sampleType.nominalDiaMm.toFloat() * 8.5f

        // Head
        canvas.drawRect(startX, cy - headH / 2, startX + headW, cy + headH / 2, fillPaint)
        canvas.drawRect(startX, cy - headH / 2, startX + headW, cy + headH / 2, partPaint)

        // Shank
        canvas.drawRect(startX + headW, cy - shankH / 2, startX + headW + shankL, cy + shankH / 2, fillPaint)
        canvas.drawRect(startX + headW, cy - shankH / 2, startX + headW + shankL, cy + shankH / 2, partPaint)

        // Thread lines
        val threadL = shankL * 0.7f
        var tx = startX + headW + (shankL - threadL)
        val threadPaint = Paint().apply {
            color = Color.rgb(148, 163, 184)
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
        }
        while (tx < startX + headW + shankL) {
            canvas.drawLine(tx, cy - shankH / 2 + 4f, tx + 6f, cy + shankH / 2 - 4f, threadPaint)
            tx += 14f
        }

        // Leader lines & Dimensions
        // 1. Nominal Dia callout
        canvas.drawLine(startX + headW + 50f, cy + shankH / 2 + 10f, startX + headW + 50f, cy + shankH / 2 + 70f, dimPaint)
        canvas.drawLine(startX + headW + 150f, cy + shankH / 2 + 10f, startX + headW + 150f, cy + shankH / 2 + 70f, dimPaint)
        canvas.drawText("Ø${sampleType.nominalDiaMm} ${sampleType.threadClass} (Pitch: ${sampleType.pitchMm}mm)", startX + headW + 30f, cy + shankH / 2 + 100f, notePaint)

        // 2. Length callout
        canvas.drawLine(startX + headW, cy - headH / 2 - 30f, startX + headW, cy - headH / 2 - 70f, dimPaint)
        canvas.drawLine(startX + headW + shankL, cy - headH / 2 - 30f, startX + headW + shankL, cy - headH / 2 - 70f, dimPaint)
        canvas.drawLine(startX + headW, cy - headH / 2 - 50f, startX + headW + shankL, cy - headH / 2 - 50f, dimPaint)
        canvas.drawText("LENGTH L = ${sampleType.lengthMm} mm ±0.3", startX + headW + shankL / 2 - 120f, cy - headH / 2 - 60f, notePaint)

        // Notes box in top left
        canvas.drawText("ENGINEERING DRAWING CALLOUTS:", 50f, 60f, textPaint)
        canvas.drawText("1. THREAD: M${sampleType.nominalDiaMm} x ${sampleType.pitchMm} - ${sampleType.threadClass} per ISO 965 / ASME B1.13M", 50f, 90f, subPaint)
        canvas.drawText("2. MATERIAL: ${sampleType.materialKey} (AMS / ISO Grade, Min UTS: ${sampleType.utsMpa} MPa)", 50f, 116f, subPaint)
        canvas.drawText("3. HEAT TREATMENT: Austenitize/Solution per AMS 2759 | Target Hardness: ${sampleType.targetHardness}", 50f, 142f, subPaint)
        canvas.drawText("4. FINISH: ${sampleType.coating} Electrodeposit per AMS 2417 (5-8 µm)", 50f, 168f, subPaint)
        canvas.drawText("5. DE-EMBRITTLEMENT BAKING: 190°C–200°C for 8–24 Hours post-plating mandatory", 50f, 194f, subPaint)

        return AttachmentResult(
            uri = null,
            fileName = "CAD_${sampleType.name}.pdf",
            isPdf = true,
            pageCount = 1,
            currentPage = 0,
            bitmap = bitmap,
            fileSizeFormatted = "184 KB"
        )
    }
}

enum class SampleBlueprintType(
    val title: String,
    val standard: String,
    val nominalDiaMm: Double,
    val pitchMm: Double,
    val lengthMm: Double,
    val materialKey: String,
    val propertyClass: String,
    val threadClass: String,
    val coating: String,
    val utsMpa: Int,
    val targetHardness: String
) {
    M8_SHCS(
        title = "M8x1.25 Aerospace SHCS",
        standard = "DIN 912 / ISO 4762",
        nominalDiaMm = 8.0,
        pitchMm = 1.25,
        lengthMm = 45.0,
        materialKey = "Ti-6Al-4V",
        propertyClass = "AERO_1100",
        threadClass = "4h6h",
        coating = "PASSIVATION",
        utsMpa = 1100,
        targetHardness = "36 HRC"
    ),
    M12_FLANGE_BOLT(
        title = "M12x1.75 Flange Bolt",
        standard = "ISO 4162 / EN 1665",
        nominalDiaMm = 12.0,
        pitchMm = 1.75,
        lengthMm = 65.0,
        materialKey = "4140",
        propertyClass = "10.9",
        threadClass = "6g",
        coating = "ZN_NI",
        utsMpa = 1040,
        targetHardness = "32-39 HRC"
    ),
    HALF_INCH_INCONEL_STUD(
        title = "1/2-20 Aircraft Stud",
        standard = "MS21250 / AS8879",
        nominalDiaMm = 12.7,
        pitchMm = 1.27, // 20 TPI ~ 1.27mm
        lengthMm = 75.0,
        materialKey = "INCONEL718",
        propertyClass = "AERO_1380",
        threadClass = "3A",
        coating = "CADMIUM",
        utsMpa = 1380,
        targetHardness = "44 HRC"
    )
}
