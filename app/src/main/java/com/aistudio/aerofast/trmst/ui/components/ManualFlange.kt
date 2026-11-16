package com.aistudio.aerofast.trmst.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Flange values typed on the Sizing card (from the drawing), tied to the bolt size they were entered for. */
object ManualFlange {
    var diaMm by mutableStateOf(-1.0)
    var dc by mutableStateOf("")
    var k by mutableStateOf("")
    var s by mutableStateOf("")
    var c by mutableStateOf("")

    private fun value(text: String, forDiaMm: Double) = if (kotlin.math.abs(diaMm - forDiaMm) < 1e-6) text.toDoubleOrNull()?.takeIf { it > 0 } else null
    fun dcFor(d: Double) = value(dc, d)
    fun kFor(d: Double) = value(k, d)
    fun sFor(d: Double) = value(s, d)
    fun cFor(d: Double) = value(c, d)
}
