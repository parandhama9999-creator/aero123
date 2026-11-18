package com.aistudio.aerofast.trmst.data

import java.util.Locale

/** Key production figures for one part, shared by the scanner summary card and the full report. */
data class ProductionSummary(
    val batchQty: Int,
    val wireDiaMm: Double,
    val cutOffLengthMm: Double,
    val finishedWeightG: Double,
    val slugWeightG: Double,
    val finishedKgPer1000: Double,
    val batchFinishedKg: Double,
    val batchRawMaterialKg: Double,
    val piecesPerTonne: Int,
    val plan: RawMaterialPlan?
) {
    val scrapPerPieceG get() = (slugWeightG - finishedWeightG).coerceAtLeast(0.0)
    val scrapPct get() = if (slugWeightG > 0) scrapPerPieceG / slugWeightG * 100.0 else 0.0
}

/** Everything a production planner needs from one drawing, as plain text to share. */
object ProductionReport {
    private fun n(v: Double, dp: Int = 2) = String.format(Locale.US, "%,.${dp}f", v)

    fun summary(r: FastenerCalculationResult): ProductionSummary {
        val density = r.material.density
        val grossVol = if (density > 0) r.grossWeightGrams / density * 1000.0 else 0.0
        val plan = RawMaterialPlanner.plan(
            grossVolumeMm3 = grossVol, densityGPerCm3 = density, batchQty = r.batchQty,
            wireDiameters = RawMaterialPlanner.suggestedDiameters(r.blankTargetDia, r.wireDiaMm),
            headVolumeMm3 = r.headVolumeMm3, rollingBlankDiaMm = r.blankTargetDia
        )
        return ProductionSummary(
            batchQty = r.batchQty,
            wireDiaMm = r.wireDiaMm,
            cutOffLengthMm = r.cutOffLengthMm,
            finishedWeightG = r.netWeightGrams,
            slugWeightG = r.grossWeightGrams,
            finishedKgPer1000 = r.netWeightGrams,
            batchFinishedKg = r.netWeightGrams * r.batchQty / 1000.0,
            batchRawMaterialKg = plan?.batchWeightKg ?: (r.grossWeightGrams * r.batchQty / 1000.0),
            piecesPerTonne = plan?.piecesPerTonne ?: if (r.grossWeightGrams > 0) (1_000_000.0 / r.grossWeightGrams).toInt() else 0,
            plan = plan
        )
    }

    fun build(r: FastenerCalculationResult, spec: ExtractedFastenerSpec? = null, timestamp: String = ""): String = buildString {
        val s = summary(r)
        val line = "=".repeat(72)
        val rule = "-".repeat(72)
        fun section(title: String) { append('\n').append(title).append('\n').append(rule).append('\n') }
        fun row(label: String, value: String) { append(" ").append(label.padEnd(24)).append(": ").append(value).append('\n') }

        append(line).append('\n')
        append(" AEROFAST PRO - PRODUCTION REPORT FROM DRAWING\n")
        append(line).append('\n')
        if (timestamp.isNotBlank()) row("Date", timestamp)
        row("Part", r.fastenerType.label)
        row("Thread x length", "${r.threadLabel} x ${n(r.lengthMm, 1)} mm")
        row("Governing standard", "${r.standard.code} (${r.standard.label})")
        row("Material", "${r.material.key} - ${r.material.name}")
        row("Property class", r.propertyClass?.label ?: "Per material specification")
        row("Coating", r.coating.label)
        row("Batch quantity", "${r.batchQty} pcs")
        spec?.let {
            row("Read by", it.modelUsed)
            if (it.assumed.isNotEmpty()) row("Not on the drawing", "shop default / standard used for: " + it.assumed.joinToString("; "))
        }
        row("Release status", if (r.isReleaseBlocked) "BLOCKED - fix the CRITICAL items in section 1" else "OK to plan - complete the controls in section 1")

        section("1. REALITY CHECK AGAINST THE STANDARD (poka-yoke)")
        append(PokaYokeEngine.reportSection(r.pokaYokeIssues))

        section("2. CUT-OFF LENGTH & WEIGHT")
        row("Wire / shank dia", "${n(s.wireDiaMm)} mm")
        row("CUT-OFF LENGTH", "${n(s.cutOffLengthMm)} mm")
        row("Slug weight (gross)", "${n(s.slugWeightG, 3)} g")
        row("Finished weight (net)", "${n(s.finishedWeightG, 3)} g")
        if (r.partVolumeMm3 > 0) {
            row("Finished volume", "${n(r.partVolumeMm3, 0)} mm3 (head ${n(r.headWeightGrams, 2)} g + shank ${n(r.shankWeightGrams, 2)} g)")
        }
        row("Scrap per piece", "${n(s.scrapPerPieceG, 3)} g (${n(s.scrapPct, 1)} %)")
        row("Weight per 1000 pcs", "${n(s.finishedKgPer1000, 2)} kg finished")
        row("Batch finished weight", "${n(s.batchFinishedKg, 1)} kg for ${s.batchQty} pcs")
        row("Batch raw material", "${n(s.batchRawMaterialKg, 1)} kg (${s.piecesPerTonne} pcs per tonne)")

        s.plan?.let { plan ->
            section("3. RAW MATERIAL - WIRE OPTIONS FOR ${plan.batchQty} PCS")
            append(" Weight is the same at any wire dia; cut-off and length change.\n")
            plan.options.forEach { o ->
                val tag = if (o == plan.recommended) "  <- RECOMMENDED" else ""
                append(" Wire ${n(o.wireDiaMm)} mm: cut-off ${n(o.cutOffLengthMm)} mm, ${n(o.kgPerMetre, 3)} kg/m, ${n(o.batchLengthM, 0)} m$tag\n")
                o.role?.let { append("   ${it}\n") }
                o.cautions.forEach { c -> append("   Caution: $c\n") }
            }
        }

        r.softAnnealSpec?.let { a ->
            section("4. WIRE PREPARATION - SOFT ANNEAL")
            row("Route", a.route.label)
            row("Supplied condition", a.suppliedCondition)
            row("In-house anneal", if (a.inHouseAnnealNeeded) "REQUIRED" else "Not required")
            if (a.temperatureC.isNotBlank()) row("Temperature", a.temperatureC)
            if (a.soakTime.isNotBlank()) row("Soak", a.soakTime)
            if (a.coolingRate.isNotBlank()) row("Cooling", a.coolingRate)
            if (a.atmosphere.isNotBlank()) row("Atmosphere", a.atmosphere)
            if (a.targetUtsMpa.isNotBlank()) row("Target after anneal", listOf(a.targetUtsMpa, a.targetHardnessHb).filter { it.isNotBlank() }.joinToString("; "))
            if (a.route == AnnealRoute.SPHEROIDIZE) {
                AnnealCycleEngine.spheroidize(r.material, AnnealCycleEngine.DEFAULT_CHARGE_TONNES, r.wireDiaMm)?.let { c ->
                    append(" Furnace cycle (${n(c.chargeTonnes, 0)} t charge, wire ${n(r.wireDiaMm)} mm):\n")
                    c.stages.forEach { st -> append("   ${st.name.padEnd(22)} ${st.hoursText.padEnd(12)} ${st.detail}\n") }
                    append("   Total ${c.totalText}\n")
                    c.cautions.forEach { append("   Caution: $it\n") }
                }
            }
        }

        section("5. HEADING - CUT-OFF TO FINISHED HEAD")
        row("Machine", r.recommendedMachine)
        row("Forging load", "${n(r.forgingTonnage, 1)} t")
        row("Upset L/D", n(r.upsetLdRatio, 2))
        r.boltForging?.let { f ->
            row("Blank dia (thread)", "${n(f.blankDiaMm, 3)} mm via 15 deg/side cone ${n(f.coneLengthMm)} mm")
            row("Thread length b", "${n(f.threadLengthMm, 1)} mm")
            row("Stock to extrude", "${n(f.stockToExtrudeMm)} mm")
            f.headStockMm?.let { row("Head stock", "${n(it)} mm") }
            f.cheeseVolumeMm3?.let { row("Trim cheese", "${n(it * f.densityGPerMm3)} g") }
            append(" Head: ${f.headNote}\n")
        }
        r.headingPlan?.let { plan ->
            append(" ${plan.route.label}: slug ${n(plan.slugDiaMm)} x ${n(plan.slugLengthMm)} mm, ${plan.upsetBlows} upset blow(s)\n")
            plan.stations.forEach { st -> append("   St ${st.index}: ${st.title} - ${st.operation}\n") }
            plan.offHeaderOps.forEach { op -> append("   Off-header: ${op.title} - ${op.operation}\n") }
            plan.notes.forEach { append("   Note: $it\n") }
        }

        if (!r.fastenerType.isUnthreaded && !r.fastenerType.isWasher) {
            section("6. THREAD ROLLING")
            row("Rolling blank dia", "${n(r.blankLowerLimit, 3)} - ${n(r.blankUpperLimit, 3)} mm (target ${n(r.blankTargetDia, 3)})")
            row("Major dia", "${n(r.majorDiaMin, 3)} - ${n(r.majorDiaMax, 3)} mm")
            row("Pitch dia (${r.threadClass})", "${n(r.pitchDiaMin, 3)} - ${n(r.pitchDiaMax, 3)} mm (aim ${n(r.pitchDiaSweet, 3)})")
            row("Minor dia", "${n(r.minorDiaMin, 3)} - ${n(r.minorDiaMax, 3)} mm")
            if (r.rootRadiusMinMm != null) row("Root radius", "${n(r.rootRadiusMinMm, 3)} - ${n(r.rootRadiusMaxMm ?: r.rootRadiusMinMm, 3)} mm")
            if (r.prePlatePitchAllowanceUm > 0) row("Pre-plate allowance", "${n(r.prePlatePitchAllowanceUm, 0)} um on pitch dia")
        }

        val ht = r.heatTreatmentSpec
        section("7. HEAT TREATMENT")
        if (ht.atmosphere.isNotBlank()) row("Atmosphere", ht.atmosphere)
        row("Harden / solution", "${ht.austenitizingOrSolutionTempC}, soak ${ht.soakingTimeMinutes}")
        row("Quench", ht.quenchMedium)
        row("Temper / age", "${ht.temperingOrAgingTempC}, ${ht.temperingOrAgingTimeHours}")
        row("Hardness", ht.targetHardnessHrc)
        row("Standard", ht.standardReference)

        section("8. COATING & HYDROGEN EMBRITTLEMENT")
        row("Coating", r.coating.label)
        row("Specification", r.coatingSpecText)
        row("De-embrittlement bake", if (r.hasHydrogenEmbrittlementRisk) "REQUIRED: ${ht.bakingTempC}, ${ht.bakingSoakTime}, start within 4 h of plating" else "Not required")

        section("9. MECHANICAL PROPERTIES REQUIRED")
        row("Tensile Rm min", "${n(r.postForgingTargetUtsMpa, 0)} MPa")
        row("Yield min", "${n(r.yieldStrengthMpa, 0)} MPa")
        row("Elongation min", "${n(r.minElongationPct, 0)} %")
        if (r.stressAreaAsMm2 > 0) row("Stress area As", "${n(r.stressAreaAsMm2, 1)} mm2")
        if (r.proofLoadKn > 0) row("Proof load", "${n(r.proofLoadKn, 1)} kN")
        if (r.tensileBreakingLoadKn > 0) row("Min breaking load", "${n(r.tensileBreakingLoadKn, 1)} kN")

        section("10. RECOMMENDATIONS")
        val recs = buildList {
            r.pokaYokeIssues.sortedBy { it.severity.ordinal }.forEach { add("[${it.severity.name}] ${it.title}: ${it.fix}") }
            // The station-by-station plan in section 5 is the heading route; only the risks are repeated here
            r.forgingRisks.forEach { add("Risk: $it") }
            spec?.assumed?.forEach { add("Confirm with the customer: $it") }
        }.distinct()
        if (recs.isEmpty()) append(" No open items.\n")
        recs.forEachIndexed { i, t -> append(" ${i + 1}. $t\n") }

        append('\n').append(line).append('\n')
        append(" Engineering reference generated by AeroFast Pro. Verify against the customer drawing,\n")
        append(" the current standard and the qualified process before release.\n")
        append(line).append('\n')
    }
}
