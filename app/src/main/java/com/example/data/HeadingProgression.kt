package com.example.data

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

enum class SectionShape { ROUND, HEX }

private val HEX_AREA_FACTOR = sqrt(3.0) / 2.0

private fun sectionArea(shape: SectionShape, size: Double) =
    if (shape == SectionShape.HEX) HEX_AREA_FACTOR * size * size else PI / 4.0 * size * size

/** One axial slice of the part, listed from the die end (bottom) to the punch end (top). HEX size is across flats. */
data class ProfileSegment(
    val heightMm: Double,
    val dBottomMm: Double,
    val dTopMm: Double = dBottomMm,
    val shape: SectionShape = SectionShape.ROUND
) {
    val volumeMm3: Double
        get() = if (shape == SectionShape.HEX) sectionArea(shape, dBottomMm) * heightMm
        else PI * heightMm / 12.0 * (dBottomMm * dBottomMm + dBottomMm * dTopMm + dTopMm * dTopMm)

    /** Width seen in side view; a hex is drawn across corners. */
    val maxWidthMm: Double
        get() = if (shape == SectionShape.HEX) dBottomMm * 2.0 / sqrt(3.0) else max(dBottomMm, dTopMm)
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
    val maxWidthMm: Double get() = segments.maxOf { it.maxWidthMm }
    val volumeMm3: Double
        get() = segments.sumOf { it.volumeMm3 } - recesses.sumOf { it.volumeMm3 } -
            (throughHoleMm?.let { sectionArea(SectionShape.ROUND, it) * heightMm } ?: 0.0)
}

data class OffHeaderOp(val title: String, val operation: String)

data class HeadingPlan(
    val slugDiaMm: Double,
    val slugLengthMm: Double,
    val slugVolumeMm3: Double,
    val stations: List<HeadingStation>,
    val offHeaderOps: List<OffHeaderOp>,
    /** Upsetting blows the head stock needs (1 ≤ L/D 2.3, 2 ≤ 4.5, else 3). */
    val upsetBlows: Int,
    val notes: List<String>
) {
    val dieStations: Int get() = stations.count { !it.isCutOff }

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
 * Builds a cold-heading station sequence whose every station holds the slug volume (constant-volume
 * plastic flow), so the drawing and the stage list describe a manufacturable progression.
 * Upset limits follow the classic heading rules: free stock L/D ≤ 2.3 in one blow,
 * ≤ 4.5 with one cone pre-upset, beyond that two cones.
 */
object HeadingProgression {
    const val SINGLE_BLOW_LD = 2.3
    const val TWO_BLOW_LD = 4.5
    const val THREE_BLOW_LD = 6.5

    fun build(
        type: FastenerType,
        wireDiaMm: Double,
        nominalDiaMm: Double,
        lengthMm: Double,
        threadLengthMm: Double,
        blankDiaMm: Double,
        partVolumeMm3: Double,
        head: HeadDimensions,
        socket: Pair<Double, Double>?,
        threadLabel: String
    ): HeadingPlan? = when {
        type.isWasher -> null
        type.isNut -> nut(wireDiaMm, blankDiaMm, partVolumeMm3, head, threadLabel)
        else -> {
            val lt = threadLengthMm.coerceIn(0.0, lengthMm)
            val headVol = partVolumeMm3 - area(blankDiaMm) * lt - area(nominalDiaMm) * (lengthMm - lt)
            if (type.isStud || type == FastenerType.SOCKET_SET || headVol <= 0.0) {
                headless(type, wireDiaMm, nominalDiaMm, lengthMm, lt, blankDiaMm, partVolumeMm3, socket, threadLabel)
            } else {
                headed(type, wireDiaMm, nominalDiaMm, lengthMm, lt, blankDiaMm, headVol, partVolumeMm3, head, socket, threadLabel)
            }
        }
    }

    private fun area(d: Double) = sectionArea(SectionShape.ROUND, d)
    private fun f(v: Double, decimals: Int = 2) = String.format(java.util.Locale.US, "%.${decimals}f", v)

    private fun cutOff(wire: Double, length: Double) = HeadingStation(
        index = 0,
        title = "Shear cut-off",
        operation = "Shear slug Ø${f(wire)} × ${f(length, 1)} mm from wire coil",
        segments = listOf(ProfileSegment(length, wire))
    )

    private fun rollingOps(threadLabel: String) = listOf(
        OffHeaderOp("Pointing", "Chamfer thread start to minor diameter (45°)"),
        OffHeaderOp("Thread rolling", "Roll $threadLabel on flat or cylindrical dies")
    )

    private fun headed(
        type: FastenerType, wire: Double, d: Double, length: Double, lt: Double, blank: Double,
        headVol: Double, slugVol: Double, head: HeadDimensions, socket: Pair<Double, Double>?, threadLabel: String
    ): HeadingPlan {
        val aw = area(wire)
        val slugLen = slugVol / aw
        val stock = headVol / aw
        val ld = stock / wire
        val blows = when {
            ld <= SINGLE_BLOW_LD -> 1
            ld <= TWO_BLOW_LD -> 2
            else -> 3
        }
        val isSocket = socket != null
        val shank = buildList {
            if (lt > 0.0) add(ProfileSegment(lt, blank))
            if (length - lt > 0.0) add(ProfileSegment(length - lt, d))
        }
        val reductionPct = (1.0 - area(blank) / aw) * 100.0
        // The round preform must enter the hex punch insert (inscribed circle s) or match the socket head Ø
        // A rivet head is round: the upset IS the finished head. Hex/socket heads need a preform that fits the tooling.
        val roundHead = type.isUnthreaded
        val dRound = when {
            roundHead -> head.acrossFlatsS
            isSocket -> 0.97 * head.acrossFlatsS
            else -> 0.96 * head.acrossFlatsS
        }

        val stations = mutableListOf(cutOff(wire, slugLen))
        stations += HeadingStation(
            index = 1,
            title = "Square & forward extrude",
            operation = if (lt > 0.0) "Square sheared ends; extrude thread section Ø${f(wire)} → Ø${f(blank, 3)} × ${f(lt, 1)} mm " +
                "(${f(reductionPct, 0)} % area reduction); head stock ${f(stock, 1)} mm, L/D ${f(ld)}"
            else "Square sheared ends; head stock ${f(stock, 1)} mm, L/D ${f(ld)}",
            segments = shank + ProfileSegment(stock, wire),
            inDieMm = length
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
                segments = shank + ProfileSegment(hc, dc, wire),
                inDieMm = length
            )
        }

        val hRound = headVol / area(dRound)
        stations += HeadingStation(
            index = stations.size,
            title = if (roundHead) "Head upset (finished round head)" else "Head upset",
            operation = "Upset round head Ø${f(dRound)} × ${f(hRound)} mm against die face",
            segments = shank + ProfileSegment(hRound, dRound),
            inDieMm = length,
            isPeakBlow = isSocket || roundHead
        )

        if (!roundHead) stations += if (isSocket) {
            val (key, depth) = socket!!
            HeadingStation(
                index = stations.size,
                title = "Socket back-extrude",
                operation = (if (type == FastenerType.HEXALOBULAR_SCREW) "Back-extrude hexalobular recess (volume modelled as hex s=${f(key, 1)})"
                    else "Back-extrude hex socket s=${f(key, 1)}") +
                    " × t=${f(depth, 1)} mm; finish head Ø${f(head.acrossFlatsS)} × k=${f(head.headHeightK)} mm",
                segments = shank + ProfileSegment(head.headHeightK, head.acrossFlatsS),
                recesses = listOf(Recess(true, key, depth, SectionShape.HEX)),
                inDieMm = length
            )
        } else {
            HeadingStation(
                index = stations.size,
                title = "Hex form",
                operation = "Close hex s=${f(head.acrossFlatsS)} × k=${f(head.headHeightK)} mm in punch hex insert, 30° chamfer, bearing face",
                segments = shank + ProfileSegment(head.headHeightK, head.acrossFlatsS, shape = SectionShape.HEX),
                inDieMm = length,
                isPeakBlow = true
            )
        }

        val notes = buildList {
            if (ld > THREE_BLOW_LD) add("Head stock L/D ${f(ld)} exceeds the 3-blow limit ($THREE_BLOW_LD): use larger wire or warm forming.")
            if (reductionPct > 30.0) add("Open-die extrusion ${f(reductionPct, 0)} % exceeds ~30 %: extrude in a closed (trapped) die to avoid upsetting the shank.")
            if (type == FastenerType.BLIND_RIVET) add("Blind rivet: only the sleeve is modelled, as a solid shank. The sleeve bore and the mandrel are separate parts not covered here.")
        }
        return HeadingPlan(wire, slugLen, slugVol, stations, if (roundHead) emptyList() else rollingOps(threadLabel), blows, notes)
    }

    private fun headless(
        type: FastenerType, wire: Double, d: Double, length: Double, lt: Double, blank: Double,
        slugVol: Double, socket: Pair<Double, Double>?, threadLabel: String
    ): HeadingPlan {
        val slugLen = slugVol / area(wire)
        val stations = mutableListOf(cutOff(wire, slugLen))
        if (type == FastenerType.SOCKET_SET && socket != null) {
            val (key, depth) = socket
            stations += HeadingStation(
                index = 1,
                title = "Square & extrude",
                operation = "Square ends; extrude full length Ø${f(wire)} → Ø${f(blank, 3)} mm",
                segments = listOf(ProfileSegment(slugVol / area(blank), blank)),
                inDieMm = slugVol / area(blank)
            )
            stations += HeadingStation(
                index = 2,
                title = "Socket back-extrude",
                operation = "Back-extrude hex socket s=${f(key, 1)} × t=${f(depth, 1)} mm; part grows to L=${f(length, 1)} mm",
                segments = listOf(ProfileSegment(length, blank)),
                recesses = listOf(Recess(true, key, depth, SectionShape.HEX)),
                inDieMm = length,
                isPeakBlow = true
            )
        } else {
            // Double-end studs carry half the thread length at each end
            val segments = when {
                lt >= length -> listOf(ProfileSegment(length, blank))
                lt <= 0.0 -> listOf(ProfileSegment(length, d))
                else -> listOf(ProfileSegment(lt / 2, blank), ProfileSegment(length - lt, d), ProfileSegment(lt / 2, blank))
            }
            stations += HeadingStation(
                index = 1,
                title = "Square & extrude ends",
                operation = "Square ends; extrude thread sections Ø${f(wire)} → Ø${f(blank, 3)} mm",
                segments = segments,
                inDieMm = length,
                isPeakBlow = true
            )
        }
        return HeadingPlan(wire, slugLen, slugVol, stations, rollingOps(threadLabel), 0, emptyList())
    }

    private fun nut(wire: Double, hole: Double, nutVol: Double, head: HeadDimensions, threadLabel: String): HeadingPlan {
        val s = head.acrossFlatsS
        val m = head.nutHeightM
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
                title = "Hex form",
                operation = "Close hex s=${f(s)} × m=${f(m)} mm, deepen indents to ${f(indentFinal)} mm, leave web ${f(web)} mm",
                segments = listOf(ProfileSegment(m, s, shape = SectionShape.HEX)),
                recesses = listOf(Recess(true, hole, indentFinal, SectionShape.ROUND), Recess(false, hole, indentFinal, SectionShape.ROUND)),
                isPeakBlow = true
            ),
            HeadingStation(
                index = 4,
                title = "Pierce",
                operation = "Punch out web Ø${f(hole)} × ${f(web)} mm (${f(webVol / 1000.0, 3)} cm³ scrap); bore = tap drill",
                segments = listOf(ProfileSegment(m, s, shape = SectionShape.HEX)),
                throughHoleMm = hole,
                scrapMm3 = webVol
            )
        )
        return HeadingPlan(
            wire, slugLen, slugVol, stations,
            listOf(OffHeaderOp("Tapping", "Tap $threadLabel on bent-shank nut tapper")),
            0, emptyList()
        )
    }

    /** Large-end diameter D of a frustum of height h with small end d holding volume v. */
    private fun coneLargeEnd(v: Double, d: Double, h: Double): Double {
        val c = 12.0 * v / (PI * h)
        return (-d + sqrt(d * d - 4.0 * (d * d - c))) / 2.0
    }
}
