package com.aistudio.aerofast.trmst.data

import java.util.Locale

/** Raw wire as tested on the supplier's mill certificate. A null field was not entered. */
data class MillCert(val rmMpa: Double? = null, val hrb: Double? = null) {
    val isEmpty: Boolean get() = rmMpa == null && hrb == null

    companion object {
        /** Null when nothing usable was entered. */
        fun of(rmMpa: Double?, hrb: Double?): MillCert? =
            MillCert(rmMpa?.takeIf { it in 100.0..3000.0 }, hrb?.takeIf { it in 20.0..120.0 }).takeIf { !it.isEmpty }
    }
}

enum class WireVerdict { HEADABLE, SOFTER_THAN_RANGE, TOO_HARD, NO_REFERENCE }

/** The certificate compared with the catalogue's headable (annealed) condition for the grade. */
data class WireCheck(
    val verdict: WireVerdict,
    /** e.g. "580 – 660 MPa" or "≈ 82 – 88 HRB (derived)". */
    val reference: String,
    val message: String,
    /** The head is back-extruded (socket / recess), the most severe heading operation. */
    val recessHead: Boolean
)

/**
 * Mill certificate gate. Compares the wire's tested Rm (and/or HRB) with the raw-wire range the catalogue lists for the
 * grade in its headable condition. Nothing here is a universal limit: the reference is the app's own catalogue range,
 * and the HRB band is derived from it (Rm / 3.38 = HB, ASTM A370 HB → HRB), as on the Soft Anneal card.
 */
object MillCertCheck {
    private fun f(v: Double) = String.format(Locale.US, "%.0f", v)

    /** True for parts whose head carries a back-extruded socket or recess. */
    fun hasRecessHead(type: FastenerType, part: FinishedPart?): Boolean =
        part?.recesses?.any { it.fromTop } == true ||
            type == FastenerType.SHCS || type == FastenerType.SOCKET_BUTTON || type == FastenerType.SOCKET_CSK

    fun check(material: MaterialInfo, type: FastenerType, part: FinishedPart?, cert: MillCert?): WireCheck? {
        if (cert == null || cert.isEmpty) return null
        val recess = hasRecessHead(type, part)
        val supplied = material.rawMaterialSourcingUtsMpa
        val family = PokaYokeEngine.materialFamily(material)
        val isSteel = family == MaterialFamily.CARBON_ALLOY_STEEL || family == MaterialFamily.STAINLESS
        val hardCatalogue = supplied.uppercase(Locale.ROOT).let { it.contains("COLD DRAWN") || it.contains("HALF-HARD") || it.contains("HALF HARD") }
        val range = SoftAnnealEngine.parseUtsRange(supplied)

        if (range == null || hardCatalogue) {
            val why = if (hardCatalogue) "The catalogue lists ${material.key} wire in the hard (cold drawn / half-hard) condition, which is not a headable condition, so there is no headable range to compare with."
                else "The catalogue has no raw-wire Rm for ${material.key} (\"$supplied\")."
            return WireCheck(WireVerdict.NO_REFERENCE, supplied, "$why Confirm the condition with the wire supplier (see the Soft Anneal card).", recess)
        }
        val (lo, hi) = range
        val hrbLo = if (isSteel) SoftAnnealEngine.hbToHrb(lo / SoftAnnealEngine.HB_PER_MPA) else null
        val hrbHi = if (isSteel) SoftAnnealEngine.hbToHrb(hi / SoftAnnealEngine.HB_PER_MPA) else null

        val rmHard = cert.rmMpa != null && cert.rmMpa > hi
        val hrbHard = cert.hrb != null && hrbHi != null && cert.hrb > hrbHi
        val rmSoft = cert.rmMpa != null && cert.rmMpa < lo
        val hrbSoft = cert.hrb != null && hrbLo != null && cert.hrb < hrbLo
        val refText = "${f(lo)} – ${f(hi)} MPa" + if (hrbLo != null && hrbHi != null) " (≈ ${f(hrbLo)} – ${f(hrbHi)} HRB, derived)" else ""
        val given = listOfNotNull(cert.rmMpa?.let { "Rm ${f(it)} MPa" }, cert.hrb?.let { "${f(it)} HRB" }).joinToString(", ")
        val hrbIgnored = if (cert.hrb != null && hrbHi == null) " The HRB value is not compared: the HRB band is derived for steels only." else ""

        return when {
            rmHard || hrbHard -> WireCheck(
                WireVerdict.TOO_HARD, refText,
                "Mill cert $given is above the catalogue's headable range for ${material.key} ($refText): the wire is not in the annealed condition the heading plan assumes." +
                    (if (recess) " The socket / recess is back-extruded, the most severe heading operation: hard wire risks punch breakage and cracks at the socket corners." else "") +
                    hrbIgnored,
                recess
            )
            rmSoft || hrbSoft -> WireCheck(
                WireVerdict.SOFTER_THAN_RANGE, refText,
                "Mill cert $given is below the catalogue range for ${material.key} ($refText). Softer wire heads more easily; the final strength comes from the heat treatment, so check the grade is right on the certificate.$hrbIgnored",
                recess
            )
            else -> WireCheck(
                WireVerdict.HEADABLE, refText,
                "Mill cert $given is within the catalogue's headable range for ${material.key} ($refText)." +
                    (if (recess) " Socket / recess heading: OK on raw-wire strength." else "") + hrbIgnored,
                recess
            )
        }
    }

    fun issues(c: WireCheck?): List<PokaYokeIssue> = when (c?.verdict) {
        WireVerdict.TOO_HARD -> listOf(
            PokaYokeIssue(
                if (c.recessHead) PokaYokeSeverity.MANDATORY else PokaYokeSeverity.WARNING,
                if (c.recessHead) "Wire too hard for the socket head" else "Wire harder than the headable range",
                c.message,
                "Spheroidize / soft anneal the wire before heading (cycle on the Soft Anneal card), or source annealed wire with Rm in ${c.reference}."
            )
        )
        WireVerdict.NO_REFERENCE -> listOf(PokaYokeIssue(PokaYokeSeverity.WARNING, "Mill cert not checked against a range", c.message, "Get the headable Rm / hardness for this grade from the wire supplier."))
        else -> emptyList()
    }
}
