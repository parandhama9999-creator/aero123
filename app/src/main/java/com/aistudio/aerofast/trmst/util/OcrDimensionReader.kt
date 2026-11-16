package com.aistudio.aerofast.trmst.util

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** A number read on the phone from a drawing; the user decides what it measures. */
data class OcrValue(val value: Double, val pitch: Double?, val source: String, val isDiameter: Boolean)

/** On-device OCR (ML Kit, offline): reads numbers and thread callouts, but does not guess which feature they belong to. */
object OcrDimensionReader {
    private val THREAD = Regex("""M\s?(\d{1,2}(?:[.,]\d+)?)\s*[xX×]\s*(\d(?:[.,]\d+)?)""")
    private val DIAMETER = Regex("""[Ø⌀Φφ]\s?(\d{1,3}(?:[.,]\d+)?)""")
    private val NUMBER = Regex("""(?<![\d.,])(\d{1,3}(?:[.,]\d{1,3})?)(?![\d])""")

    suspend fun read(bitmap: Bitmap): List<OcrValue> {
        val text = suspendCancellableCoroutine { cont ->
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
        val found = LinkedHashMap<String, OcrValue>()
        fun num(s: String) = s.replace(',', '.').toDoubleOrNull()
        for (block in text.textBlocks) for (line in block.lines) {
            val t = line.text
            THREAD.findAll(t).forEach { m ->
                val d = num(m.groupValues[1]); val p = num(m.groupValues[2])
                if (d != null && p != null) found.putIfAbsent("M$d×$p", OcrValue(d, p, t, isDiameter = true))
            }
            DIAMETER.findAll(t).forEach { m -> num(m.groupValues[1])?.let { found.putIfAbsent("Ø$it", OcrValue(it, null, t, isDiameter = true)) } }
            NUMBER.findAll(THREAD.replace(DIAMETER.replace(t, " "), " ")).forEach { m ->
                num(m.groupValues[1])?.takeIf { it in 0.3..500.0 }?.let { found.putIfAbsent("$it", OcrValue(it, null, t, isDiameter = false)) }
            }
        }
        return found.values.toList()
    }
}
