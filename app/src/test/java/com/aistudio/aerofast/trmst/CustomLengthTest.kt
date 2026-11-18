package com.aistudio.aerofast.trmst

import com.aistudio.aerofast.trmst.data.CalculationEngine
import com.aistudio.aerofast.trmst.data.CoatingType
import com.aistudio.aerofast.trmst.data.DrawingGeometry
import com.aistudio.aerofast.trmst.data.FastenerCatalog
import com.aistudio.aerofast.trmst.data.FastenerType
import com.aistudio.aerofast.trmst.data.GripStatus
import com.aistudio.aerofast.trmst.data.JointGrip
import com.aistudio.aerofast.trmst.data.PokaYokeEngine
import com.aistudio.aerofast.trmst.data.PokaYokeIssue
import com.aistudio.aerofast.trmst.data.PropertyClass
import com.aistudio.aerofast.trmst.data.SelectionFilter
import com.aistudio.aerofast.trmst.data.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Customer length / thread length overrides and the aerospace joint-grip check. */
class CustomLengthTest {
    private val sixteenth = 25.4 / 16

    @Test
    fun threadsInJointWhenGripShorterThanStack() {
        // L = 1", b = 0.5", no runout → grip 0.5"; a 0.6" stack puts 0.1" of thread in the joint
        val g = JointGrip.check(UnitSystem.INCH, 25.4, 12.7, 0.0, 0.6 * 25.4)
        assertEquals(GripStatus.THREADS_IN_JOINT, g.status)
        assertEquals(0.1 * 25.4, g.threadsInJointMm, 1e-9)
        // Suggested grip rounds the stack up to the next 1/16": 0.6" → 10/16" = 0.625"
        assertEquals(10 * sixteenth + 12.7, g.suggestedLengthMm, 1e-9)
    }

    @Test
    fun runoutThreadsCountAsThreadInTheJoint() {
        // 1/4-28: 2P = 1.814 mm. L − b = 23.8 mm covers a 22.9 mm stack, but the runout does not
        val twoP = 2 * 25.4 / 28
        val g = JointGrip.check(UnitSystem.INCH, 36.5125, 12.7, twoP, 22.86)
        assertEquals(GripStatus.THREADS_IN_JOINT, g.status)
        assertEquals(36.5125 - 12.7 - twoP, g.gripMinMm, 1e-9)
        assertEquals(15 * sixteenth + 12.7 + twoP, g.suggestedLengthMm, 1e-9)
    }

    @Test
    fun suggestedLengthPassesTheCheck() {
        for (stack in listOf(3.0, 7.9, 12.7, 19.05, 31.0)) {
            for (unit in UnitSystem.values()) {
                val first = JointGrip.check(unit, 20.0, 10.0, 2.5, stack)
                val again = JointGrip.check(unit, first.suggestedLengthMm, 10.0, 2.5, stack)
                assertEquals("$unit stack $stack", GripStatus.OK, again.status)
                assertTrue(again.excessMm < JointGrip.stepMm(unit) + 1e-9)
            }
        }
    }

    @Test
    fun exactFitAndExcessGrip() {
        assertEquals(GripStatus.OK, JointGrip.check(UnitSystem.METRIC, 40.0, 15.0, 0.0, 25.0).status)
        val g = JointGrip.check(UnitSystem.METRIC, 40.0, 15.0, 0.0, 20.0)
        assertEquals(GripStatus.EXCESS_GRIP, g.status)
        assertEquals(5.0, g.excessMm, 1e-9)
    }

    @Test
    fun gripCheckOnlyForThreadedAerospaceBolts() {
        assertTrue(JointGrip.appliesTo(FastenerType.NAS_CLOSE_TOL_BOLT))
        assertTrue(JointGrip.appliesTo(FastenerType.TWELVE_POINT))
        assertFalse(JointGrip.appliesTo(FastenerType.HEX_BOLT))
        assertFalse(JointGrip.appliesTo(FastenerType.AERO_LOCK_NUT))
        assertFalse(JointGrip.appliesTo(FastenerType.SOLID_RIVET))
    }

    private fun gate(type: FastenerType, unit: UnitSystem, length: Double, b: Double?): List<PokaYokeIssue> {
        val idx = FastenerCatalog.sizeIndexFor(unit, if (unit == UnitSystem.INCH) 6.35 else 10.0)
        val pitch = if (unit == UnitSystem.METRIC) FastenerCatalog.METRIC_SIZES[idx].pitches.first() else FastenerCatalog.INCH_SIZES[idx].tpis.first().toDouble()
        val std = PokaYokeEngine.defaultStandard(type, unit)
        return PokaYokeEngine.check(type, unit, std, idx, pitch, FastenerCatalog.diameterMm(unit, idx), length,
            SelectionFilter.threadClasses(unit, type, std).first(), PropertyClass.CLASS_10_9, FastenerCatalog.MATERIALS["10B21"]!!,
            CoatingType.GEOMET, 100, 0.0, 0.0, 100.0, false, 50.0, null, threadLengthMm = b)
    }

    @Test
    fun aerospaceBoltAsksForDrawingThreadLength() {
        val title = "Aerospace thread length not from the drawing"
        assertTrue(gate(FastenerType.NAS_CLOSE_TOL_BOLT, UnitSystem.INCH, 25.4, null).any { it.title == title })
        assertFalse(gate(FastenerType.NAS_CLOSE_TOL_BOLT, UnitSystem.INCH, 25.4, 12.0).any { it.title == title })
        assertFalse(gate(FastenerType.HEX_BOLT, UnitSystem.METRIC, 60.0, null).any { it.title == title })
    }

    @Test
    fun threadLongerThanPartIsFlagged() {
        assertTrue(gate(FastenerType.HEX_BOLT, UnitSystem.METRIC, 40.0, 45.0).any { it.title == "Thread length longer than the part" })
        assertFalse(gate(FastenerType.HEX_BOLT, UnitSystem.METRIC, 40.0, 20.0).any { it.title == "Thread length longer than the part" })
    }

    @Test
    fun customLengthAndThreadLengthReachTheCalculation() {
        val idx = FastenerCatalog.sizeIndexFor(UnitSystem.METRIC, 10.0)
        fun run(b: Double?) = CalculationEngine.calculate(
            FastenerType.HEX_BOLT, UnitSystem.METRIC, PokaYokeEngine.defaultStandard(FastenerType.HEX_BOLT, UnitSystem.METRIC), idx,
            1.5, 47.5, "6g", FastenerCatalog.MATERIALS["10B21"]!!, CoatingType.GEOMET, 1000,
            propertyClass = PropertyClass.CLASS_10_9, drawing = DrawingGeometry(threadLengthMm = b)
        )
        val standard = run(null)
        assertEquals(PokaYokeEngine.threadLengthB(FastenerType.HEX_BOLT, UnitSystem.METRIC, 10.0, 47.5), standard.threadLengthMm, 1e-9)
        val custom = run(18.0)
        assertEquals(18.0, custom.threadLengthMm, 1e-9)
    }
}
