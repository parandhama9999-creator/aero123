package com.example.data

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
data class SteelComposition(
    val carbon: Double,
    val manganese: Double,
    val silicon: Double,
    val chromium: Double,
    val nickel: Double,
    val molybdenum: Double = 0.0
)

object SteelCompositions {

    /**
     * Andrews (1965), the linear regression still used for A1 on low-alloy steels:
     * Ac1 = 723 − 10.7·Mn − 16.9·Ni + 29.1·Si + 16.9·Cr
     *
     * It holds for the low-alloy range covered here. It is not valid for a high-alloy grade such as
     * a duplex or an austenitic, which have no A1 to anneal below in the first place.
     */
    fun ac1(c: SteelComposition): Double =
        723.0 - 10.7 * c.manganese - 16.9 * c.nickel + 29.1 * c.silicon + 16.9 * c.chromium

    /** A1 for a plain carbon steel, the fallback when a grade's composition is not on file. */
    const val A1_PLAIN_CARBON_C = 723.0

    /**
     * Carbon below this leaves nothing to spheroidise. The case it is here for is maraging steel,
     * which is iron-nickel martensite at about 0.02 %. A 1008 at 0.08 % still carries cementite
     * worth balling up, so the threshold sits under it.
     */
    const val MIN_CARBON_TO_SPHEROIDISE = 0.05

    private val C = ::SteelComposition

    val BY_KEY: Map<String, SteelComposition> = mapOf(
        // --- plain carbon -------------------------------------------------------------------
        "1008" to C(0.08, 0.40, 0.08, 0.00, 0.00, 0.00),
        "1018" to C(0.18, 0.75, 0.20, 0.00, 0.00, 0.00),
        "1022" to C(0.22, 0.85, 0.20, 0.00, 0.00, 0.00),
        "1035" to C(0.35, 0.75, 0.20, 0.00, 0.00, 0.00),
        "1045" to C(0.45, 0.75, 0.20, 0.00, 0.00, 0.00),
        "EN8" to C(0.40, 0.80, 0.20, 0.00, 0.00, 0.00),

        // --- boron and medium carbon --------------------------------------------------------
        "10B21" to C(0.21, 0.90, 0.20, 0.00, 0.00, 0.00),
        "10B33" to C(0.33, 0.90, 0.20, 0.00, 0.00, 0.00),
        "20MNB4" to C(0.20, 1.00, 0.20, 0.00, 0.00, 0.00),
        "23MNB4" to C(0.23, 1.05, 0.20, 0.00, 0.00, 0.00),
        "30MNB4" to C(0.30, 1.05, 0.20, 0.00, 0.00, 0.00),
        "32CRB4" to C(0.32, 0.60, 0.20, 0.40, 0.00, 0.00),
        "36CRB4" to C(0.36, 0.60, 0.20, 0.40, 0.00, 0.00),
        "41CR4" to C(0.41, 0.75, 0.25, 1.05, 0.00, 0.00),
        "AISI_51B37" to C(0.37, 0.90, 0.22, 0.85, 0.00, 0.00),
        "16MNCR5" to C(0.16, 1.15, 0.25, 0.95, 0.00, 0.00),

        // --- chromium-molybdenum ------------------------------------------------------------
        "AISI_4130" to C(0.30, 0.50, 0.25, 0.95, 0.00, 0.20),
        "4135" to C(0.35, 0.80, 0.25, 0.95, 0.00, 0.20),
        "SCM435" to C(0.35, 0.75, 0.25, 1.05, 0.00, 0.20),
        "4140" to C(0.40, 0.88, 0.25, 0.95, 0.00, 0.20),
        "SCM440" to C(0.40, 0.75, 0.25, 1.05, 0.00, 0.22),
        "42CRMO4" to C(0.42, 0.75, 0.25, 1.05, 0.00, 0.22),
        "34CRMO4" to C(0.34, 0.70, 0.25, 1.05, 0.00, 0.22),
        "18CRMO4" to C(0.18, 0.70, 0.25, 1.05, 0.00, 0.22),
        "AISI_4037" to C(0.37, 0.80, 0.25, 0.00, 0.00, 0.25),
        "EN16" to C(0.36, 1.50, 0.25, 0.00, 0.00, 0.27),

        // --- nickel-chromium-molybdenum -----------------------------------------------------
        "8740" to C(0.40, 0.85, 0.25, 0.50, 0.55, 0.25),
        "4340" to C(0.40, 0.70, 0.25, 0.80, 1.83, 0.25),
        "EN24" to C(0.40, 0.65, 0.25, 1.10, 1.50, 0.28),
        "EN24_ESR" to C(0.40, 0.65, 0.25, 1.10, 1.50, 0.28),
        "34CRNIMO6" to C(0.34, 0.65, 0.25, 1.55, 1.55, 0.22),
        // 4 % nickel drops A1 by about 65 °C: the grade that a plain-carbon band gets wrong
        "35NCD16" to C(0.35, 0.45, 0.25, 1.80, 3.85, 0.35),
        // 1.6 % silicon pushes A1 the other way
        "300M" to C(0.42, 0.75, 1.60, 0.80, 1.80, 0.40),

        // --- silicon-bearing GOST aero grades -----------------------------------------------
        "30KHGSA" to C(0.30, 0.95, 1.05, 0.95, 0.00, 0.00),
        "16KHGSN" to C(0.16, 1.00, 0.80, 0.85, 1.40, 0.00),

        // --- tool steel ----------------------------------------------------------------------
        "H11_H13" to C(0.38, 0.35, 1.00, 5.10, 0.00, 1.35),

        // --- essentially carbon free: listed so the spheroidize gate can refuse it -----------
        "MARAGING_250" to C(0.02, 0.08, 0.08, 0.00, 18.50, 4.80)
    )

    fun forMaterial(m: MaterialInfo): SteelComposition? = BY_KEY[m.key.uppercase()]

    /**
     * Andrews is a low-alloy regression. Past roughly 5 % nickel or 6 % chromium it is being
     * extrapolated far outside the data it was fitted to, and it returns nonsense: maraging C250 at
     * 18.5 % Ni comes out near 412 °C. Those grades have no A1 worth annealing below anyway.
     */
    fun andrewsApplies(c: SteelComposition): Boolean = c.nickel <= 5.0 && c.chromium <= 6.0

    /** A1 for this grade, or null when its composition is not on file or is outside Andrews' range. */
    fun ac1For(m: MaterialInfo): Double? =
        forMaterial(m)?.takeIf { andrewsApplies(it) }?.let { ac1(it) }

    /**
     * Whether a grade has enough carbon for a spheroidize anneal to mean anything. Unknown
     * composition is treated as headable steel, which is what the catalogue's other steels are.
     */
    fun canSpheroidise(m: MaterialInfo): Boolean =
        forMaterial(m)?.let { it.carbon >= MIN_CARBON_TO_SPHEROIDISE } ?: true

    /**
     * Soak length from alloy content alone (grade factor). Chromium and molybdenum carbides are
     * slower to break up and ball, so those grades hold longer; carbon adds to it.
     */
    fun soakHoursForGrade(m: MaterialInfo): Pair<Double, Double> {
        val c = forMaterial(m) ?: return 6.0 to 8.0
        val carbideFormers = c.chromium + c.molybdenum
        return when {
            carbideFormers >= 0.80 -> 8.0 to 10.0
            carbideFormers >= 0.30 || c.carbon >= 0.18 -> 6.0 to 8.0   // the 1022 Taguchi optimum sits here
            else -> 5.0 to 7.0
        }
    }

    /**
     * Soak length from wire diameter alone (thermal penetration). Thicker wire needs longer for
     * the core to reach temperature and for carbon to diffuse to the carbide particles.
     */
    fun soakHoursForDiameter(wireDiaMm: Double): Pair<Double, Double> = when {
        wireDiaMm <= 4.0  -> 6.0 to 7.0
        wireDiaMm <= 8.0  -> 7.0 to 9.0
        wireDiaMm <= 16.0 -> 9.0 to 12.0
        wireDiaMm <= 25.0 -> 12.0 to 16.0
        else              -> 16.0 to 20.0
    }

    /**
     * Combined soak: the longer of grade factor and diameter factor at each end. Small Cr-Mo wire
     * is grade-limited (carbide dissolution); large plain-carbon wire is diameter-limited (thermal
     * penetration). When [wireDiaMm] is zero or negative the diameter factor is skipped and the
     * result is grade-only, for backward compatibility.
     */
    fun soakHours(m: MaterialInfo, wireDiaMm: Double = 0.0): Pair<Double, Double> {
        val (gLo, gHi) = soakHoursForGrade(m)
        if (wireDiaMm <= 0.0) return gLo to gHi
        val (dLo, dHi) = soakHoursForDiameter(wireDiaMm)
        return maxOf(gLo, dLo) to maxOf(gHi, dHi)
    }
}
