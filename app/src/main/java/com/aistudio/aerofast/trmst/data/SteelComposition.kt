package com.aistudio.aerofast.trmst.data

import kotlinx.serialization.Serializable

/**
 * Nominal composition of the headable steels, and the lower critical temperature A1 it implies.
 *
 * A sub-critical spheroidize anneal has to sit just below A1: above it the structure austenitises
 * and the anneal is undone. A1 is not a constant. Nickel and manganese depress it, silicon and
 * chromium raise it, and across the grades here that spans more than 140 °C — so a single band
 * cannot serve all of them.
 *
 * Figures are the mid-range of each grade's own specification (AISI/SAE, EN, GOST, AFNOR, AMS).
 * They are nominal, not a mill certificate: a heat at the far end of its range shifts A1 by several
 * degrees, so confirm against the certificate before setting a furnace.
 */
@Serializable
data class SteelComposition(
    val carbon: Double,
    val manganese: Double,
    val silicon: Double,
    val chromium: Double,
    val nickel: Double,
    val molybdenum: Double = 0.0
)
