package com.aistudio.aerofast.trmst.data

import java.util.Locale

/** One line of a control plan. [special] marks a characteristic the customer usually flags; the symbol is theirs to set. */
data class ControlPlanRow(
    val step: String,
    val characteristic: String,
    val isProduct: Boolean,
    val special: Boolean,
    val spec: String,
    val method: String,
    val sampling: String,
    val control: String,
    val reaction: String
)

data class PpapElement(val no: Int, val name: String, val levels: String, val forThisPart: String)

data class ChecklistPhase(val title: String, val items: List<Pair<String, String>>)

/**
 * APQP, PPAP, control plan, change management, inspection and standards references for the current part.
 * Values come from the calculation and the published standards; anything the plant or the customer decides
 * (sample sizes, salt-spray hours, customer symbols) is labelled as such instead of being invented.
 */
object QualityDocs {
    private fun n(v: Double, dp: Int = 2) = String.format(Locale.US, "%.${dp}f", v)

    private const val REACTION = "Stop, quarantine parts since the last good check, correct, 100 % sort, record"

    private fun isIsoHexBolt(r: FastenerCalculationResult) =
        r.unitSystem == UnitSystem.METRIC && r.standard in setOf(ProductStandard.ISO_4014, ProductStandard.ISO_4017)

    /** ISO 4014 / 4017 product grade: A for d ≤ 24 and l ≤ 10d or 150 mm (whichever is shorter), else B. */
    fun productGradeA(r: FastenerCalculationResult) = r.nominalDiaMm <= 24.0 && r.lengthMm <= minOf(10 * r.nominalDiaMm, 150.0)

    /** ISO 4759-1 tolerances for ISO 4014 / 4017 hex bolts: s h13 (s ≤ 30) or h14; k js14 (A) / js15 (B); l js15 (A). */
    fun hexBoltTolerances(r: FastenerCalculationResult): List<Triple<String, String, String>> {
        val hd = r.headDimensions ?: return emptyList()
        if (!isIsoHexBolt(r) || !hd.isTable) return emptyList()
        val gradeA = productGradeA(r)
        val out = mutableListOf<Triple<String, String, String>>()
        QualityEngine.itGrade(if (hd.acrossFlatsS <= 30.0) 13 else 14, hd.acrossFlatsS)?.let {
            out += Triple("Across flats s", "${n(hd.acrossFlatsS)} −${n(it)} / 0 (${if (hd.acrossFlatsS <= 30.0) "h13" else "h14"})", "Caliper / flat-anvil micrometer")
        }
        QualityEngine.itGrade(if (gradeA) 14 else 15, hd.headHeightK)?.let {
            out += Triple("Head height k", "${n(hd.headHeightK)} ±${n(it / 2)} (${if (gradeA) "js14, grade A" else "js15, grade B"})", "Micrometer / height gauge")
        }
        if (gradeA) QualityEngine.itGrade(15, r.lengthMm)?.let {
            out += Triple("Length l", "${n(r.lengthMm, 1)} ±${n(it / 2)} (js15, grade A)", "Caliper / height gauge")
        } else out += Triple("Length l", "${n(r.lengthMm, 1)} (js17, grade B: see ISO 4759-1)", "Caliper / height gauge")
        out += Triple("Bearing face dw", "${n(hd.bearingDiaDw)} min", "Caliper / profile projector")
        return out
    }

    // ---------------- Control plan ----------------

    fun controlPlan(r: FastenerCalculationResult): List<ControlPlanRow> = buildList {
        val threaded = !r.fastenerType.isUnthreaded && !r.fastenerType.isWasher
        val ht = r.heatTreatmentSpec
        val hardened = !ht.austenitizingOrSolutionTempC.startsWith("N/A")
        val sample = "Set by PFMEA / customer (typical start: first-off, then per shift)"

        add(ControlPlanRow("10 Receive wire", "Wire diameter and grade", true, false,
            "Ø${n(r.wireDiaMm)} mm ${r.material.key}; mill certificate to spec", "Micrometer; mill certificate review",
            "Each coil", "Incoming inspection record", "Reject coil, notify supplier"))
        r.wireCheck?.let {
            add(ControlPlanRow("10 Receive wire", "Wire tensile / hardness for heading", true, false,
                r.rawMaterialSourcingUts, "Mill certificate; tensile test if required", "Each heat / coil",
                "Incoming inspection record", "Hold coil; anneal or reject"))
        }
        add(ControlPlanRow("20 Cut-off & head", "Cut-off length / slug weight", false, false,
            "${n(r.cutOffLengthMm)} mm; ${n(r.grossWeightGrams, 3)} g", "Caliper; balance", sample,
            "Set-up approval, patrol check", REACTION))
        hexBoltTolerances(r).forEach { (name, spec, method) ->
            add(ControlPlanRow("20 Cut-off & head", name, true, false, spec, method, sample, "First-off approval, patrol check", REACTION))
        }
        if (hexBoltTolerances(r).isEmpty()) r.headDimensions?.let { hd ->
            add(ControlPlanRow("20 Cut-off & head", "Head dimensions", true, false,
                "s / dk ${n(hd.acrossFlatsS)}, k ${n(hd.headHeightK)} nominal; tolerances per ${r.standard.code} / drawing", "Caliper / micrometer / profile projector",
                sample, "First-off approval, patrol check", REACTION))
        }
        r.finishedPart?.recesses?.firstOrNull { it.fromTop }?.let {
            add(ControlPlanRow("20 Cut-off & head", "Drive recess", true, false, "Penetration per standard / drawing",
                "Recess penetration gauge (e.g. ISO 4757 cross recess, ISO 10664 hexalobular)", sample, "First-off approval, patrol check", REACTION))
        }
        if (threaded) {
            if (!r.isInternalNut) add(ControlPlanRow("30 Thread roll", "Rolling blank diameter", false, false,
                "${n(r.blankLowerLimit, 3)} – ${n(r.blankUpperLimit, 3)} mm", "Micrometer", sample, "Set-up approval, patrol check", REACTION))
            add(ControlPlanRow("30 Thread roll", "Pitch diameter (thread ${r.threadClass})", true, true,
                "${n(r.pitchDiaMin, 3)} – ${n(r.pitchDiaMax, 3)} mm" + (if (r.prePlatePitchAllowanceUm > 0) " before coating (allowance ${n(r.prePlatePitchAllowanceUm, 1)} µm)" else ""),
                if (r.isInternalNut) "GO / NO-GO plug gauge (${if (r.unitSystem == UnitSystem.INCH) "ASME B1.2" else "ISO 1502"})"
                else "GO / NO-GO ring gauge (${if (r.unitSystem == UnitSystem.INCH) "ASME B1.2" else "ISO 1502"}); three-wire for set-up",
                sample, "First-off approval, patrol check", REACTION))
            add(ControlPlanRow("30 Thread roll", if (r.isInternalNut) "Minor diameter" else "Major diameter", true, false,
                if (r.isInternalNut) "${n(r.minorDiaMin, 3)} – ${n(r.minorDiaMax, 3)} mm" else "${n(r.majorDiaMin, 3)} – ${n(r.majorDiaMax, 3)} mm",
                if (r.isInternalNut) "Plug gauge / bore gauge" else "Micrometer", sample, "Patrol check", REACTION))
        }
        if (hardened) {
            add(ControlPlanRow("40 Heat treat", "Furnace temperature, time, atmosphere", false, true,
                "${ht.austenitizingOrSolutionTempC}; quench ${ht.quenchMedium}; temper ${ht.temperingOrAgingTempC}",
                "Furnace controller record; thermocouple survey (CQI-9 where required)", "Continuous / each load",
                "Furnace chart review", "Hold load, re-test hardness, re-treat or scrap"))
            add(ControlPlanRow("40 Heat treat", "Core hardness", true, true, ht.targetHardnessHrc,
                "Rockwell (ISO 6508-1) / Vickers HV10 referee (ISO 6507-1)", "Each load", "Lab record", "Hold load, re-treat or scrap"))
            add(ControlPlanRow("40 Heat treat", "Surface hardness / decarburisation", true, true,
                "Per ${r.propertyClass?.let { "ISO 898-1 (${it.label.substringBefore(" (")})" } ?: "material spec"}",
                "Vickers HV0.3 surface vs core; metallographic section", "Each load", "Lab record", "Hold load, re-treat or scrap"))
        }
        r.propertyClass?.takeIf { !r.fastenerType.isWasher && !r.fastenerType.isUnthreaded }?.let { pc ->
            add(ControlPlanRow(if (hardened) "40 Heat treat" else "50 Final", "Proof load", true, true,
                "${n(r.proofLoadKn, 1)} kN (${if (r.fastenerType.isNut) "ISO 898-2" else "ISO 898-1"}, ${pc.label.substringBefore(" (")})",
                "Tensile machine, proof load test", "Each load / lot", "Lab record", "Hold lot, investigate"))
            if (!r.fastenerType.isNut) add(ControlPlanRow(if (hardened) "40 Heat treat" else "50 Final", "Minimum tensile (wedge) load", true, true,
                "${n(r.tensileBreakingLoadKn, 1)} kN min (ISO 898-1 wedge test)", "Tensile machine with wedge", "Each load / lot", "Lab record", "Hold lot, investigate"))
        }
        if (r.coating.thicknessMaxUm > 0) {
            add(ControlPlanRow("60 Coat", "Coating thickness", true, false, "${r.coating.specStandard}",
                coatingThicknessMethods(r.coating).firstOrNull()?.second ?: "Per coating spec", "Each lot", "Coater certificate + receiving check", "Hold lot, return to coater"))
            add(ControlPlanRow("60 Coat", "Corrosion resistance", true, false, "Neutral salt spray hours per drawing / customer spec",
                "ISO 9227 NSS / ASTM B117", "Per customer frequency", "Lab report", "Hold lot, investigate coating"))
        }
        if (r.hasHydrogenEmbrittlementRisk) {
            add(ControlPlanRow("60 Coat", "Hydrogen de-embrittlement bake", false, true,
                "${ht.bakingTempC}, ${ht.bakingSoakTime}", "Oven chart; time from plating to bake recorded", "Each load",
                "Oven chart review", "Hold load; embrittlement test before release"))
            add(ControlPlanRow("60 Coat", "Hydrogen embrittlement", true, true, "No fracture",
                "ISO 15330 sustained load test / ASTM F1940 process control", "Per customer frequency", "Lab report", "Hold lot, investigate"))
        }
        if (r.threadLockPatch) add(ControlPlanRow("70 Patch", "Patch position and prevailing torque", true, false,
            "Per drawing (e.g. DIN 267-28 for polyamide patches)", "Visual; torque test", "Each lot", "Patrol check", REACTION))
        add(ControlPlanRow("80 Final inspection", "Appearance, mixed parts", true, false, "No cracks, laps, burrs or foreign parts",
            "Visual; optical sorting where required", "Each lot", "Final inspection record", REACTION))
        add(ControlPlanRow("90 Pack & label", "Label, quantity, traceability", false, false, "Part number, lot and heat traceable",
            "Visual; label check", "Each container", "Packing record", "Re-label, re-count"))
    }

    // ---------------- APQP ----------------

    fun apqp(r: FastenerCalculationResult): List<ChecklistPhase> = listOf(
        ChecklistPhase("1. Plan and define program", listOf(
            "Design goals" to "Application, load and environment of the ${r.fastenerType.label.substringBefore(" (")}",
            "Reliability and quality goals" to "Customer PPM target, field failure history",
            "Preliminary bill of material" to "${r.material.key} wire Ø${n(r.wireDiaMm)} mm, ${r.coating.label}",
            "Preliminary process flow chart" to ProductionReport.processSequence(r).size.let { "$it operations (see Process sequence)" },
            "Preliminary special characteristics" to specialCharacteristics(r).joinToString(", "),
            "Product assurance plan" to "Requirements, reliability targets, test plan",
            "Management support" to "Resources and timing approved"
        )),
        ChecklistPhase("2. Product design and development", listOf(
            "Design FMEA" to "Customer-owned for build-to-print parts; supplier-owned for supplier-designed parts",
            "Design for manufacturability" to "Heading route: ${r.headingPlan?.route?.label ?: "per header"}, upset L/D ${n(r.upsetLdRatio)}",
            "Design verification" to "Proof load ${n(r.proofLoadKn, 1)} kN, hardness ${r.heatTreatmentSpec.targetHardnessHrc}",
            "Design reviews" to "Drawing, standard ${r.standard.code}, thread ${r.threadClass}",
            "Prototype control plan" to "Dimensions, thread, hardness, coating",
            "Engineering drawings and specifications" to "Customer drawing revision recorded",
            "Material specifications" to r.rawMaterialSourcingUts,
            "New equipment, tooling and gauges" to "Header ${r.recommendedMachine}; thread gauges ${r.threadClass}",
            "Team feasibility commitment" to "Signed feasibility review"
        )),
        ChecklistPhase("3. Process design and development", listOf(
            "Packaging standards" to "Customer packaging and labelling spec",
            "Process flow chart" to "From wire receiving to packing (see Control plan steps)",
            "Floor plan layout" to "Header, roller, furnace, coater, sorting",
            "Characteristics matrix" to "Links each characteristic to the operation that creates it",
            "Process FMEA" to "See the 8D / PFMEA tab",
            "Pre-launch control plan" to "Control plan with tighter frequencies",
            "Process instructions" to "Set-up and patrol sheets per operation",
            "MSA plan" to "Gauge R&R on thread, hardness and head dimension gauges",
            "Preliminary capability study plan" to "Ppk on head height, pitch diameter and hardness"
        )),
        ChecklistPhase("4. Product and process validation", listOf(
            "Significant production run" to "1 to 8 h of production, at least 300 consecutive parts unless the customer states otherwise",
            "MSA evaluation" to "%GRR < 10 % acceptable; 10–30 % may be acceptable; > 30 % not acceptable (AIAG MSA)",
            "Preliminary process capability" to "Ppk > 1.67 acceptable; 1.33–1.67 may be acceptable; < 1.33 not acceptable (AIAG PPAP)",
            "PPAP" to "Submit at the level the customer requests (see PPAP)",
            "Production validation testing" to "Mechanical, coating and embrittlement tests on run parts",
            "Packaging evaluation" to "Trial shipment / drop test if required",
            "Production control plan" to "See Control plan",
            "Quality planning sign-off" to "Team and management sign-off"
        )),
        ChecklistPhase("5. Feedback, assessment and corrective action", listOf(
            "Reduced variation" to "Control charts, capability tracking",
            "Customer satisfaction" to "PPM, field returns",
            "Delivery and service" to "On-time delivery, 8D response time",
            "Lessons learned" to "Feed findings back into PFMEA and control plan"
        ))
    )

    fun specialCharacteristics(r: FastenerCalculationResult): List<String> = buildList {
        if (!r.fastenerType.isUnthreaded && !r.fastenerType.isWasher) add("thread pitch diameter")
        if (!r.heatTreatmentSpec.austenitizingOrSolutionTempC.startsWith("N/A")) addAll(listOf("core hardness", "decarburisation"))
        if (r.propertyClass != null && !r.fastenerType.isWasher && !r.fastenerType.isUnthreaded) add("proof load")
        if (r.hasHydrogenEmbrittlementRisk) add("hydrogen embrittlement")
    }

    /** Variable (measured) characteristics where a Ppk study is meaningful. */
    fun capabilityCharacteristics(r: FastenerCalculationResult): List<String> = buildList {
        if (r.headDimensions != null) addAll(listOf("head height k", "across flats / head diameter"))
        if (!r.fastenerType.isUnthreaded && !r.fastenerType.isWasher) add("pitch diameter (thread micrometer / three-wire)")
        if (!r.heatTreatmentSpec.austenitizingOrSolutionTempC.startsWith("N/A")) add("core hardness")
        if (isEmpty()) add("the drawing's key dimensions")
    }

    // ---------------- PPAP (AIAG PPAP 4th edition, Table 4.2) ----------------

    /** Retention / submission per level 1–5: S = submit and retain, R = retain and make available, * = retain, submit if the customer asks. */
    fun ppapElements(r: FastenerCalculationResult): List<PpapElement> {
        val threaded = !r.fastenerType.isUnthreaded && !r.fastenerType.isWasher
        return listOf(
            PpapElement(1, "Design records", "R S S * R", "Customer drawing ${r.standard.code}, balloon every characteristic"),
            PpapElement(2, "Engineering change documents", "R S S * R", "Authorised ECNs not yet on the drawing (see Change management)"),
            PpapElement(3, "Customer engineering approval", "R R S * R", "If the customer requires it"),
            PpapElement(4, "Design FMEA", "R R S * R", "Only if the supplier is design-responsible"),
            PpapElement(5, "Process flow diagram", "R R S * R", "${ProductionReport.processSequence(r).size} operations"),
            PpapElement(6, "Process FMEA", "R R S * R", "See the 8D / PFMEA tab"),
            PpapElement(7, "Control plan", "R R S * R", "${controlPlan(r).size} lines (see Control plan)"),
            PpapElement(8, "Measurement system analysis", "R R S * R", "GR&R on " + (if (threaded) "thread gauges, " else "") + "head gauges, hardness tester"),
            PpapElement(9, "Dimensional results", "R S S * R", "All ballooned characteristics on the PPAP samples"),
            PpapElement(10, "Material / performance test results", "R S S * R",
                listOfNotNull("material cert", r.propertyClass?.let { "proof load ${n(r.proofLoadKn, 1)} kN" }, "hardness",
                    r.coating.takeIf { it.thicknessMaxUm > 0 }?.let { "coating thickness and salt spray" },
                    "hydrogen embrittlement".takeIf { r.hasHydrogenEmbrittlementRisk }).joinToString(", ")),
            PpapElement(11, "Initial process studies", "R R S * R", "Ppk on measured characteristics: " + capabilityCharacteristics(r).joinToString(", ") + " (pass / fail tests are not capability studies)"),
            PpapElement(12, "Qualified laboratory documentation", "R S S * R", "Lab scope (ISO/IEC 17025 or customer-approved)"),
            PpapElement(13, "Appearance approval report", "S S S * R", "Only for appearance items"),
            PpapElement(14, "Sample production parts", "R S S * R", "Quantity per customer"),
            PpapElement(15, "Master sample", "R R R * R", "Retain a signed sample"),
            PpapElement(16, "Checking aids", "R R R * R", if (threaded) "Thread gauges ${r.threadClass}, recess / special gauges" else "Special gauges"),
            PpapElement(17, "Customer-specific requirements", "R R S * R", "See the customer requirements list"),
            PpapElement(18, "Part submission warrant (PSW)", "S S S S R", "Part weight ${n(r.netWeightGrams / 1000.0, 4)} kg"),
            PpapElement(19, "Bulk material requirements checklist", "R R S * R", "Bulk materials only")
        )
    }

    val PSW_REASONS = listOf(
        "Initial submission", "Engineering change(s)", "Tooling: transfer, replacement, refurbishment or additional",
        "Correction of discrepancy", "Tooling inactive > 1 year", "Change to optional construction or material",
        "Sub-supplier or material source change", "Change in part processing", "Parts produced at additional location", "Other"
    )

    /** Customer notification cases from AIAG PPAP 4th edition, section 3.1; the customer's CSR can add more. */
    val CHANGE_TRIGGERS = listOf(
        "Use of other construction or material than in the approved part",
        "Production from new or modified tools (except perishable tools), dies or moulds, including additional or replacement tooling",
        "Production after upgrade or rearrangement of existing tooling or equipment",
        "Production from tooling and equipment transferred to a different or additional plant location",
        "Change of supplier for parts, non-equivalent materials or services (e.g. heat treatment, plating)",
        "Product produced after tooling has been inactive for volume production for 12 months or more",
        "Product and process changes on components made internally or by suppliers",
        "Change in test / inspection method (new technique, no change to acceptance criteria)",
        "Bulk materials: new source of raw material, or change in appearance attributes"
    )

    fun psw(r: FastenerCalculationResult, partNo: String, drawingRev: String, customer: String, level: Int, reason: String, csr: String): String = buildString {
        val rule = "-".repeat(60)
        append("PART SUBMISSION WARRANT (AIAG PPAP 4th ed. format)\n").append(rule).append('\n')
        append("Part name        : ${r.fastenerType.label}\n")
        append("Customer part no.: ${partNo.ifBlank { "________" }}\n")
        append("Drawing revision : ${drawingRev.ifBlank { "________" }}\n")
        append("Customer         : ${customer.ifBlank { "________" }}\n")
        append("Size             : ${r.threadLabel} x ${n(r.lengthMm, 1)} mm\n")
        append("Material         : ${r.material.key} - ${r.material.name}\n")
        append("Strength         : ${r.tensileRange?.label ?: r.propertyClass?.label ?: "per material spec"}\n")
        append("Coating          : ${r.coating.label} (${r.coating.specStandard})\n")
        append("Part weight      : ${n(r.netWeightGrams / 1000.0, 4)} kg\n")
        append("Submission level : $level\n")
        append("Reason           : $reason\n")
        if (csr.isNotBlank()) append("Customer-specific requirements:\n").append(csr.lines().filter { it.isNotBlank() }.joinToString("\n") { " - ${it.trim()}" }).append('\n')
        append(rule).append('\n')
        append("ELEMENTS FOR LEVEL $level (S = submit, R = retain, * = per customer)\n")
        ppapElements(r).forEach { e ->
            val code = e.levels.split(' ').getOrNull(level - 1) ?: "?"
            append(String.format(Locale.US, " %2d %-38s %s  %s\n", e.no, e.name, code, e.forThisPart))
        }
        append(rule).append('\n')
        append("Declaration: samples are representative of parts made by the production process, from a significant\n")
        append("production run, and meet all drawing and specification requirements.\n")
        append("Supplier signature / date: __________________\n")
    }

    // ---------------- Engineering change ----------------

    fun ecn(r: FastenerCalculationResult, number: String, fromRev: String, toRev: String, description: String, reason: String,
            triggers: Set<Int>, csr: String): String = buildString {
        val rule = "-".repeat(60)
        append("ENGINEERING CHANGE NOTICE ${number.ifBlank { "________" }}\n").append(rule).append('\n')
        append("Part      : ${r.fastenerType.label} ${r.threadLabel} x ${n(r.lengthMm, 1)} mm\n")
        append("Revision  : ${fromRev.ifBlank { "__" }} -> ${toRev.ifBlank { "__" }}\n")
        append("Change    : ${description.ifBlank { "________" }}\n")
        append("Reason    : ${reason.ifBlank { "________" }}\n")
        append(rule).append('\n')
        if (triggers.isEmpty()) append("No PPAP customer-notification case selected.\n")
        else {
            append("CUSTOMER NOTIFICATION REQUIRED (AIAG PPAP section 3.1):\n")
            triggers.sorted().forEach { append(" - ${CHANGE_TRIGGERS[it]}\n") }
            append("Submit PPAP before shipping changed parts unless the customer waives it.\n")
        }
        if (csr.isNotBlank()) append("Check also: ${csr.lines().filter { it.isNotBlank() }.joinToString("; ")}\n")
        append(rule).append('\n')
        append("DOCUMENTS TO UPDATE\n")
        listOf("Drawing / specification", "Process flow", "PFMEA", "Control plan", "Process and inspection instructions",
            "Gauges and MSA", "Tooling drawings", "PPAP file / PSW", "Packaging and labels").forEach { append(" [ ] $it\n") }
        append(rule).append('\n')
        append("Stock: [ ] use up  [ ] rework  [ ] scrap   Effective from lot / date: ________\n")
        append("Approved: Engineering ______  Quality ______  Production ______  Customer ______\n")
    }

    // ---------------- Coating verification ----------------

    /** Thickness or coating-weight methods for this coating, most usual first. */
    fun coatingThicknessMethods(c: CoatingType): List<Pair<String, String>> = when (c) {
        CoatingType.ZN_NI -> listOf("XRF" to "X-ray fluorescence ISO 3497 / ASTM B568 (also reads the Ni %)", "Magnetic" to "Magnetic induction ISO 2178 / ASTM B499", "Referee" to "Microscopic cross-section ISO 1463 / ASTM B487")
        CoatingType.ZINC_BLUE, CoatingType.ZINC_YELLOW, CoatingType.AERO_CAD, CoatingType.AERO_CAD_CL2, CoatingType.AERO_CAD_CL3, CoatingType.AERO_CAD_LHE ->
            listOf("Magnetic" to "Magnetic induction ISO 2178 / ASTM B499", "XRF" to "X-ray fluorescence ISO 3497 / ASTM B568", "Coulometric" to "Coulometric ISO 2177 / ASTM B504", "Referee" to "Microscopic cross-section ISO 1463 / ASTM B487")
        CoatingType.GEOMET, CoatingType.DACROMET -> listOf("Coating weight" to "Coating mass per unit area (ISO 10683)", "Magnetic" to "Magnetic induction ISO 2178 on the reference area", "Referee" to "Microscopic cross-section ISO 1463")
        CoatingType.PHOSPHATE, CoatingType.PHOSPHATE_MN -> listOf("Coating weight" to "Gravimetric coating mass ISO 3892 (g/m²)")
        CoatingType.HOT_DIP_GALV -> listOf("Magnetic" to "Magnetic induction ISO 2178", "Gravimetric" to "Gravimetric ISO 1460 (ISO 10684)")
        CoatingType.ENP -> listOf("XRF" to "X-ray fluorescence ISO 3497", "Referee" to "Microscopic cross-section ISO 1463")
        CoatingType.AERO_IVD, CoatingType.PTFE_XYLAN -> listOf("Magnetic" to "Magnetic induction ISO 2178 / ASTM B499")
        CoatingType.PASSIVATION, CoatingType.PASSIVATION_CITRIC -> listOf("No thickness" to "Verify per ASTM A967: water immersion, humidity, salt spray or copper sulphate test")
        CoatingType.BLACK_OXIDE -> listOf("No thickness" to "Appearance and oil film; corrosion test per MIL-DTL-13924")
        else -> listOf("Per spec" to "Per the coating specification")
    }

    // ---------------- Inspection, testing and GD&T references ----------------

    val TEST_METHODS = listOf(
        "Tensile and wedge load (bolts)" to "ISO 898-1 full-size tests; ISO 6892-1 machined test pieces",
        "Proof load (bolts / nuts)" to "ISO 898-1 / ISO 898-2; inch: ASTM F606",
        "Hardness" to "Rockwell ISO 6508-1, Vickers ISO 6507-1 (HV10 referee); surface HV0.3 for decarburisation",
        "Torque-tension / friction" to "ISO 16047 (total, thread and head friction coefficients)",
        "Prevailing torque" to "ISO 2320 (lock nuts); DIN 267-28 (polyamide patch)",
        "Hydrogen embrittlement" to "ISO 15330 sustained load; ASTM F1940 process control; ASTM F519 (aerospace)",
        "Salt spray" to "ISO 9227 NSS / ASTM B117; hours to white and red corrosion per drawing",
        "Thread gauging" to "ISO 1502 GO / NO-GO (metric); ASME B1.2 gauges, ASME B1.3 system 21 / 22 / 23 (inch)",
        "Dimensions" to "Caliper, micrometer, height gauge; profile projector for radii and chamfers; CMM / vision for GD&T",
        "Drive recess" to "Penetration gauges: ISO 4757 (cross recess), ISO 10664 (hexalobular)"
    )

    val GDT = listOf(
        Triple("Straightness", "Shank / thread axis", "V-blocks + dial indicator, or CMM"),
        Triple("Perpendicularity / total runout", "Bearing face to thread axis", "Threaded collet + dial indicator on the bearing face"),
        Triple("Coaxiality / position", "Head or socket to shank", "CMM, vision system, or functional gauge"),
        Triple("Symmetry", "Slot to head axis", "Profile projector / vision"),
        Triple("Position", "Cross hole, split-pin hole", "Functional pin gauge or CMM"),
        Triple("Profile / angle", "Countersink head, cone seat", "Profile projector, angle gauge")
    )

    // ---------------- Standards cross-reference ----------------

    val JIS_EQUIVALENTS = listOf(
        "JIS B 1180" to "ISO 4014 / 4017 / 4018 hex bolts and screws (old JIS sizes in the annex)",
        "JIS B 1181" to "ISO 4032 / 4033 / 4035 hex nuts",
        "JIS B 1176" to "ISO 4762 hexagon socket head cap screws",
        "JIS B 1177" to "ISO 4026 – 4029 hexagon socket set screws",
        "JIS B 1101" to "ISO 1207 / 1580 / 2009 / 2010 slotted head screws",
        "JIS B 1111" to "ISO 7045 / 7046 / 7047 cross recessed head screws",
        "JIS B 1256" to "ISO 7089 / 7090 / 7092 / 7093 plain washers",
        "JIS B 1051" to "ISO 898-1 bolt mechanical properties",
        "JIS B 1052-2" to "ISO 898-2 nut mechanical properties",
        "JIS B 0205" to "ISO 261 / 262 / 724 / 965-1 metric threads",
        "JIS B 0209" to "ISO 965 metric thread tolerances"
    )

    /** Old JIS B 1180 annex across-flats that differ from ISO (d to JIS s and ISO s). */
    val JIS_OLD_ACROSS_FLATS = listOf(Triple(10.0, 17.0, 16.0), Triple(12.0, 19.0, 18.0), Triple(14.0, 22.0, 21.0), Triple(22.0, 32.0, 34.0))

    // ---------------- Lean / Six Sigma ----------------

    val DMAIC = listOf(
        "Define" to "Problem, customer impact, scope, goal (e.g. cut head crack PPM by 80 %)",
        "Measure" to "Baseline PPM / Cpk; validate gauges with GR&R first",
        "Analyze" to "Root cause: 5-Why, fishbone, data by machine / shift / coil / heat",
        "Improve" to "Fix and verify with data (tooling, wire, settings, poka-yoke)",
        "Control" to "Update PFMEA and control plan, SPC, standard work"
    )

    val WASTES = listOf(
        "Transport" to "Bins moved between header, roller and furnace",
        "Inventory" to "WIP waiting for heat treatment or plating",
        "Motion" to "Searching for gauges and tools",
        "Waiting" to "Die change, first-off approval, furnace loading",
        "Over-production" to "Running beyond the order to avoid a die change",
        "Over-processing" to "Tighter tolerances than the drawing asks",
        "Defects" to "Cracks, mixed parts, thread damage, sorting",
        "Skills" to "Operator knowledge not used for improvement"
    )
}
