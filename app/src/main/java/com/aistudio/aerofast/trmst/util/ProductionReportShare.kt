package com.aistudio.aerofast.trmst.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.aistudio.aerofast.trmst.data.ExtractedFastenerSpec
import com.aistudio.aerofast.trmst.data.FastenerCalculationResult
import com.aistudio.aerofast.trmst.data.ProductionReport
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ProductionReportShare {
    fun share(context: Context, result: FastenerCalculationResult, spec: ExtractedFastenerSpec?) {
        try {
            val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
            val text = ProductionReport.build(result, spec, stamp)
            val name = "AFP_Production_${result.threadLabel}x${result.lengthMm.toInt()}".replace(Regex("[^A-Za-z0-9_]"), "_") + ".txt"
            val file = File(context.cacheDir, name)
            file.writeText(text)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Production report - ${result.threadLabel} x ${result.lengthMm} mm")
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share production report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share the report: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
