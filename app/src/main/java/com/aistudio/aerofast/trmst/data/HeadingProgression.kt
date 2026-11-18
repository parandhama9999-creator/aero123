package com.aistudio.aerofast.trmst.data

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class SectionShape { ROUND, HEX, SQUARE, TWELVE_POINT, DOME, HEXALOBULAR }

private val HEX_AREA_FACTOR = sqrt(3.0) / 2.0

/** Two hexagons 30° apart (bi-hex): 2 × hexagon − their common dodecagon = (√3 − 3·tan 15°)·s². */
private val TWELVE_POINT_AREA_FACTOR = sqrt(3.0) - 3.0 * kotlin.math.tan(Math.toRadians(15.0))

/** Cross-section area; size is the diameter of a round or dome, across flats of a hex / 12-point, the side of a square. */
internal fun sectionArea(shape: SectionShape, size: Double) = when (shape) {
    SectionShape.HEX -> HEX_AREA_FACTOR * size * size
    SectionShape.TWELVE_POINT -> TWELVE_POINT_AREA_FACTOR * size * size
    SectionShape.SQUARE -> size * size
    // Hexalobular recess, size = point-to-point A; inner lobes B ≈ 0.713 A (ISO 10664): a circle of the mean of A and B
    SectionShape.HEXALOBULAR -> PI / 4.0 * (0.8565 * size) * (0.8565 * size)
    else -> PI / 4.0 * size * size
}

/**
 * One axial slice of the part, listed from the die end (bottom) to the punch end (top).
 * ROUND is a cylinder or cone; HEX / TWELVE_POINT are prisms sized across flats; SQUARE is a prism sized by its side;
 * DOME is a spherical cap on a base of diameter dBottomMm.
 */
data class ProfileSegment(
    val heightMm: Double,
    val dBottomMm: Double,
    val dTopMm: Double = dBottomMm,
    val shape: SectionShape = SectionShape.ROUND
) {
    val volumeMm3: Double
        get() = when (shape) {
            SectionShape.ROUND -> PI * heightMm / 12.0 * (dBottomMm * dBottomMm + dBottomMm * dTopMm + dTopMm * dTopMm)
            SectionShape.DOME -> PI * heightMm / 6.0 * (3.0 * (dBottomMm / 2.0) * (dBottomMm / 2.0) + heightMm * heightMm)
            else -> sectionArea(shape, dBottomMm) * heightMm
        }

    /** Width seen in side view; a hex or 12-point is drawn across corners. */
    val maxWidthMm: Double
        get() = when (shape) {
            SectionShape.HEX, SectionShape.TWELVE_POINT -> dBottomMm * 2.0 / sqrt(3.0)
            SectionShape.ROUND -> max(dBottomMm, dTopMm)
            else -> dBottomMm
        }

    /** Largest cross-section, which the forming load acts on. */
    val crossSectionMm2: Double
        get() = when (shape) {
            SectionShape.ROUND -> sectionArea(shape, max(dBottomMm, dTopMm))
            else -> sectionArea(shape, dBottomMm)
        }
}

/** Blind cavity (indent or socket) entering from the top or bottom face. */
data class Recess(val fromTop: Boolean, val sizeMm: Double, val depthMm: Double, val shape: SectionShape) {
    val volumeMm3: Double get() = sectionArea(shape, sizeMm) * depthMm
}

data class HeadingStation(
    /** 0 = shear cut-off, 1..n = die stations. */
    val index: Int,
    val title: String,
    val operation: String,
    val segments: List<ProfileSegment>,
    val recesses: List<Recess> = emptyList(),
    val throughHoleMm: Double? = null,
    /** Length of the part held inside the die bore, measured from the bottom. */
    val inDieMm: Double = 0.0,
    /** Material removed at this station (e.g. the pierced nut web). */
    val scrapMm3: Double = 0.0,
    /** The blow that needs the full forming load. */
    val isPeakBlow: Boolean = false
) {
    val isCutOff: Boolean get() = index == 0
    val heightMm: Double get() = segments.sumOf { it.heightMm }

    /** Diameter of the part where it sits in the die; segments run die end first. */
    val inDieDiaMm: Double? get() = if (isCutOff) null else segments.firstOrNull()?.maxWidthMm

    /** Bore this station's die is cut to: the stock it holds, plus the transfer clearance. */
    val dieBoreMm: Double?
        get() = inDieDiaMm?.let { it + HeadingProgression.stageClearance(it) }
    val maxWidthMm: Double get() = segments.maxOf { it.maxWidthMm }
    val volumeMm3: Double
        get() = segments.sumOf { it.volumeMm3 } - recesses.sumOf { it.volumeMm3 } -
            (throughHoleMm?.let { sectionArea(SectionShape.ROUND, it) * heightMm } ?: 0.0)
}

data class OffHeaderOp(val title: String, val operation: String)

/** The machine the part is headed on. The two bolt routes give different station sequences, not just a different name. */
enum class HeaderRoute(val label: String) {
    /** Blanks transfer from die to die: extrusion, pre-upsets, socket back-extrusion and trimming are all in-line. */
    MULTI_STATION("Multi-station progressive former"),
    NUT_FORMER("Nut former"),
    PRESS("Progressive press"),
    /** One die, two punches: cone / extrusion blow, then the finishing blow. Hex heads are formed, not trimmed. */
    TWO_BLOW("1-die 2-blow header")
}

/** The machine asked for on screen. AUTO picks the 1-die 2-blow header only where it is enough for the part. */
enum class HeaderChoice(val label: String) {
    AUTO("Auto"),
    TWO_BLOW("1-die 2-blow"),
    MULTI_STATION("Multi-station")
}

data class HeadingPlan(
    val slugDiaMm: Double,
    val slugLengthMm: Double,
    val slugVolumeMm3: Double,
    val stations: List<HeadingStation>,
    val offHeaderOps: List<OffHeaderOp>,
    /** Upsetting blows the head stock needs (1 ≤ L/D 2.3, 2 ≤ 4.5, else 3). */
    val upsetBlows: Int,
    val notes: List<String>,
    val route: HeaderRoute = HeaderRoute.MULTI_STATION,
    /** Why this route was chosen, or why a route that was asked for cannot make the part. */
    val routeReason: String = "",
    /** Set when the chosen machine cannot make the part as designed: the reason, for the poka-yoke gate. */
    val feasibilityIssue: String? = null
) {
    val dieStations: Int get() = stations.count { !it.isCutOff }

    /** "1-die 2-blow header", "4-die progressive former". */
    val machineDescription: String get() = when (route) {
        HeaderRoute.MULTI_STATION -> "$dieStations-die progressive former"
        HeaderRoute.NUT_FORMER -> "$dieStations-die nut former"
        HeaderRoute.PRESS -> "progressive press"
        HeaderRoute.TWO_BLOW -> "1-die 2-blow header"
    }

    /** Largest relative gap between the slug volume and (station volume + scrap removed so far). */
    val maxVolumeError: Double
        get() {
            var scrap = 0.0
            return stations.maxOfOrNull { s ->
                scrap += s.scrapMm3
                abs(s.volumeMm3 + scrap - slugVolumeMm3) / slugVolumeMm3
            } ?: 0.0
        }
}

/**
 * Builds the cold-heading station sequence for a fastener type. Every station holds the slug volume
 * (constant-volume plastic flow) and the last one is the finished shape of that type, so the drawing
 * and the stage list describe a manufacturable progression.
 * Upset limits follow the classic heading rules: free stock L/D ≤ 2.3 in one blow,
 * ≤ 4.5 with one cone pre-upset, beyond that two cones.
 */
object HeadingProgression {
    const val SINGLE_BLOW_LD = 2.3
    const val TWO_BLOW_LD = 4.5

    /**
     * Diametral clearance between the stock and the die bore, so a blank transfers into the next
     * die instead of jamming on its lip.
     *
     * The 0.02 mm floor and 0.09 mm ceiling are the shop rule; the scaling between them is linear in
     * diameter because no finer breakdown was given. Treat the figure as a starting point for the
     * die drawing, not a toleranced dimension.
     */
    const val MIN_STAGE_CLEARANCE_MM = 0.02
    const val MAX_STAGE_CLEARANCE_MM = 0.09
    private const val CLEARANCE_MIN_DIA = 3.0
    private const val CLEARANCE_MAX_DIA = 24.0

    fun stageClearance(diaMm: Double): Double {
        val t = ((diaMm - CLEARANCE_MIN_DIA) / (CLEARANCE_MAX_DIA - CLEARANCE_MIN_DIA)).coerceIn(0.0, 1.0)
        val raw = MIN_STAGE_CLEARANCE_MM + t * (MAX_STAGE_CLEARANCE_MM - MIN_STAGE_CLEARANCE_MM)
        return kotlin.math.round(raw / 0.01) * 0.01 // die shops cut to 0.01 mm
    }
    const val THREE_BLOW_LD = 6.5

    fun build(
        type: FastenerType,
        wireDiaMm: Double,
        part: FinishedPart,
        lengthMm: Double,
        threadLabel: String,
        forging: BoltForgingResult? = null,
        forgedHeadDiaMm: Double? = null,
        trimmedHexS: Double = 0.0,
        trimmedHexK: Double = 0.0,
        flangeDiaMm: Double? = null,
        flangeThicknessMm: Double? = null,
        choice: HeaderChoice = HeaderChoice.AUTO
    ): HeadingPlan? {
        if (part.style == HeadStyle.WASHER) return null
        if (part.style == HeadStyle.NUT) return nut(type, wireDiaMm, part, threadLabel)
        val headed = part.style != HeadStyle.HEADLESS && part.headVolumeMm3 > 0.0
        if (headed && choice == HeaderChoice.TWO_BLOW) return twoBlow(type, part, threadLabel, chosen = true)
        if (headed && choice == HeaderChoice.AUTO && forging == null && twoBlowIsEnough(type, part, wireDiaMm))
            return twoBlow(type, part, threadLabel, chosen = false)
        val reason = when {
            part.style == HeadStyle.HEADLESS -> "Progressive former: the thread sections are extruded and there is no head to upset."
            choice == HeaderChoice.MULTI_STATION -> "Multi-station progressive former (your choice): extrusion, pre-upsets, socket back-extrusion and trimming each run in their own station."
            else -> "Progressive former: extrusion, pre-upsets, socket back-extrusion and trimming each run in their own station."
        }
        val plan = when {
            forging != null && forgedHeadDiaMm != null ->
                trimmedHex(type, lengthMm, forging, forgedHeadDiaMm, trimmedHexS, trimmedHexK, threadLabel, flangeDiaMm, flangeThicknessMm, part.cautions)
            part.style == HeadStyle.HEADLESS || part.headVolumeMm3 <= 0.0 -> headless(type, wireDiaMm, part, threadLabel)
            else -> headed(type, wireDiaMm, part, threadLabel)
        }
        return plan.copy(route = HeaderRoute.MULTI_STATION, routeReason = reason)
    }

    /** Largest area reduction a single closed die takes in the first blow before the shank upsets instead of extruding. */
    const val MAX_DIE_EXTRUSION_PCT = 30.0

    /** Auto: one die is enough when nothing is extruded, the head needs ≤ 2 blows and is formed, not trimmed. */
    private fun twoBlowIsEnough(type: FastenerType, part: FinishedPart, wire: Double): Boolean {
        val lt = part.threadLengthMm
        val extrude = lt > 0.0 && (1.0 - area(part.blankDiaMm) / area(wire)) * 100.0 >= 1.0
        val ld = part.headVolumeMm3 / area(wire) / wire
        val upsetIsFinal = part.style == HeadStyle.RIVET || part.style == HeadStyle.ROUND_PIN
        return !extrude && ld <= TWO_BLOW_LD && !upsetIsFinal && (type.category == "Machine Screws" || part.style == HeadStyle.HEX_FORMED)
    }

    /**
     * The part designed for a 1-die 2-blow header. The wire is the largest shank diameter; when the head would need a
     * third blow the wire is enlarged until two blows are enough, as long as the die can extrude it back down. The
     * first blow extrudes the shank in the closed die (when it is reduced) and gathers the head in the cone punch; the
     * second forms the head and any recess. A hex or 12-point head is closed in the punch insert: no trim, no cheese.
     */
    private fun twoBlow(type: FastenerType, part: FinishedPart, threadLabel: String, chosen: Boolean): HeadingPlan {
        val body = part.dieSegments
        val inDie = body.sumOf { it.heightMm }
        val headVol = part.headVolumeMm3
        val minD = body.minOfOrNull { min(it.dBottomMm, it.dTopMm) } ?: part.blankDiaMm
        val maxD = body.maxOfOrNull { max(it.dBottomMm, it.dTopMm) } ?: part.blankDiaMm
        fun ldAt(w: Double) = headVol / area(w) / w
        fun reductionAt(w: Double) = (1.0 - area(minD) / area(w)) * 100.0
        var wire = kotlin.math.ceil(maxD * 100.0 - 1e-6) / 100.0
        var issue: String? = null
        var enlarged = false
        if (ldAt(wire) > TWO_BLOW_LD) {
            // Two blows need L/D ≤ 4.5: L/D = 4V / (π D³), so D ≥ (4V / (4.5 π))^⅓, drawn in 0.05 mm steps
            val need = kotlin.math.ceil(Math.cbrt(4.0 * headVol / (PI * TWO_BLOW_LD)) / 0.05 - 1e-9) * 0.05
            if (reductionAt(need) <= MAX_DIE_EXTRUSION_PCT) { wire = need; enlarged = true }
            else issue = "the head stock L/D is ${f(ldAt(wire))} on Ø${f(wire)} wire, more than two blows can gather ($TWO_BLOW_LD); " +
                "wire large enough for two blows (Ø${f(need)}) would have to be extruded ${f(reductionAt(need), 0)} % in the die, over the ~${MAX_DIE_EXTRUSION_PCT.toInt()} % one die takes"
        }
        val reductionPct = reductionAt(wire).coerceAtLeast(0.0)
        if (issue == null && reductionPct > MAX_DIE_EXTRUSION_PCT)
            issue = "extruding Ø${f(wire)} → Ø${f(minD)} is ${f(reductionPct, 0)} % area reduction, over the ~${MAX_DIE_EXTRUSION_PCT.toInt()} % one die takes in the first blow"
        val aw = area(wire)
        val slugVol = part.volumeMm3          // formed, not trimmed: the slug is the finished part
        val slugLen = slugVol / aw
        val stock = headVol / aw
        val ld = stock / wire
        val top = part.headSegments.last()
        val prism = top.shape == SectionShape.HEX || top.shape == SectionShape.TWELVE_POINT
        // The preform must enter the finishing cavity: the inscribed circle of a prism head, the diameter otherwise
        val dCavity = if (prism) 0.96 * top.dBottomMm else top.maxWidthMm
        var hc = stock * 0.62
        var dc = coneLargeEnd(headVol, wire, hc)
        if (dc > 0.9 * dCavity) {
            dc = 0.9 * dCavity
            hc = 12.0 * headVol / (PI * (dc * dc + dc * wire + wire * wire))
        }
        val extrude = reductionPct >= 1.0
        val stations = mutableListOf(cutOff(wire, slugLen))
        stations += HeadingStation(
            index = 1,
            title = if (extrude) "1st blow: extrude & cone preform" else "1st blow: cone preform",
            operation = (if (extrude) "Forward-extrude the shank in the closed die Ø${f(wire)} → Ø${f(minD, 3)} (${f(reductionPct, 0)} % area reduction); " else "") +
                "gather head stock ${f(stock, 1)} mm (L/D ${f(ld)}) in the cone punch: Ø${f(dc)} → Ø${f(wire)} × ${f(hc, 1)} mm",
            segments = body + ProfileSegment(hc, dc, wire),
            inDieMm = inDie
        )
        val (title, operation) = finishingBlow(type, part)
        stations += HeadingStation(
            index = 2, title = "2nd blow: $title", operation = operation,
            segments = part.segments, recesses = part.recesses, inDieMm = inDie, isPeakBlow = true
        )
        val notes = buildList {
            issue?.let { add("NOT FEASIBLE on a 1-die 2-blow header: $it. Choose Multi-station, or change the head / wire.") }
            if (enlarged) add("Wire enlarged to Ø${f(wire)} so the head gathers in two blows; the shank is extruded back to Ø${f(minD, 3)} in the first blow.")
            else if (!extrude && part.threadLengthMm > 0.0) add("Wire is drawn to the rolling blank Ø${f(minD, 3)} mm: the shank is not extruded, the thread is rolled straight onto it.")
            if (prism) add("Formed head, no trim: the ${if (top.shape == SectionShape.HEX) "hex" else "12-point"} is closed in the punch insert, so there is no trim cheese; corners are slightly fuller than a trimmed head.")
            if (part.recesses.any { it.fromTop }) add("The recess is back-extruded in the finishing blow.")
            addAll(part.cautions)
        }
        val reason = when {
            issue != null -> "1-die 2-blow header (your choice) cannot make this part as drawn: $issue."
            chosen -> "1-die 2-blow header (your choice): ${if (extrude) "shank extruded in the die and head gathered in the first blow" else "head gathered in the first blow"}, head${if (prism) " formed (no trim)" else ""} finished in the second."
            else -> "1-die 2-blow header (auto): no extrusion and head L/D ${f(ld)} ≤ $TWO_BLOW_LD, so one die with a cone blow and a finishing blow makes the part. Choose Multi-station to run it on the progressive former."
        }
        return HeadingPlan(wire, slugLen, slugVol, stations, offHeaderOps(type, threadLabel), 2, notes,
            route = HeaderRoute.TWO_BLOW, routeReason = reason, feasibilityIssue = issue)
    }


    private fun area(d: Double) = sectionArea(SectionShape.ROUND, d)
    private fun f(v: Double, decimals: Int = 2) = String.format(java.util.Locale.US, "%.${decimals}f", v)

    private fun cutOff(wire: Double, length: Double) = HeadingStation(
        index = 0,
        title = "Shear cut-off",
        operation = "Shear slug Ø${f(wire)} × ${f(length, 1)} mm from wire coil",
        segments = listOf(ProfileSegment(length, wire))
    )

    /** Work done on the blank after it leaves the header: differs by fastener type. */
    private fun offHeaderOps(type: FastenerType, threadLabel: String): List<OffHeaderOp> {
        val point = OffHeaderOp("Pointing", "Chamfer thread start to minor diameter (45°)")
        val roll = OffHeaderOp("Thread rolling", "Roll $threadLabel on flat or cylindrical dies")
        val rollAfterHt = OffHeaderOp("Thread rolling", "Roll $threadLabel after heat treatment (controlled root radius)")
        val filletRoll = OffHeaderOp("Fillet rolling", "Cold-roll the head-to-shank fillet after heat treatment")
        val tap = OffHeaderOp("Tapping", "Tap $threadLabel on bent-shank nut tapper")
        val deflect = OffHeaderOp("Locking deflection", "Deflect the top threads (oval / tri-lobe) for the prevailing torque")
        return when (type) {
            FastenerType.SOLID_RIVET, FastenerType.BLIND_RIVET -> emptyList()
            FastenerType.SOCKET_SET -> listOf(OffHeaderOp("Point forming", "Form the cup / cone / dog point on the end opposite the socket"), roll)
            FastenerType.TWELVE_POINT -> listOf(point, rollAfterHt, filletRoll)
            FastenerType.NAS_CLOSE_TOL_BOLT ->
                listOf(OffHeaderOp("Shank grinding", "Centreless grind the shank to the close-tolerance diameter"), point, rollAfterHt, filletRoll)
            FastenerType.HI_LOK_PIN -> listOf(
                OffHeaderOp("Shank grinding", "Centreless grind the shank to the interference-fit diameter"), rollAfterHt,
                OffHeaderOp("Key recess", "Broach the hex key recess in the threaded end")
            )
            FastenerType.TAPER_LOK -> listOf(OffHeaderOp("Taper grinding", "Grind the tapered shank to the OEM taper"), rollAfterHt)
            FastenerType.LOCKBOLT_PIN -> listOf(
                OffHeaderOp("Groove rolling", "Roll the annular lock grooves and the pintail pull grooves"),
                OffHeaderOp("Breakneck", "Form the breakneck groove between the lock grooves and the pintail")
            )
            FastenerType.BLIND_BOLT -> listOf(point, roll, OffHeaderOp("Assembly", "Assemble the core bolt with its sleeve and drive nut"))
            FastenerType.BANJO_BOLT -> listOf(OffHeaderOp("Drilling", "Drill the axial bore and the cross hole"), point, roll)
            FastenerType.THREAD_FORMING_SCREW ->
                listOf(OffHeaderOp("Thread rolling", "Roll the trilobular thread-forming thread $threadLabel with its tapered lead-in"))
            FastenerType.EYE_BOLT -> listOf(OffHeaderOp("Eye piercing", "Pierce and size the eye bore"), point, roll)
            FastenerType.T_BOLT -> listOf(OffHeaderOp("Head trimming", "Trim two sides of the head to the T shape"), point, roll)
            FastenerType.STUD -> listOf(OffHeaderOp("Pointing", "Chamfer both ends to minor diameter (45°)"), OffHeaderOp("Thread rolling", "Roll $threadLabel on both ends"))
            FastenerType.WHEEL_STUD -> listOf(OffHeaderOp("Knurling", "Roll the press-fit serration on the collar"), point, roll)
            FastenerType.U_BOLT -> listOf(
                OffHeaderOp("Pointing", "Chamfer both ends to minor diameter (45°)"), OffHeaderOp("Thread rolling", "Roll $threadLabel on both ends"),
                OffHeaderOp("Bending", "Bend to the U form")
            )
            FastenerType.NYLOC_NUT -> listOf(tap, OffHeaderOp("Insert assembly", "Press the polyamide ring into the collar and crimp it"))
            FastenerType.METAL_LOCK_NUT, FastenerType.AERO_LOCK_NUT -> listOf(tap, deflect)
            FastenerType.CASTLE_NUT -> listOf(OffHeaderOp("Slot milling", "Mill the six slots in the crown"), tap)
            FastenerType.ACORN_NUT -> listOf(OffHeaderOp("Tapping", "Tap $threadLabel blind with a bottoming tap"))
            FastenerType.HI_LOK_COLLAR -> listOf(tap, OffHeaderOp("Torque-off groove", "Machine the break groove under the wrenching hex"))
            FastenerType.NUTPLATE -> listOf(tap, OffHeaderOp("Plate assembly", "Stamp the lug plate and swage the nut element into it"))
            else -> if (type.isNut) listOf(tap) else listOf(point, roll)
        }
    }

    /** Hex head forged round (1.2 × e), then trimmed: the slug carries the cheese, the trim station removes it. */
    private fun trimmedHex(
        type: FastenerType, length: Double, fg: BoltForgingResult, dRound: Double, s: Double, k: Double, threadLabel: String,
        flangeDc: Double? = null, flangeC: Double? = null, cautions: List<String> = emptyList()
    ): HeadingPlan {
        val isFlange = flangeDc != null && flangeC != null
        val wire = fg.shankDiaMm
        val aw = area(wire)
        val cheese = fg.cheeseVolumeMm3!!
        val headForged = fg.headStockMm!! * aw
        val slugVol = fg.finishedVolumeMm3!! + cheese
        val slugLen = slugVol / aw
        val stock = fg.headStockMm
        val ld = stock / wire
        val blows = when {
            ld <= SINGLE_BLOW_LD -> 1
            ld <= TWO_BLOW_LD -> 2
            else -> 3
        }
        val lt = fg.threadLengthMm
        val shank = buildList {
            if (lt > 0.0) add(ProfileSegment(lt, fg.blankDiaMm))
            if (fg.coneLengthMm > 0.0) add(ProfileSegment(fg.coneLengthMm, fg.blankDiaMm, wire))
            val body = length - lt - fg.coneLengthMm
            if (body > 0.0) add(ProfileSegment(body, wire))
        }
        val reductionPct = (1.0 - area(fg.blankDiaMm) / aw) * 100.0
        val stations = mutableListOf(cutOff(wire, slugLen))
        stations += HeadingStation(
            index = 1,
            title = "Square & forward extrude",
            operation = "Square sheared ends; extrude thread section Ø${f(wire)} → Ø${f(fg.blankDiaMm, 3)} × ${f(lt, 1)} mm through 15°/side cone " +
                "(${f(fg.coneLengthMm)} mm, ${f(reductionPct, 0)} % area reduction); stock before extrusion ${f(fg.stockToExtrudeMm)} mm; head stock ${f(stock, 1)} mm, L/D ${f(ld)}",
            segments = shank + ProfileSegment(stock, wire),
            inDieMm = length
        )
        val coneFractions = when (blows) {
            1 -> emptyList()
            2 -> listOf(0.62)
            else -> listOf(0.75, 0.52)
        }
        coneFractions.forEachIndexed { i, fraction ->
            var hc = stock * fraction
            var dc = coneLargeEnd(headForged, wire, hc)
            if (dc > 0.9 * dRound) {
                dc = 0.9 * dRound
                hc = 12.0 * headForged / (PI * (dc * dc + dc * wire + wire * wire))
            }
            stations += HeadingStation(
                index = stations.size,
                title = if (coneFractions.size > 1) "Cone pre-upset ${i + 1}" else "Cone pre-upset",
                operation = "Gather head stock in cone punch: Ø${f(dc)} → Ø${f(wire)} × ${f(hc, 1)} mm (L/D ${f(ld)} > $SINGLE_BLOW_LD)",
                segments = shank + ProfileSegment(hc, dc, wire),
                inDieMm = length
            )
        }
        if (isFlange) {
            val c = flangeC!!
            val dc = flangeDc!!
            val hexPart = ProfileSegment(k - c, s, shape = SectionShape.HEX)
            // The preform has to drop inside the flange cavity of the finish die
            val dPre = 0.95 * dc
            stations += HeadingStation(
                index = stations.size,
                title = "Head upset",
                operation = "Upset round head Ø${f(dPre)} × ${f(headForged / area(dPre))} mm against die face, to enter the flange cavity",
                segments = shank + ProfileSegment(headForged / area(dPre), dPre),
                inDieMm = length
            )
            stations += HeadingStation(
                index = stations.size,
                title = "Hex & flange form",
                operation = "Coin flange Ø${f(dc)} × c=${f(c)} mm in the die cavity and close hex s=${f(s)} × ${f(k - c)} mm " +
                    "in the punch hex insert; formed to size, no trim",
                segments = shank + ProfileSegment(c, dc) + hexPart,
                inDieMm = length,
                isPeakBlow = true
            )
        } else {
            stations += HeadingStation(
                index = stations.size,
                title = "Head upset (round, for trimming)",
                operation = "Upset round head Ø${f(dRound)} (1.20 × across flats) × ${f(headForged / area(dRound))} mm against die face",
                segments = shank + ProfileSegment(headForged / area(dRound), dRound),
                inDieMm = length,
                isPeakBlow = true
            )
            stations += HeadingStation(
                index = stations.size,
                title = "Trim to hex",
                operation = "Trim round head to hex s=${f(s)} × k=${f(k)} mm; trim cheese ${f(cheese, 0)} mm³ removed",
                segments = shank + ProfileSegment(k, s, shape = SectionShape.HEX),
                inDieMm = length,
                scrapMm3 = cheese
            )
        }
        val notes = buildList {
            if (ld > THREE_BLOW_LD) add("Head stock L/D ${f(ld)} exceeds the 3-blow limit ($THREE_BLOW_LD): use larger wire or warm forming.")
            if (reductionPct > 30.0) add("Open-die extrusion ${f(reductionPct, 0)} % exceeds ~30 %: extrude in a closed (trapped) die to avoid upsetting the shank.")
        }
        return HeadingPlan(wire, slugLen, slugVol, stations, offHeaderOps(type, threadLabel), blows, notes + cautions)
    }

    /** Blows the free head stock needs: 1 up to L/D 2.3, 2 up to 4.5, else 3. */
    private fun blowsFor(ld: Double) = when {
        ld <= SINGLE_BLOW_LD -> 1
        ld <= TWO_BLOW_LD -> 2
        else -> 3
    }

    /** Title and operation of the blow that finishes the head, by fastener type. */
    private fun finishingBlow(type: FastenerType, part: FinishedPart): Pair<String, String> {
        val head = part.headSegments
        val top = head.last()
        val size = f(if (top.shape == SectionShape.ROUND) max(top.dBottomMm, top.dTopMm) else top.dBottomMm)
        val k = f(top.heightMm)
        val socket = part.recesses.firstOrNull { it.fromTop }?.let { "hex socket s=${f(it.sizeMm, 1)} × t=${f(it.depthMm, 1)} mm" }
        val flange = head.firstOrNull { it !== top && it.shape == SectionShape.ROUND }
            ?.let { ", flange Ø${f(it.dBottomMm)} × c=${f(it.heightMm)} mm on the die face" } ?: ""
        val hex = "Close hex s=$size × k=$k mm in punch hex insert, 30° chamfer, bearing face"
        return when (part.style) {
            HeadStyle.SOCKET_CAP -> {
                val rec = part.recesses.firstOrNull { it.fromTop }
                if (top.shape == SectionShape.DOME) {
                    val edge = head.getOrNull(head.size - 2)?.takeIf { it.shape == SectionShape.ROUND }
                    val kAll = f(head.sumOf { it.heightMm })
                    val crown = "pan head Ø$size × k=$kAll mm (edge ${f(edge?.heightMm ?: 0.0)} mm + crown ${f(top.heightMm)} mm) in the crowned punch"
                    when {
                        rec == null -> "Pan head form" to "Form $crown, under-head fillet in the die"
                        rec.shape == SectionShape.HEXALOBULAR -> "Pan head & hexalobular recess" to
                            "Form $crown; back-extrude hexalobular recess A=${f(rec.sizeMm)} × t=${f(rec.depthMm)} mm with the recess pin"
                        else -> "Pan head & recess" to "Form $crown; back-extrude recess ${f(rec.sizeMm, 1)} × t=${f(rec.depthMm, 1)} mm"
                    }
                } else if (rec == null) {
                    // Pan, cheese, shoulder and clevis heads: a plain cylindrical head. The slot or cross drive is
                    // not in the modelled volume, so the blow closes the head only.
                    "Cylindrical head form" to "Close cylindrical head Ø$size × k=$k mm in the punch cavity, bearing face and edge radius"
                } else {
                    "Socket back-extrude" to
                        ((if (type == FastenerType.HEXALOBULAR_SCREW) "Back-extrude hexalobular recess (volume modelled as hex s=${f(rec.sizeMm, 1)})"
                        else "Back-extrude hex socket s=${f(rec.sizeMm, 1)}") +
                            " × t=${f(rec.depthMm, 1)} mm; finish head Ø$size × k=$k mm")
                }
            }
            HeadStyle.SOCKET_COUNTERSUNK -> {
                val angle = 2.0 * Math.toDegrees(kotlin.math.atan((top.dTopMm - top.dBottomMm) / 2.0 / top.heightMm))
                "Countersunk head & socket" to "Form countersunk head Ø$size × k=$k mm (${f(angle, 0)}° included) in the die countersink; back-extrude $socket"
            }
            HeadStyle.SOCKET_BUTTON -> "Button head & socket" to "Form dome head Ø$size × k=$k mm in the cupped punch; back-extrude $socket"
            HeadStyle.TWELVE_POINT -> "12-point form" to "Close 12-point (bi-hex) s=$size × k=$k mm in the punch insert$flange"
            HeadStyle.CARRIAGE -> "Mushroom head & square neck" to ("Form dome head Ø$size × k=$k mm in the cupped punch" +
                (head.firstOrNull { it.shape == SectionShape.SQUARE }
                    ?.let { "; square neck ${f(it.dBottomMm)} × ${f(it.dBottomMm)} × ${f(it.heightMm)} mm fills the square die recess" } ?: ""))
            HeadStyle.RIVET -> "Head upset (universal head)" to "Form dome head Ø$size × k=$k mm in the cupped punch in one blow"
            HeadStyle.ROUND_PIN -> when (type) {
                FastenerType.HI_LOK_PIN -> "Head upset (Hi-Lok head)"
                FastenerType.TAPER_LOK -> "Head upset (Taper-Lok head)"
                FastenerType.LOCKBOLT_PIN -> "Head upset (lockbolt head)"
                else -> "Head upset (core bolt head)"
            } to "Upset round head Ø$size × k=$k mm against die face"
            else -> when (type) {
                FastenerType.T_BOLT -> "T-head form" to "Flatten the upset into the T-head blank (counted as hex s=$size × k=$k mm); two sides are trimmed off the header"
                FastenerType.EYE_BOLT -> "Eye blank form" to "Flatten the upset into the eye blank (counted as hex s=$size × k=$k mm); the eye is pierced off the header"
                FastenerType.WHEEL_BOLT -> "Hex & seat form" to "$hex; form the conical / spherical seat in the die"
                FastenerType.TTY_HEAD_BOLT -> "Hex flange head form" to "$hex$flange"
                FastenerType.THREAD_FORMING_SCREW -> "Hex washer head form" to "$hex$flange"
                else -> (if (flange.isNotEmpty()) "Hex & flange form" else "Hex form") to "$hex$flange"
            }
        }
    }

    private fun headed(type: FastenerType, wire: Double, part: FinishedPart, threadLabel: String): HeadingPlan {
        val aw = area(wire)
        val slugVol = part.volumeMm3
        val slugLen = slugVol / aw
        val body = part.dieSegments
        val inDie = body.sumOf { it.heightMm }
        val headVol = part.headVolumeMm3
        val stock = headVol / aw
        val ld = stock / wire
        val blows = blowsFor(ld)
        val lt = part.threadLengthMm
        val blank = part.blankDiaMm
        val reductionPct = if (lt > 0.0) (1.0 - area(blank) / aw) * 100.0 else 0.0
        val top = part.headSegments.last()
        val headSize = if (top.shape == SectionShape.ROUND) max(top.dBottomMm, top.dTopMm) else top.dBottomMm
        val hasSocket = part.recesses.any { it.fromTop }
        // A rivet or pin head is round: the upset IS the finished head. Other heads need a round preform that fits the tooling.
        val upsetIsFinal = part.style == HeadStyle.RIVET || part.style == HeadStyle.ROUND_PIN
        val dRound = when (part.style) {
            HeadStyle.SOCKET_CAP -> 0.97 * headSize
            HeadStyle.SOCKET_COUNTERSUNK, HeadStyle.SOCKET_BUTTON, HeadStyle.CARRIAGE -> 0.90 * headSize
            HeadStyle.RIVET, HeadStyle.ROUND_PIN -> headSize
            // The round preform must enter the hex / 12-point punch insert (inscribed circle s)
            else -> 0.96 * headSize
        }

        val stations = mutableListOf(cutOff(wire, slugLen))
        // Full-thread parts are headed from wire drawn to the rolling blank: nothing to extrude
        val extrude = lt > 0.0 && reductionPct >= 1.0
        stations += HeadingStation(
            index = 1,
            title = if (extrude) "Square & forward extrude" else "Square ends",
            operation = if (extrude) "Square sheared ends; extrude thread section Ø${f(wire)} → Ø${f(blank, 3)} × ${f(lt, 1)} mm " +
                "(${f(reductionPct, 0)} % area reduction); head stock ${f(stock, 1)} mm, L/D ${f(ld)}"
            else if (lt > 0.0) "Square sheared ends; wire is at the rolling blank Ø${f(blank, 3)}, so the shank is not extruded; head stock ${f(stock, 1)} mm, L/D ${f(ld)}"
            else "Square sheared ends; head stock ${f(stock, 1)} mm, L/D ${f(ld)}",
            segments = body + ProfileSegment(stock, wire),
            inDieMm = inDie
        )

        val coneFractions = when (blows) {
            1 -> emptyList()
            2 -> listOf(0.62)
            else -> listOf(0.75, 0.52)
        }
        coneFractions.forEachIndexed { i, fraction ->
            // Cone punch: small end at the punch, large end on the die face; volume fixes the large end
            var hc = stock * fraction
            var dc = coneLargeEnd(headVol, wire, hc)
            if (dc > 0.9 * dRound) {
                dc = 0.9 * dRound
                hc = 12.0 * headVol / (PI * (dc * dc + dc * wire + wire * wire))
            }
            stations += HeadingStation(
                index = stations.size,
                title = if (coneFractions.size > 1) "Cone pre-upset ${i + 1}" else "Cone pre-upset",
                operation = "Gather head stock in cone punch: Ø${f(dc)} → Ø${f(wire)} × ${f(hc, 1)} mm (L/D ${f(ld)} > $SINGLE_BLOW_LD)",
                segments = body + ProfileSegment(hc, dc, wire),
                inDieMm = inDie
            )
        }

        if (!upsetIsFinal) {
            val hRound = headVol / area(dRound)
            stations += HeadingStation(
                index = stations.size,
                title = "Head upset",
                operation = "Upset round head Ø${f(dRound)} × ${f(hRound)} mm against die face",
                segments = body + ProfileSegment(hRound, dRound),
                inDieMm = inDie,
                isPeakBlow = hasSocket
            )
        }
        val (title, operation) = finishingBlow(type, part)
        stations += HeadingStation(
            index = stations.size,
            title = title,
            operation = operation,
            segments = part.segments,
            recesses = part.recesses,
            inDieMm = inDie,
            isPeakBlow = !hasSocket
        )

        val notes = buildList {
            if (ld > THREE_BLOW_LD) add("Head stock L/D ${f(ld)} exceeds the 3-blow limit ($THREE_BLOW_LD): use larger wire or warm forming.")
            if (reductionPct > 30.0) add("Open-die extrusion ${f(reductionPct, 0)} % exceeds ~30 %: extrude in a closed (trapped) die to avoid upsetting the shank.")
            addAll(part.cautions)
        }
        return HeadingPlan(wire, slugLen, slugVol, stations, offHeaderOps(type, threadLabel), blows, notes)
    }

    private fun headless(type: FastenerType, wire: Double, part: FinishedPart, threadLabel: String): HeadingPlan {
        val slugVol = part.volumeMm3
        val slugLen = slugVol / area(wire)
        val blank = part.blankDiaMm
        val stations = mutableListOf(cutOff(wire, slugLen))
        val socket = part.recesses.firstOrNull()
        if (socket != null) {
            val solidLen = slugVol / area(blank)
            stations += HeadingStation(
                index = 1,
                title = "Square & extrude",
                operation = "Square ends; extrude full length Ø${f(wire)} → Ø${f(blank, 3)} mm",
                segments = listOf(ProfileSegment(solidLen, blank)),
                inDieMm = solidLen
            )
            stations += HeadingStation(
                index = 2,
                title = "Socket back-extrude",
                operation = "Back-extrude hex socket s=${f(socket.sizeMm, 1)} × t=${f(socket.depthMm, 1)} mm; part grows to L=${f(part.heightMm, 1)} mm",
                segments = part.segments,
                recesses = part.recesses,
                inDieMm = part.heightMm,
                isPeakBlow = true
            )
        } else {
            val threaded = part.threadLengthMm > 0.0
            stations += HeadingStation(
                index = 1,
                title = if (threaded) "Square & extrude ends" else "Square ends",
                operation = if (threaded) "Square ends; extrude thread sections Ø${f(wire)} → Ø${f(blank, 3)} mm" else "Square both sheared ends",
                segments = part.segments,
                inDieMm = part.heightMm,
                isPeakBlow = true
            )
        }
        return HeadingPlan(wire, slugLen, slugVol, stations, offHeaderOps(type, threadLabel), 0, part.cautions)
    }

    private fun nut(type: FastenerType, wire: Double, part: FinishedPart, threadLabel: String): HeadingPlan {
        val hexSeg = part.segments.last()
        val s = hexSeg.dBottomMm
        val m = part.heightMm
        val hole = part.throughHoleMm!!
        val nutVol = part.volumeMm3
        // Web left between the two indents and punched out at the last station
        val web = max(0.8, 0.25 * hole).coerceAtMost(0.4 * m)
        val webVol = area(hole) * web
        val slugVol = nutVol + webVol
        val slugLen = slugVol / area(wire)
        val indentFinal = (m - web) / 2.0
        val dRound = 0.96 * s
        val d1 = if (dRound > wire) wire + 0.35 * (dRound - wire) else wire
        val h1 = slugVol / area(d1)
        val indent2 = 0.4 * indentFinal
        val h2 = (slugVol + 2.0 * area(hole) * indent2) / area(dRound)
        val flange = part.segments.firstOrNull { it !== hexSeg && it.shape == SectionShape.ROUND }
            ?.let { ", flange Ø${f(it.dBottomMm)} × c=${f(it.heightMm)} mm" } ?: ""
        // The same forming blow, named for what this nut type adds to the plain hex
        val formTitle = when (type) {
            FastenerType.FLANGE_NUT -> "Hex & flange form"
            FastenerType.NYLOC_NUT -> "Hex & collar form"
            FastenerType.CASTLE_NUT -> "Hex & crown form"
            FastenerType.ACORN_NUT -> "Hex & dome form"
            FastenerType.WELD_NUT -> "Hex & weld projection form"
            FastenerType.WHEEL_NUT -> "Hex & seat form"
            FastenerType.AERO_LOCK_NUT -> "Reduced hex form"
            FastenerType.HI_LOK_COLLAR -> "Collar form"
            FastenerType.NUTPLATE -> "Nut element form"
            else -> "Hex form"
        }
        val formExtra = when (type) {
            FastenerType.NYLOC_NUT -> "; raise the collar that holds the insert"
            FastenerType.CASTLE_NUT -> "; raise the round crown for the slots"
            FastenerType.ACORN_NUT -> "; raise the dome cap"
            FastenerType.WELD_NUT -> "; coin the weld projections"
            FastenerType.WHEEL_NUT -> "; form the conical / spherical seat"
            FastenerType.HI_LOK_COLLAR -> "; form the wrenching hex above the collar"
            else -> ""
        }

        val stations = listOf(
            cutOff(wire, slugLen),
            HeadingStation(
                index = 1,
                title = "Square & pre-upset",
                operation = "Flatten sheared ends, upset to Ø${f(d1)} × ${f(h1)} mm",
                segments = listOf(ProfileSegment(h1, d1))
            ),
            HeadingStation(
                index = 2,
                title = "Pre-form & indent",
                operation = "Upset round Ø${f(dRound)} × ${f(h2)} mm, start indents Ø${f(hole)} × ${f(indent2)} mm both faces",
                segments = listOf(ProfileSegment(h2, dRound)),
                recesses = listOf(Recess(true, hole, indent2, SectionShape.ROUND), Recess(false, hole, indent2, SectionShape.ROUND))
            ),
            HeadingStation(
                index = 3,
                title = formTitle,
                operation = "Close hex s=${f(s)} × m=${f(m)} mm$flange, deepen indents to ${f(indentFinal)} mm, leave web ${f(web)} mm$formExtra",
                segments = part.segments,
                recesses = listOf(Recess(true, hole, indentFinal, SectionShape.ROUND), Recess(false, hole, indentFinal, SectionShape.ROUND)),
                isPeakBlow = true
            ),
            HeadingStation(
                index = 4,
                title = "Pierce",
                operation = "Punch out web Ø${f(hole)} × ${f(web)} mm (${f(webVol / 1000.0, 3)} cm³ scrap); bore = tap drill",
                segments = part.segments,
                throughHoleMm = hole,
                scrapMm3 = webVol
            )
        )
        return HeadingPlan(
            wire, slugLen, slugVol, stations, offHeaderOps(type, threadLabel), 0, part.cautions,
            route = HeaderRoute.NUT_FORMER,
            routeReason = "Nut former: the blank is upset, indented, hex-formed and pierced from wire in line."
        )
    }

    /** Large-end diameter D of a frustum of height h with small end d holding volume v. */
    private fun coneLargeEnd(v: Double, d: Double, h: Double): Double {
        val c = 12.0 * v / (PI * h)
        val disc = (4.0 * c - 3.0 * d * d).coerceAtLeast(0.0)
        return (-d + sqrt(disc)) / 2.0
    }
}
