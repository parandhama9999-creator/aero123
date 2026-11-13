package com.example.data

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

/** Dimensions read from a drawing, in mm. null = not on the drawing: the standard value is used and reported. */
data class DrawingGeometry(
    val threadLengthMm: Double? = null,
    val shankDiaMm: Double? = null,
    /** Across flats of a hex or 12-point head, or of a nut. */
    val acrossFlatsMm: Double? = null,
    /** Diameter of a round head: socket cap, countersunk, button, carriage, pin or rivet. */
    val headDiaMm: Double? = null,
    val headHeightMm: Double? = null,
    val flangeDiaMm: Double? = null,
    val flangeThicknessMm: Double? = null,
    val socketSizeMm: Double? = null,
    val socketDepthMm: Double? = null,
    /** Square neck height of a carriage bolt. */
    val neckHeightMm: Double? = null,
    val nutHeightMm: Double? = null,
    val washerOuterDiaMm: Double? = null,
    val washerInnerDiaMm: Double? = null,
    val washerThicknessMm: Double? = null
)

/** Where a volume figure's dimensions came from, weakest source first when several are mixed. */
enum class DimSource(val label: String) {
    DRAWING("drawing"), CALCULATED("calculated"), STANDARD("standard"), ESTIMATED("estimated")
}

/** One feature of the finished part. A negative volume is material removed (socket, bore). */
data class VolumeItem(val feature: String, val detail: String, val volumeMm3: Double, val source: DimSource)

/** How the head (or the whole part) is formed, which decides both its shape and its die progression. */
enum class HeadStyle {
    HEX_TRIMMED, HEX_FORMED, SOCKET_CAP, SOCKET_COUNTERSUNK, SOCKET_BUTTON, TWELVE_POINT, CARRIAGE, ROUND_PIN, RIVET,
    HEADLESS, NUT, WASHER
}

/**
 * The finished part as a stack of solids from the die end (bottom) to the punch end (top), with its cavities.
 * Its volume is the part volume: weights, the cut-off slug and the last die station all come from this one shape.
 */
data class FinishedPart(
    val style: HeadStyle,
    val segments: List<ProfileSegment>,
    val recesses: List<Recess>,
    val throughHoleMm: Double?,
    /** Top segments formed outside the die bore: neck, flange, head (all of a nut or washer). */
    val headSegmentCount: Int,
    val threadLengthMm: Double,
    val blankDiaMm: Double,
    /** Labelled breakdown; the volumes add up to [volumeMm3]. */
    val items: List<VolumeItem>,
    val fromDrawing: List<String>,
    /** Dimensions the drawing did not give, with the value used instead and where it came from. */
    val notOnDrawing: List<String>,
    /** Features of this fastener type that the shape does not include. */
    val cautions: List<String>
) {
    val heightMm: Double get() = segments.sumOf { it.heightMm }
    val volumeMm3: Double
        get() = segments.sumOf { it.volumeMm3 } - recesses.sumOf { it.volumeMm3 } -
            (throughHoleMm?.let { PI / 4.0 * it * it * heightMm } ?: 0.0)
    val dieSegments: List<ProfileSegment> get() = segments.dropLast(headSegmentCount)
    val headSegments: List<ProfileSegment> get() = segments.takeLast(headSegmentCount)
    /** Material formed outside the die bore, net of the socket it displaces. */
    val headVolumeMm3: Double get() = volumeMm3 - dieSegments.sumOf { it.volumeMm3 }
    /** True when every dimension in the volume is from the drawing and the type's shape is fully modelled. */
    val isComplete: Boolean get() = notOnDrawing.isEmpty() && cautions.isEmpty()

    /** The finished shape as a drawable station (no die). */
    fun asStation(): HeadingStation = HeadingStation(
        index = 1, title = "Finished part", operation = "", segments = segments, recesses = recesses, throughHoleMm = throughHoleMm
    )
}

object PartGeometry {

    private val ROUND_PIN_TYPES = setOf(
        FastenerType.HI_LOK_PIN, FastenerType.TAPER_LOK, FastenerType.LOCKBOLT_PIN, FastenerType.BLIND_BOLT,
        FastenerType.CLEVIS_PIN
    )

    /** Headless parts: set screws, studs, rod, and the screws whose "head" is a bend or a thread. */
    private val HEADLESS_TYPES = setOf(
        FastenerType.SOCKET_SET, FastenerType.SET_SCREW_CONE, FastenerType.SET_SCREW_DOG, FastenerType.SET_SCREW_CUP,
        FastenerType.SLOTTED_SET_SCREW, FastenerType.HANGER_BOLT, FastenerType.ANCHOR_BOLT,
        // Plain pins have no head at all: they are a parted-off cylinder, not an upset part
        FastenerType.DOWEL_PIN, FastenerType.TAPER_PIN, FastenerType.SPRING_PIN, FastenerType.GROOVED_PIN,
        FastenerType.SPLIT_PIN
    )

    /** Cylindrical-head screws: pan, cheese, shoulder, knurled and the aircraft clevis bolt. */
    private val CYLINDRICAL_HEAD_TYPES = setOf(
        FastenerType.PAN_SLOT, FastenerType.PAN_CROSS, FastenerType.CHEESE_SLOT, FastenerType.SHOULDER_BOLT,
        FastenerType.THUMB_SCREW, FastenerType.CLEVIS_BOLT
    )

    /** Countersunk screws: the head is a cone sunk into the joint. */
    private val COUNTERSUNK_TYPES = setOf(
        FastenerType.SOCKET_CSK, FastenerType.CSK_SLOT, FastenerType.CSK_CROSS, FastenerType.RAISED_CSK_CROSS,
        FastenerType.TAPPING_CSK, FastenerType.DRYWALL_SCREW, FastenerType.CHIPBOARD_SCREW
    )
    private val SOCKET_STYLES = setOf(HeadStyle.SOCKET_CAP, HeadStyle.SOCKET_COUNTERSUNK, HeadStyle.SOCKET_BUTTON)
    private val ROUND_HEAD_STYLES = SOCKET_STYLES + setOf(HeadStyle.CARRIAGE, HeadStyle.ROUND_PIN, HeadStyle.RIVET)

    fun styleOf(type: FastenerType): HeadStyle = when {
        type.isWasher -> HeadStyle.WASHER
        type.isNut -> HeadStyle.NUT
        type.isStud || type in HEADLESS_TYPES -> HeadStyle.HEADLESS
        BoltForging.applies(type) -> HeadStyle.HEX_TRIMMED
        type == FastenerType.SHCS || type == FastenerType.HEXALOBULAR_SCREW -> HeadStyle.SOCKET_CAP
        type in COUNTERSUNK_TYPES -> HeadStyle.SOCKET_COUNTERSUNK
        type in CYLINDRICAL_HEAD_TYPES -> HeadStyle.SOCKET_CAP
        type == FastenerType.SOCKET_BUTTON || type == FastenerType.TAPPING_PAN || type == FastenerType.SELF_DRILLING ->
            HeadStyle.SOCKET_BUTTON
        type == FastenerType.TWELVE_POINT -> HeadStyle.TWELVE_POINT
        type == FastenerType.CARRIAGE_BOLT -> HeadStyle.CARRIAGE
        type == FastenerType.RETAINING_RING -> HeadStyle.WASHER
        type in ROUND_PIN_TYPES -> HeadStyle.ROUND_PIN
        type.isUnthreaded -> HeadStyle.RIVET
        else -> HeadStyle.HEX_FORMED
    }

    /** Features of a fastener type that its modelled shape leaves out. */
    private fun shapeCaution(type: FastenerType): String? = when (type) {
        FastenerType.T_BOLT -> "T-head is counted as a hex prism s × k: take the head volume from the drawing."
        FastenerType.EYE_BOLT -> "The eye is counted as a hex prism s × k: the eye and its bore are not modelled."
        FastenerType.WHEEL_BOLT, FastenerType.WHEEL_NUT -> "The conical / spherical seat is not in the volume."
        FastenerType.BANJO_BOLT -> "The axial bore and cross hole are not in the volume (drilled after heading)."
        FastenerType.TTY_HEAD_BOLT -> "The reduced (waisted) shank is not modelled: the shank is taken at one diameter."
        FastenerType.HI_LOK_PIN, FastenerType.TAPER_LOK, FastenerType.LOCKBOLT_PIN, FastenerType.BLIND_BOLT ->
            "Head counted as a plain cylinder; lock grooves, pintail, taper and sleeve are not in the volume."
        FastenerType.HEXALOBULAR_SCREW -> "The hexalobular recess is counted as a hex socket of the same key size."
        FastenerType.CASTLE_NUT -> "The crown slots are not in the volume (milled after forming)."
        FastenerType.ACORN_NUT -> "The dome cap and blind bore are not modelled: counted as a through-bored hex."
        FastenerType.NYLOC_NUT -> "The insert collar and nylon ring are not in the volume."
        FastenerType.WELD_NUT -> "The weld projections are not in the volume."
        FastenerType.NUTPLATE -> "The lug plate is not in the volume: nut element only."
        FastenerType.HI_LOK_COLLAR -> "The torque-off wrenching element is not modelled."
        FastenerType.AERO_LOCK_NUT, FastenerType.METAL_LOCK_NUT -> "The locking deflection is not modelled: counted as a plain hex nut."
        FastenerType.SPRING_WASHER -> "Counted as a flat ring: the split and the helix are not in the volume."
        FastenerType.U_BOLT -> "The length is the developed (straight) length before bending."
        FastenerType.WHEEL_STUD -> "The head and the knurl are not modelled: counted as a plain stud."
        FastenerType.SET_SCREW_CONE, FastenerType.SET_SCREW_DOG, FastenerType.SET_SCREW_CUP, FastenerType.SLOTTED_SET_SCREW ->
            "The point (cone / dog / cup) and the drive recess are not modelled: counted as a plain threaded cylinder."
        FastenerType.SHOULDER_BOLT -> "The ground shoulder is taken at the thread diameter: give the shoulder Ø and length on the drawing."
        FastenerType.SQUARE_BOLT -> "The square head is counted as a hex prism of the same across flats."
        FastenerType.HANGER_BOLT -> "Counted as a plain stud: the wood thread (coarse, different pitch) is not modelled."
        FastenerType.ANCHOR_BOLT -> "The L or J bend is not modelled: the length is the developed (straight) length."
        FastenerType.LAG_SCREW, FastenerType.CHIPBOARD_SCREW, FastenerType.DRYWALL_SCREW, FastenerType.SELF_DRILLING,
        FastenerType.TAPPING_PAN, FastenerType.TAPPING_CSK, FastenerType.TAPPING_HEX ->
            "The tapping / wood thread form and the point (or drill tip) are not modelled: the shank is taken at the rolling blank Ø."
        FastenerType.SEMS_SCREW -> "The captive washer is not in the volume: screw only."
        FastenerType.PAN_SLOT, FastenerType.PAN_CROSS, FastenerType.CHEESE_SLOT, FastenerType.CSK_SLOT,
        FastenerType.CSK_CROSS, FastenerType.RAISED_CSK_CROSS, FastenerType.HEX_WASHER_SCREW ->
            "The slot or cross recess is not in the volume: the head is counted as solid."
        FastenerType.CONROD_BOLT -> "The necked-down (waisted) shank is not modelled: the shank is taken at one diameter."
        FastenerType.SHEAR_BOLT -> "The reduced head height of a shear bolt is taken from the drawing; no shear-bolt table in the app."
        FastenerType.TAPER_PIN -> "Counted as a plain cylinder at the nominal Ø: the 1:50 taper is not in the volume."
        FastenerType.SPRING_PIN -> "Counted as a solid cylinder: the slot and the rolled wall thickness are not modelled."
        FastenerType.GROOVED_PIN -> "The three grooves are not in the volume: counted as a plain cylinder."
        FastenerType.SPLIT_PIN -> "Counted as a solid cylinder: the split, the eye and the semicircular section are not modelled."
        FastenerType.RETAINING_RING -> "Counted as a flat ring: the gap, lugs and the tapered section are not modelled."
        FastenerType.THREADED_ROD, FastenerType.TAP_END_STUD -> "Counted as a plain threaded rod over the whole length."
        FastenerType.SQUARE_NUT, FastenerType.T_SLOT_NUT -> "The square / T profile is counted as a hex prism of the same width."
        FastenerType.COUPLING_NUT -> "Counted as a long hex nut: the full-length thread is bored through."
        FastenerType.CAGE_NUT, FastenerType.CLINCH_NUT, FastenerType.RIVET_NUT ->
            "Only the nut body is in the volume: the cage, the clinch ring or the deforming sleeve is not modelled."
        FastenerType.WING_NUT, FastenerType.WING_SCREW, FastenerType.THUMB_NUT, FastenerType.THUMB_SCREW ->
            "The wings / knurl are counted as a prism of the same envelope: the real profile is not modelled."
        FastenerType.THIN_NUT, FastenerType.HIGH_NUT -> "Nut height m is taken from the type's own standard, not ISO 4032."
        FastenerType.TOOTH_LOCK_WASHER -> "Counted as a flat ring: the teeth are not in the volume."
        FastenerType.BELLEVILLE_WASHER, FastenerType.WAVE_WASHER -> "Counted as a flat ring at the material thickness: the cone / wave is not modelled."
        FastenerType.TAPER_WASHER -> "Counted as a flat ring at the mean thickness: the 8° / 14° taper is not modelled."
        FastenerType.BLIND_RIVET -> "Blind rivet: only the sleeve is modelled, as a solid shank. The sleeve bore and the mandrel are separate parts not covered here."
        else -> null
    }

    private fun n(v: Double, dp: Int = 2) = String.format(java.util.Locale.US, "%.${dp}f", v)
    private fun worst(vararg s: DimSource) = s.maxByOrNull { it.ordinal }!!

    fun build(
        type: FastenerType, unit: UnitSystem, dMm: Double, lengthMm: Double, threadLengthMm: Double,
        blankDiaMm: Double, table: HeadDimensions, tableSocket: Pair<Double, Double>?, g: DrawingGeometry,
        forging: BoltForgingResult? = null, flange: BoltForging.FlangeGeom? = null
    ): FinishedPart {
        val style = styleOf(type).let { if (it == HeadStyle.HEX_TRIMMED && forging == null) HeadStyle.HEX_FORMED else it }
        val segs = mutableListOf<ProfileSegment>()
        val items = mutableListOf<VolumeItem>()
        val recs = mutableListOf<Recess>()
        val fromDrawing = mutableListOf<String>()
        val notOnDrawing = mutableListOf<String>()
        val cautions = mutableListOf<String>()
        var hole: Double? = null
        var headCount = 0
        var lt = 0.0

        /** The drawing's value when it has one, else the fallback; either way it is recorded. */
        fun pick(name: String, drawn: Double?, fallback: Double, from: String = table.source, fromTable: Boolean = table.isTable): Pair<Double, DimSource> =
            if (drawn != null && drawn > 0.0) {
                fromDrawing += "$name ${n(drawn)} mm"
                drawn to DimSource.DRAWING
            } else {
                notOnDrawing += "$name ${n(fallback)} mm — $from"
                fallback to (if (fromTable) DimSource.STANDARD else DimSource.ESTIMATED)
            }
        fun seg(s: ProfileSegment, feature: String, detail: String, source: DimSource, head: Boolean = false) {
            if (s.heightMm <= 1e-9) return
            segs += s
            items += VolumeItem(feature, detail, s.volumeMm3, source)
            if (head) headCount++
        }
        fun recess(rc: Recess, feature: String, detail: String, source: DimSource) {
            recs += rc
            items += VolumeItem(feature, detail, -rc.volumeMm3, source)
        }
        fun bore(dia: Double, feature: String, detail: String, source: DimSource) {
            hole = dia
            items += VolumeItem(feature, detail, -PI / 4.0 * dia * dia * segs.sumOf { it.heightMm }, source)
        }
        val threadSrc = if (g.threadLengthMm != null) DimSource.DRAWING else DimSource.STANDARD
        fun noteThread(len: Double) {
            if (g.threadLengthMm != null) fromDrawing += "thread length b ${n(len)} mm"
            else notOnDrawing += "thread length b ${n(len)} mm — standard thread length rule"
        }
        fun threadSeg(len: Double, feature: String = "Thread section") =
            seg(ProfileSegment(len, blankDiaMm), feature, "rolling blank Ø${n(blankDiaMm, 3)} × ${n(len)} mm", threadSrc)

        when (style) {
            HeadStyle.WASHER -> {
                val w = DimensionTables.washerFor(type, unit, dMm)
                val (od, a) = pick("washer outside Ø", g.washerOuterDiaMm, w.outerDiaMm, w.source, w.isTable)
                val (id, b) = pick("washer bore Ø", g.washerInnerDiaMm, w.innerDiaMm, w.source, w.isTable)
                val (t, c) = pick("washer thickness", g.washerThicknessMm, w.thicknessMm, w.source, w.isTable)
                seg(ProfileSegment(t, od), "Washer disc", "Ø${n(od)} × ${n(t)} mm", worst(a, c), head = true)
                bore(min(id, 0.98 * od), "Bore", "Ø${n(id)} × ${n(t)} mm", worst(b, c))
                if (id >= od) cautions += "Bore Ø ${n(id)} is not smaller than the outside Ø ${n(od)}: check the drawing values."
            }

            HeadStyle.NUT -> {
                val (s, a) = pick("across flats s", g.acrossFlatsMm, table.acrossFlatsS)
                val (m, b) = pick("nut height m", g.nutHeightMm ?: g.headHeightMm, table.nutHeightM)
                val dc = g.flangeDiaMm
                val c = g.flangeThicknessMm
                if (dc != null && c != null && c > 0.0 && c < m && dc > s) {
                    fromDrawing += "flange Ø dc ${n(dc)} mm"
                    fromDrawing += "flange thickness c ${n(c)} mm"
                    seg(ProfileSegment(c, dc), "Flange", "Ø${n(dc)} × ${n(c)} mm", DimSource.DRAWING, head = true)
                    seg(ProfileSegment(m - c, s, shape = SectionShape.HEX), "Hex body", "s ${n(s)} × ${n(m - c)} mm", worst(a, b), head = true)
                } else {
                    seg(ProfileSegment(m, s, shape = SectionShape.HEX), "Hex body", "s ${n(s)} × m ${n(m)} mm", worst(a, b), head = true)
                    if (type == FastenerType.FLANGE_NUT) cautions += "Flange Ø dc and thickness c are not given: the volume is the hex body without the flange."
                }
                bore(blankDiaMm, "Threaded bore", "tap drill Ø${n(blankDiaMm, 3)} × ${n(m)} mm", DimSource.CALCULATED)
            }

            HeadStyle.HEADLESS -> {
                if (type == FastenerType.SOCKET_SET) {
                    lt = lengthMm
                    threadSeg(lengthMm, "Threaded body")
                    tableSocket?.let { (key0, depth0) ->
                        val (key, a) = pick("socket key s", g.socketSizeMm, key0, "ISO 4026 key = d / 2", true)
                        val (depth, b) = pick("socket depth t", g.socketDepthMm, depth0, "typical depth d / 2", false)
                        recess(Recess(true, key, depth, SectionShape.HEX), "Hex socket", "s ${n(key, 1)} × t ${n(depth, 1)} mm", worst(a, b))
                    }
                } else {
                    lt = min(threadLengthMm, lengthMm).coerceAtLeast(0.0)
                    noteThread(lt)
                    if (lt >= lengthMm - 1e-9) {
                        threadSeg(lengthMm)
                    } else {
                        val (ds, a) = pick("shank Ø", g.shankDiaMm, dMm, "nominal diameter", true)
                        if (lt <= 1e-9) {
                            seg(ProfileSegment(lengthMm, ds), "Plain body", "Ø${n(ds)} × ${n(lengthMm)} mm", a)
                        } else {
                            // Double-end studs carry half the thread length at each end
                            threadSeg(lt / 2, "Thread section (first end)")
                            seg(ProfileSegment(lengthMm - lt, ds), "Plain shank", "Ø${n(ds)} × ${n(lengthMm - lt)} mm", a)
                            threadSeg(lt / 2, "Thread section (second end)")
                        }
                    }
                }
            }

            HeadStyle.HEX_TRIMMED -> {
                val fg = forging!!
                lt = fg.threadLengthMm
                noteThread(lt)
                threadSeg(lt)
                seg(
                    ProfileSegment(fg.coneLengthMm, fg.blankDiaMm, fg.shankDiaMm), "Extrusion cone",
                    "15°/side, Ø${n(fg.blankDiaMm, 3)} → Ø${n(fg.shankDiaMm)} × ${n(fg.coneLengthMm)} mm", DimSource.CALCULATED
                )
                val plain = lengthMm - lt - fg.coneLengthMm
                if (plain > 1e-9) {
                    if (fg.shankFromDrawing) fromDrawing += "shank Ø ${n(fg.shankDiaMm)} mm"
                    else notOnDrawing += "shank Ø ${n(fg.shankDiaMm)} mm — wire diameter"
                    seg(ProfileSegment(plain, fg.shankDiaMm), "Plain shank", "Ø${n(fg.shankDiaMm)} × ${n(plain)} mm",
                        if (fg.shankFromDrawing) DimSource.DRAWING else DimSource.STANDARD)
                }
                val fl = flange?.takeIf { it.complete }
                if (fl != null) {
                    fun used(name: String, drawn: Double?, value: Double): DimSource =
                        if (drawn != null) { fromDrawing += "$name ${n(value)} mm"; DimSource.DRAWING }
                        else { notOnDrawing += "$name ${n(value)} mm — DIN 6921"; DimSource.STANDARD }
                    val sDc = used("flange Ø dc", g.flangeDiaMm, fl.dc!!)
                    val sC = used("flange thickness c", g.flangeThicknessMm, fl.c!!)
                    val sS = used("across flats s", g.acrossFlatsMm, fl.s!!)
                    val sK = used("head height k", g.headHeightMm, fl.k!!)
                    seg(ProfileSegment(fl.c, fl.dc), "Flange", "Ø${n(fl.dc)} × c ${n(fl.c)} mm", worst(sDc, sC), head = true)
                    seg(ProfileSegment(fl.k - fl.c, fl.s, shape = SectionShape.HEX), "Hex head", "s ${n(fl.s)} × ${n(fl.k - fl.c)} mm", worst(sS, sK, sC), head = true)
                } else {
                    val (s, a) = pick("across flats s", g.acrossFlatsMm, table.acrossFlatsS)
                    val (k, b) = pick("head height k", g.headHeightMm, table.headHeightK)
                    seg(ProfileSegment(k, s, shape = SectionShape.HEX), "Hex head", "s ${n(s)} × k ${n(k)} mm", worst(a, b), head = true)
                }
            }

            else -> {
                val round = style in ROUND_HEAD_STYLES
                val (headSize, a) = if (round) pick("head Ø dk", g.headDiaMm, table.acrossFlatsS) else pick("across flats s", g.acrossFlatsMm, table.acrossFlatsS)
                val (k, b) = pick("head height k", g.headHeightMm, table.headHeightK)
                // ISO 10642: the length of a countersunk screw includes its head
                val under = if (style == HeadStyle.SOCKET_COUNTERSUNK) max(0.0, lengthMm - k) else lengthMm
                var neckH = 0.0
                var neckSrc = DimSource.STANDARD
                if (style == HeadStyle.CARRIAGE) {
                    // No sourced neck height in the app: an estimate stands in, labelled, until the drawing gives f
                    val (h, c) = pick(
                        "square neck height f", g.neckHeightMm, table.neckHeightMm ?: 0.45 * dMm,
                        from = if (table.neckHeightMm != null) table.source else "estimated 0.45 d — take f from the drawing",
                        fromTable = table.neckHeightMm != null
                    )
                    neckH = min(h, under)
                    neckSrc = c
                }
                lt = if (type.isUnthreaded) 0.0 else min(threadLengthMm, under - neckH).coerceAtLeast(0.0)
                val plain = under - neckH - lt
                if (!type.isUnthreaded) noteThread(lt)
                var shankDia = dMm
                var shankSrc = DimSource.STANDARD
                if (plain > 1e-9) {
                    val (ds, c) = pick("shank Ø", g.shankDiaMm, dMm, "nominal diameter", true)
                    shankDia = ds
                    shankSrc = c
                }
                threadSeg(lt)
                seg(ProfileSegment(plain, shankDia), if (type.isUnthreaded) "Rivet shank" else "Plain shank", "Ø${n(shankDia)} × ${n(plain)} mm", shankSrc)
                seg(ProfileSegment(neckH, dMm, shape = SectionShape.SQUARE), "Square neck", "${n(dMm)} × ${n(dMm)} × ${n(neckH)} mm", neckSrc, head = true)

                // A flange or washer face under a hex / 12-point head counts only when the drawing gives both sizes
                val dc = g.flangeDiaMm
                val c = g.flangeThicknessMm
                val withFlange = (style == HeadStyle.HEX_FORMED || style == HeadStyle.TWELVE_POINT) &&
                    dc != null && c != null && c > 0.0 && c < k && dc > headSize
                if (withFlange) {
                    fromDrawing += "flange Ø dc ${n(dc!!)} mm"
                    fromDrawing += "flange thickness c ${n(c!!)} mm"
                    seg(ProfileSegment(c, dc), "Flange", "Ø${n(dc)} × c ${n(c)} mm", DimSource.DRAWING, head = true)
                }
                val headH = if (withFlange) k - c!! else k
                val headSrc = worst(a, b)
                when (style) {
                    HeadStyle.TWELVE_POINT ->
                        seg(ProfileSegment(headH, headSize, shape = SectionShape.TWELVE_POINT), "12-point head", "s ${n(headSize)} × ${n(headH)} mm", headSrc, true)
                    HeadStyle.SOCKET_CAP ->
                        seg(ProfileSegment(k, headSize), "Cylindrical head", "Ø${n(headSize)} × k ${n(k)} mm", headSrc, true)
                    HeadStyle.ROUND_PIN ->
                        seg(ProfileSegment(k, headSize), "Round head", "Ø${n(headSize)} × k ${n(k)} mm", headSrc, true)
                    HeadStyle.SOCKET_COUNTERSUNK -> {
                        val base = if (plain > 1e-9) shankDia else blankDiaMm
                        seg(ProfileSegment(k, base, headSize), "Countersunk head", "cone Ø${n(base)} → Ø${n(headSize)} × k ${n(k)} mm", headSrc, true)
                    }
                    HeadStyle.SOCKET_BUTTON ->
                        seg(ProfileSegment(k, headSize, shape = SectionShape.DOME), "Button head", "dome Ø${n(headSize)} × k ${n(k)} mm", headSrc, true)
                    HeadStyle.CARRIAGE ->
                        seg(ProfileSegment(k, headSize, shape = SectionShape.DOME), "Mushroom head", "dome Ø${n(headSize)} × k ${n(k)} mm", headSrc, true)
                    HeadStyle.RIVET ->
                        seg(ProfileSegment(k, headSize, shape = SectionShape.DOME), "Universal head", "dome Ø${n(headSize)} × k ${n(k)} mm", headSrc, true)
                    else ->
                        seg(ProfileSegment(headH, headSize, shape = SectionShape.HEX), "Hex head", "s ${n(headSize)} × ${n(headH)} mm", headSrc, true)
                }
                if (style in SOCKET_STYLES) tableSocket?.let { (key0, depth0) ->
                    val (key, x) = pick("socket key s", g.socketSizeMm, key0)
                    val (depth, y) = pick("socket depth t", g.socketDepthMm, depth0)
                    recess(Recess(true, key, depth, SectionShape.HEX), "Hex socket", "s ${n(key, 1)} × t ${n(depth, 1)} mm", worst(x, y))
                }
                if (type == FastenerType.FLANGE_BOLT) cautions += "Flange Ø dc and thickness c are not both given: the flange is not in the volume."
                if (style == HeadStyle.TWELVE_POINT && !withFlange) cautions += "The flange / washer face counts only when the drawing gives dc and c."
            }
        }
        shapeCaution(type)?.let { cautions += it }

        return FinishedPart(
            style = style, segments = segs, recesses = recs, throughHoleMm = hole, headSegmentCount = headCount,
            threadLengthMm = lt, blankDiaMm = blankDiaMm, items = items,
            fromDrawing = fromDrawing, notOnDrawing = notOnDrawing, cautions = cautions
        )
    }
}
