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

object PpapReportGenerator {

    fun generatePpapPackageText(result: FastenerCalculationResult): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val timestamp = dateFormat.format(Date())

        val sb = StringBuilder()
        sb.append("================================================================================\n")
        sb.append("         AUTOMOTIVE PPAP LEVEL 3 DOCUMENTATION PACKAGE                          \n")
        sb.append("         PRODUCTION PART APPROVAL PROCESS (AIAG 4TH EDITION)                    \n")
        sb.append("================================================================================\n")
        sb.append(" Date of Submission : $timestamp\n")
        sb.append(" Customer           : Tier-1 Automotive / Aerospace OEM Assembly\n")
        sb.append(" Supplier           : AeroFast Pro Certified Fastener Plant\n")
        sb.append(" Part Name          : ${result.fastenerType.label}\n")
        sb.append(" Thread Specification: ${result.threadLabel} x ${result.lengthMm.toInt()}mm\n")
        sb.append(" Drawing Standard   : ${result.standard.code} (${result.standard.label})\n")
        sb.append(" Batch Lot Size     : ${result.batchQty} pcs\n")
        sb.append("================================================================================\n\n")

        // SECTION 1: PART SUBMISSION WARRANT (PSW)
        sb.append("1. PART SUBMISSION WARRANT (PSW)\n")
        sb.append("--------------------------------------------------------------------------------\n")
        sb.append(" Part Number / Rev  : AFP-${result.threadLabel}-REV-04\n")
        sb.append(" Engineering Change : EC-2025-0894 (Fillet Radius & Thread Roll Blank Optimization)\n")
        sb.append(" Material Spec      : ${result.material.name} (${result.material.rawMaterialSourcingUtsMpa} MPa)\n")
        sb.append(" Coating / Surface  : ${result.coating.label} (${result.coatingSpecText})\n")
        sb.append(" Weight per Piece   : ${result.netWeightGrams} grams\n")
        sb.append(" Reason for Sub.   : Initial Production Trial / Engineering Tooling Approval\n")
        sb.append(" Property Class     : ${result.propertyClass?.label ?: "Per material specification"}\n")
        sb.append(" Submission Level   : Level 3 (PSW + Dimensional + Material Test + Process Capability)\n")
        sb.append(" DECLARATION        : To be signed only after the measured results below are recorded and in tolerance.\n\n")

        // SECTION 1B: POKA-YOKE GATE (same rules as the app screen)
        sb.append("1B. ENGINEERING POKA-YOKE GATE\n")
        sb.append("--------------------------------------------------------------------------------\n")
        sb.append(PokaYokeEngine.reportSection(result.pokaYokeIssues)).append('\n')

        // SECTION 2: DIMENSIONAL RESULTS REPORT — limits are calculated, readings must be measured
        sb.append("2. DIMENSIONAL INSPECTION RESULTS REPORT (ISO 1502 / ${result.standard.code})\n")
        sb.append("--------------------------------------------------------------------------------\n")
        sb.append(" Feature            | Specification Limits         | Measured (n=5)   | Pass/Fail\n")
        sb.append("--------------------+------------------------------+------------------+----------\n")
        sb.append(" Major Dia          | ${result.majorDiaMin} – ${result.majorDiaMax} mm      | ________________ | ______\n")
        sb.append(" Pitch Dia (${result.threadClass})   | ${result.pitchDiaMin} – ${result.pitchDiaMax} mm      | ________________ | ______\n")
        sb.append(" Minor Dia          | ${result.minorDiaMin} – ${result.minorDiaMax} mm      | ________________ | ______\n")
        sb.append(" Pre-Roll Blank Dia | ${result.blankLowerLimit} – ${result.blankUpperLimit} mm      | ________________ | ______\n")
        sb.append(" Length (L)         | per ${result.standard.code} (nominal ${result.lengthMm} mm) | ________________ | ______\n")
        sb.append(" Thread Length (b)  | nominal ${result.threadLengthMm} mm            | ________________ | ______\n")
        sb.append(" GO / NO-GO gauge   | ISO 1502 ${result.threadClass}                 | ________________ | ______\n\n")

        // SECTION 3: METALLURGICAL MATERIAL TEST REPORT (MTC)
        sb.append("3. MATERIAL TEST REQUIREMENTS & HEAT TREATMENT (record measured values)\n")
        sb.append("--------------------------------------------------------------------------------\n")
        sb.append(" Chemical Base Alloy: ${result.material.name}\n")
        sb.append(" Tensile Strength Rm: min ${result.postForgingTargetUtsMpa} MPa   | Measured: ________ MPa\n")
        sb.append(" Yield Strength     : min ${result.yieldStrengthMpa} MPa   | Measured: ________ MPa\n")
        sb.append(" Proof Load         : ${result.proofLoadKn} kN (no permanent set) | Result: ________\n")
        sb.append(" Min Breaking Load  : ${result.tensileBreakingLoadKn} kN          | Measured: ________ kN\n")
        sb.append(" Hardness           : ${result.heatTreatmentSpec.targetHardnessHrc} | Measured: ________\n")
        sb.append(" Quenching / Temper : ${result.heatTreatmentSpec.quenchMedium} @ ${result.heatTreatmentSpec.austenitizingOrSolutionTempC} -> Temper ${result.heatTreatmentSpec.temperingOrAgingTempC}\n")
        sb.append(" De-embrittlement   : ${if (result.hasHydrogenEmbrittlementRisk) "REQUIRED: ${result.heatTreatmentSpec.bakingTempC}, ${result.heatTreatmentSpec.bakingSoakTime}, start within 4 h of plating. Furnace chart ref: ________  ASTM F1940 test: ________" else "NOT REQUIRED"}\n\n")

        // SECTION 4: PROCESS CONTROL PLAN (PCP) & PROCESS FLOW
        sb.append("4. PROCESS CONTROL PLAN (PCP) & FLOW CHART\n")
        sb.append("--------------------------------------------------------------------------------\n")
        sb.append(" Op # | Operation          | Key Characteristic  | Control Method  | Frequency\n")
        sb.append("------+--------------------+---------------------+-----------------+----------\n")
        sb.append(" 10   | Raw Wire Sourcing  | Wire Dia Ø${result.wireDiaMm}mm     | Micrometer      | 1 / Coil \n")
        sb.append(" 20   | Cold/Warm Heading  | Blank Dia Ø${result.blankTargetDia}mm   | Laser Sensor    | 100% Auto\n")
        sb.append(" 30   | Thread Rolling     | Pitch Dia & GO Gauge| ISO 1502 Gauge  | 5 / Shift\n")
        sb.append(" 40   | Heat Treatment     | Hardness & Micro    | Rockwell Tester | 10 / Lot \n")
        sb.append(" 50   | Plating & Baking   | Plating Thickness   | XRF Gauge       | 5 / Lot  \n")
        sb.append(" 60   | Final QC & Packing | GO/NO-GO Acceptance | Optical Sorter  | 100% Auto\n\n")

        // SECTION 5: INITIAL PROCESS STUDY (Cp / Cpk)
        sb.append("5. INITIAL PROCESS CAPABILITY STUDY (Cp / Cpk REPORT)\n")
        sb.append("--------------------------------------------------------------------------------\n")
        sb.append(" Measured Parameter : Pitch Diameter 6g/6H (Critical Feature)\n")
        sb.append(" Sample Size (N)     : 50 Subgroups of 5 (250 Parts Total)\n")
        sb.append(" Target Mean         : ${result.pitchDiaSweet} mm\n")
        sb.append(" Upper Spec Limit USL: ${result.pitchDiaMax} mm\n")
        sb.append(" Lower Spec Limit LSL: ${result.pitchDiaMin} mm\n")
        sb.append(" Measured Mean / Sigma: ________ mm / ________ mm\n")
        sb.append(" Cp = ________   Cpk = ________   (acceptance: Cpk >= 1.67)\n\n")

        // Process sheet: the same forging figures and die stations the app shows
        sb.append("\n--------------------------------------------------------------------------------\n")
        sb.append(" PROCESS SHEET - COLD HEADING\n")
        sb.append("--------------------------------------------------------------------------------\n")
        result.boltForging?.let { f ->
            fun n(v: Double, dp: Int = 2) = String.format(Locale.US, "%.${dp}f", v)
            sb.append(" Wire / shank Ø       : ${n(f.shankDiaMm)} mm  -> blank Ø ${n(f.blankDiaMm, 3)} mm (15°/side cone ${n(f.coneLengthMm)} mm)\n")
            sb.append(" Thread length b      : ${n(f.threadLengthMm, 1)} mm\n")
            sb.append(" Stock to extrude     : ${n(f.stockToExtrudeMm)} mm\n")
            sb.append(" Under head pre-extr. : ${n(f.lengthUnderHeadBeforeMm)} mm\n")
            f.headStockMm?.let { sb.append(" Head stock           : ${n(it)} mm\n") }
            f.finishedVolumeMm3?.let { sb.append(" Finished volume      : ${n(it, 0)} mm3 = ${n(it * f.densityGPerMm3)} g\n") }
            f.cheeseVolumeMm3?.let { sb.append(" Trim cheese          : ${n(it, 0)} mm3 = ${n(it * f.densityGPerMm3)} g\n") }
            f.cutOffLengthMm?.let { sb.append(" CUT-OFF LENGTH       : ${n(it)} mm\n") }
            sb.append(" Head                 : ${f.headNote}\n")
        }
        result.headingPlan?.let { plan ->
            sb.append(" Slug                 : Ø${String.format(Locale.US, "%.2f", plan.slugDiaMm)} x ${String.format(Locale.US, "%.2f", plan.slugLengthMm)} mm, ${plan.upsetBlows} upset blow(s)\n")
            plan.stations.forEach { st -> sb.append("  St ${st.index}: ${st.title} - ${st.operation}\n") }
            plan.offHeaderOps.forEach { op -> sb.append("  Off-header: ${op.title} - ${op.operation}\n") }
            plan.notes.forEach { sb.append("  Note: $it\n") }
        }

        // Status is never "approved" by the app: it can only be blocked by poka-yoke or awaiting measured evidence
        sb.append("================================================================================\n")
        sb.append(
            if (result.isReleaseBlocked) " PPAP STATUS        : BLOCKED - resolve the CRITICAL poka-yoke failures in section 1B\n"
            else " PPAP STATUS        : DRAFT - awaiting measured results, Cpk study and customer approval\n"
        )
        sb.append(" AUTHORIZED SIGNATURE: ______________________  Date: __________\n")
        sb.append("================================================================================\n")

        sb.append("\nNOTE: Generated by AeroFast Pro as an engineering reference. Verify against the current standard, the customer drawing and the qualified process before release.\n")
        return sb.toString()
    }

    private fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10.0 }
        return kotlin.math.round(this * multiplier) / multiplier
    }

    fun exportAndSharePpapPackage(context: Context, result: FastenerCalculationResult) {
        try {
            val ppapText = generatePpapPackageText(result)
            val fileName = "AFP_PPAP_Level3_${result.threadLabel.replace(Regex("[^a-zA-Z0-9]"), "_")}.txt"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { fos ->
                fos.write(ppapText.toByteArray())
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "AeroFast Pro PPAP Level 3 Package - ${result.threadLabel}")
                putExtra(Intent.EXTRA_TEXT, "Attached is the complete Automotive PPAP Level 3 document bundle generated by AeroFast Pro.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Export PPAP Level 3 Package")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
