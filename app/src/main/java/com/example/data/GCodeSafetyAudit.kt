package com.example.data

data class GCodeCheck(val passed: Boolean, val text: String)

/**
 * Reads a Fanuc lathe program line by line and checks the rules that prevent crashes and scrap.
 * The G-code screen shows these results instead of fixed "passed" claims.
 */
object GCodeSafetyAudit {

    private val word = Regex("([A-Z])(-?\\d+\\.?\\d*)")

    fun audit(program: String): List<GCodeCheck> {
        val checks = mutableListOf<GCodeCheck>()
        val lines = program.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // Comments must be ASCII with no nested parentheses
        val badComment = lines.firstOrNull { line ->
            line.count { it == '(' } != line.count { it == ')' } || Regex("\\([^)]*\\(").containsMatchIn(line) || line.any { it.code > 126 }
        }
        checks += GCodeCheck(badComment == null, if (badComment == null) "Comments are plain ASCII without nested brackets" else "Invalid comment: $badComment")

        val oLine = lines.firstOrNull { it.startsWith("O") }
        val oOk = oLine != null && Regex("^O\\d{4}\\b").containsMatchIn(oLine)
        checks += GCodeCheck(oOk, if (oOk) "Program number ${oLine!!.take(5)} is 4 digits" else "Program number missing or longer than 4 digits")

        val header = lines.firstOrNull { it.startsWith("G21") }
        val headerOk = header != null && listOf("G21", "G40", "G80", "G97", "G99").all { header.contains(it) }
        checks += GCodeCheck(headerOk, if (headerOk) "Safety header resets units, compensation, cycles and spindle mode" else "Safety header incomplete (needs G21 G40 G80 G97 G99)")

        // Modal walk: spindle clamp before G96, G97 before threading, no diagonal rapid out of a bore
        var clampRpm: Int? = null
        var g96WithoutClamp = false
        var cssActive = false
        var threadingInCss = false
        var x: Double? = null
        var z: Double? = null
        var rapidFromInside: String? = null
        var coolantOn = false
        var coolantLeftOn = false

        // Profile blocks N(P)…N(Q) of G70/G71 are contour definitions: the cycle returns to its start point,
        // so they are not real tool positions for the collision check
        var profileEnd: Int? = null
        var inProfile = false

        for (raw in lines) {
            val line = raw.replace(Regex("\\([^)]*\\)"), "").trim()
            if (line.isEmpty() || line == "%") continue
            val words = word.findAll(line).map { it.groupValues[1] to it.groupValues[2] }.toList()
            val gCodes = words.filter { it.first == "G" }.map { it.second.toDouble().toInt() }

            val seq = words.firstOrNull { it.first == "N" }?.second?.toDouble()?.toInt()
            if (gCodes.any { it == 70 || it == 71 || it == 72 || it == 73 }) {
                val q = words.firstOrNull { it.first == "Q" }?.second?.toDouble()?.toInt()
                if (words.any { it.first == "P" } && q != null && 70 !in gCodes) profileEnd = q
            }
            if (seq != null && profileEnd != null && !inProfile) inProfile = true
            if (inProfile) {
                if (seq != null && seq == profileEnd) { inProfile = false; profileEnd = null }
                continue
            }

            if (50 in gCodes) clampRpm = words.firstOrNull { it.first == "S" }?.second?.toDouble()?.toInt()
            if (96 in gCodes) { cssActive = true; if (clampRpm == null) g96WithoutClamp = true }
            if (97 in gCodes) cssActive = false
            if (76 in gCodes && cssActive) threadingInCss = true

            val newX = words.firstOrNull { it.first == "X" }?.second?.toDouble()
            val newZ = words.firstOrNull { it.first == "Z" }?.second?.toDouble()
            val isCycle = gCodes.any { it in listOf(70, 71, 76) }
            if (0 in gCodes && !isCycle && newX != null && newZ != null && z != null && z!! < 0.0 && newX != x && newZ != z) {
                rapidFromInside = raw
            }
            if (!isCycle && 28 !in gCodes) {
                if (newX != null) x = newX
                if (newZ != null) z = newZ
            }

            val mCodes = words.filter { it.first == "M" }.map { it.second.toDouble().toInt() }
            if (8 in mCodes) coolantOn = true
            if (9 in mCodes) coolantOn = false
            if (30 in mCodes && coolantOn) coolantLeftOn = true
        }

        checks += GCodeCheck(clampRpm != null && !g96WithoutClamp,
            if (clampRpm != null && !g96WithoutClamp) "G50 spindle clamp S$clampRpm set before constant surface speed (G96)" else "G96 used without a G50 spindle speed clamp")
        val hasThreading = lines.any { it.contains("G76") }
        if (hasThreading) {
            checks += GCodeCheck(!threadingInCss, if (!threadingInCss) "Threading (G76) runs at constant RPM (G97)" else "G76 threading in G96 mode — pitch synchronisation will fail")
        }
        checks += GCodeCheck(rapidFromInside == null,
            if (rapidFromInside == null) "No diagonal rapid while the tool is inside the part" else "Crash risk — diagonal rapid from Z<0: $rapidFromInside")
        checks += GCodeCheck(!coolantLeftOn, if (!coolantLeftOn) "Coolant switched off (M09) before program end" else "Coolant still on at M30")
        val ends = lines.any { it.startsWith("M30") } && lines.lastOrNull() == "%"
        checks += GCodeCheck(ends, if (ends) "Program ends with M30 and %" else "Program end (M30 / %) missing")
        return checks
    }
}
