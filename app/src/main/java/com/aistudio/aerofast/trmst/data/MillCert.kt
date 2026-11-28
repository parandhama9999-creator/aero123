package com.aistudio.aerofast.trmst.data

import kotlinx.serialization.Serializable

import java.util.Locale

/** Raw wire as tested on the supplier's mill certificate. A null field was not entered. */
@Serializable
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
@Serializable
data class WireCheck(
    val verdict: WireVerdict,
    /** e.g. "580 – 660 MPa" or "≈ 82 – 88 HRB (derived)". */
    val reference: String,
    val message: String,
    /** The head is back-extruded (socket / recess), the most severe heading operation. */
    val recessHead: Boolean
)
