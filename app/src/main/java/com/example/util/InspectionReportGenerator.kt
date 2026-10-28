package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.FastenerCalculationResult
import com.example.data.PokaYokeEngine
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InspectionReportGenerator {

    fun generateInspectionReportText(result: FastenerCalculationResult): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val timestamp = dateFormat.format(Date())

        val sb = StringBuilder()
        sb.append("========================================================\n")
        sb.append("         AEROFAST PRO - INSPECTION TRAVELER & MTC       \n")
        sb.append("       MATERIAL TEST CERTIFICATE & QUALITY RECORD       \n")
        sb.append("========================================================\n")
        sb.append(" Generated Date: $timestamp\n")
        sb.append(" Standard Compliance: ISO 898-1 / AS9100D / AMS Spec\n")
        sb.append("--------------------------------------------------------\n\n")

        sb.append("1. PART & SPECIFICATION IDENTIFICATION\n")
        sb.append("--------------------------------------------------------\n")
        sb.append(" Fastener Type    : ${result.fastenerType.label}\n")
        sb.append(" Product Standard : ${result.standard.code} (${result.standard.label})\n")
        sb.append(" Thread Designation: ${result.threadLabel}\n")
        sb.append(" Nominal Diameter : ${result.nominalDiaMm} mm\n")
        sb.append(" Pitch            : ${result.pitchMm} mm\n")
        sb.append(" Shank Length     : ${result.lengthMm} mm (Thread Length: ${result.threadLengthMm} mm)\n")
        sb.append(" Tolerance Class  : ${result.threadClass}\n")
        sb.append(" Batch Quantity   : ${result.batchQty} pcs\n\n")

        sb.append("2. MATERIAL & METALLURGY REQUIREMENTS (attach mill MTC)\n")
        sb.append("--------------------------------------------------------\n")
        sb.append(" Material Grade   : ${result.material.name}\n")
        sb.append(" Raw Wire Sourcing: ${result.rawMaterialSourcingUts}\n")
        sb.append(" Target UTS       : ${result.postForgingTargetUtsMpa} MPa\n")
        sb.append(" Min Yield Strength: ${result.yieldStrengthMpa} MPa\n")
        sb.append(" Material Density : ${result.material.density} g/cm³\n")
        sb.append(" Surface Coating  : ${result.coating.label} (${result.coatingSpecText})\n\n")

        sb.append("3. MECHANICAL LOAD & TORQUE CAPACITIES\n")
        sb.append("--------------------------------------------------------\n")
        sb.append(" Tensile Stress Area (As): ${result.stressAreaAsMm2} mm²\n")
        sb.append(" Tensile Breaking Load  : ${result.tensileBreakingLoadKn} kN\n")
        sb.append(" Proof Load Capacity    : ${result.proofLoadKn} kN\n")
        sb.append(" Recommended Clamp Load : ${(result.proofLoadKn * 0.75).round(2)} kN\n")
        sb.append(" Tightening Torque      : ${((result.proofLoadKn * 0.75 * result.nominalDiaMm * 0.2)).round(1)} N·m\n\n")

        sb.append("4. HEAT TREATMENT & POKA-YOKE QUALITY GATES\n")
        sb.append("--------------------------------------------------------\n")
        sb.append(" Austenitizing Temp : ${result.heatTreatmentSpec.austenitizingOrSolutionTempC}\n")
        sb.append(" Soaking Time     : ${result.heatTreatmentSpec.soakingTimeMinutes}\n")
        sb.append(" Quench Medium    : ${result.heatTreatmentSpec.quenchMedium}\n")
        sb.append(" Tempering / Aging: ${result.heatTreatmentSpec.temperingOrAgingTempC} for ${result.heatTreatmentSpec.temperingOrAgingTimeHours}\n")
        sb.append(" Target Hardness  : ${result.heatTreatmentSpec.targetHardnessHrc}\n")
        sb.append(" Hydrogen Baking  : ${if (result.hasHydrogenEmbrittlementRisk) "MANDATORY -> " + result.heatTreatmentSpec.hydrogenBakingCycle else "EXEMPT / NOT REQUIRED"}\n\n")

        sb.append("4B. ENGINEERING POKA-YOKE GATE\n")
        sb.append("--------------------------------------------------------\n")
        sb.append(PokaYokeEngine.reportSection(result.pokaYokeIssues)).append('\n')

        sb.append("5. SHOP-FLOOR TOOLING & CNC G-CODE SUMMARY\n")
        sb.append("--------------------------------------------------------\n")
        sb.append(" Pre-Roll Blank Dia: ${result.blankTargetDia} mm (Tol: ±${result.blankTolHalf})\n")
        sb.append(" Recommended Mach : ${result.recommendedMachine}\n")
        sb.append(" Die/Punch Steels : ${result.material.dieTooling}\n")
        sb.append(" Lubrication      : ${result.material.lubricationCoating}\n\n")

        sb.append("6. COMMERCIAL QUOTATION SUMMARY\n")
        sb.append("--------------------------------------------------------\n")
        sb.append(" Material Cost    : ${result.currencySymbol}${result.costRawMaterial}\n")
        sb.append(" Forging/Heading  : ${result.currencySymbol}${result.costForgingHeading}\n")
        sb.append(" Heat Treatment   : ${result.currencySymbol}${result.costHeatTreatment}\n")
        sb.append(" Inspection / NDT : ${result.currencySymbol}${result.costInspectionNdt}\n")
        sb.append(" Final Unit Price : ${result.currencySymbol}${result.finalUnitPrice}\n")
        sb.append(" Total Batch Price: ${result.currencySymbol}${result.batchTotalPrice}\n\n")

        sb.append("========================================================\n")
        // The app calculates specifications; it never certifies parts it has not measured
        sb.append(
            if (result.isReleaseBlocked) " STATUS: RELEASE BLOCKED BY POKA-YOKE (see section 4B)\n"
            else " STATUS: ENGINEERING SPECIFICATION - attach measured results before release\n"
        )
        sb.append(" INSPECTED BY: ______________________  Date: __________\n")
        sb.append("========================================================\n")

        return sb.toString()
    }

    private fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10.0 }
        return kotlin.math.round(this * multiplier) / multiplier
    }

    fun exportAndShareReport(context: Context, result: FastenerCalculationResult) {
        try {
            val reportContent = generateInspectionReportText(result)
            val fileName = "AeroFast_Inspection_Traveler_${result.threadLabel.replace(Regex("[^a-zA-Z0-9]"), "_")}.txt"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { fos ->
                fos.write(reportContent.toByteArray())
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "AeroFast Pro Inspection Traveler & MTC - ${result.threadLabel}")
                putExtra(Intent.EXTRA_TEXT, "Attached is the official Material Test Certificate and Process Traveler report generated by AeroFast Pro.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Export / Share Inspection Report")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
