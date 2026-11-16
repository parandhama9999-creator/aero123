package com.aistudio.aerofast.trmst.data

/**
 * An ISO 965 tolerance class: pitch-diameter tolerance followed by crest tolerance (major diameter for external
 * threads, minor diameter for internal), e.g. "4g6g" = pitch 4g, major 6g; "6g" = both 6g. "-MJ" marks the
 * ISO 5855 aerospace profile with the controlled root radius.
 */
data class IsoThreadClass(val pitchGrade: Int, val crestGrade: Int, val position: String, val isMj: Boolean) {
    companion object {
        private val PATTERN = Regex("""^(\d)([A-Za-z]{1,2})(?:(\d)([A-Za-z]{1,2}))?$""")

        fun parse(code: String): IsoThreadClass {
            val mj = code.endsWith("-MJ", ignoreCase = true)
            val core = code.removeSuffix("-MJ").removeSuffix("-mj").trim()
            val m = PATTERN.find(core)
                ?: return IsoThreadClass(6, 6, if (core.any { it.isUpperCase() }) "H" else "g", mj)
            val g1 = m.groupValues[1].toInt()
            val g2 = m.groupValues[3].toIntOrNull() ?: g1
            return IsoThreadClass(g1, g2, m.groupValues[2], mj)
        }

        /** ISO 965-1 tolerance-grade factors relative to grade 6 (Td2, TD2, TD1). */
        fun gradeFactor(grade: Int): Double = when (grade) {
            3 -> 0.50
            4 -> 0.63
            5 -> 0.80
            6 -> 1.00
            7 -> 1.25
            8 -> 1.60
            9 -> 2.00
            else -> 1.00
        }

        /** Major-diameter tolerance Td exists only in grades 4, 6 and 8. */
        fun majorGradeFactor(grade: Int): Double = when {
            grade <= 4 -> 0.63
            grade <= 6 -> 1.00
            else -> 1.60
        }

        /** Upper deviation es (mm) of an external thread; ISO 965-1 / 965-3, P in mm. */
        fun externalEs(position: String, p: Double): Double = when (position) {
            "d" -> -(0.080 + 0.011 * p)
            "e" -> -(0.050 + 0.011 * p)
            "f" -> -(0.030 + 0.011 * p)
            "h" -> 0.0
            else -> -(0.015 + 0.011 * p) // g
        }

        /**
         * Lower deviation EI (mm) of an internal thread. G and H per ISO 965-1; AZ and AX are the oversized
         * hot-dip-galvanizing positions of ISO 965-5 (EI_AZ = 300 + 20P µm, EI_AX = 220P − 20 µm).
         */
        fun internalEi(position: String, p: Double): Double = when (position) {
            "G" -> 0.015 + 0.011 * p
            "AZ" -> 0.300 + 0.020 * p
            "AX" -> 0.220 * p - 0.020
            else -> 0.0 // H
        }
    }
}
