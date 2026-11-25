package com.aistudio.aerofast.trmst

import com.aistudio.aerofast.trmst.data.QualityEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QualityEngineTest {

    // VDI 2230 Part 1 Table A1, shank bolt, µG = µK = 0.12, ν = 0.9: M10 FM = 29.6 / 43.4 / 50.8 kN, MA = 51 / 75 / 87 N·m
    @Test fun torqueTension_matchesVdi2230TableA1() {
        for ((rp, fm, ma) in listOf(Triple(640.0, 29.6, 51.0), Triple(940.0, 43.4, 75.0), Triple(1100.0, 50.8, 87.0))) {
            val t = QualityEngine.torqueTension(10.0, 1.5, rp, 0.12, 0.12, dwMm = 16.0, dhMm = 11.0)!!
            assertEquals("FM for Rp0.2 $rp", fm, t.preloadKn, 0.25)
            assertEquals("MA for Rp0.2 $rp", ma, t.torqueNm, ma * 0.04)
        }
    }

    @Test fun torqueTension_sharesAddUp_andKIsTypical() {
        val t = QualityEngine.torqueTension(12.0, 1.75, 940.0, 0.12, 0.12, 16.63, 13.5)!!
        assertEquals(100.0, t.threadFrictionPct + t.headFrictionPct + t.pitchPct, 1e-9)
        assertTrue("K ${t.nutFactorK}", t.nutFactorK in 0.15..0.22)
        assertTrue(t.headFrictionPct > t.pitchPct)
    }

    @Test fun torqueTension_rejectsBadGeometry() {
        assertNull(QualityEngine.torqueTension(10.0, 1.5, 940.0, 0.12, 0.12, dwMm = 10.0, dhMm = 11.0))
        assertNull(QualityEngine.torqueTension(10.0, 1.5, 940.0, 0.12, 0.12, 16.0, 11.0, utilisation = 1.2))
    }

    @Test fun capability_knownData() {
        // mean 10, overall s = 0.1414, MR̄ = 0.18 → σwithin = 0.1596
        val v = listOf(9.8, 10.0, 10.2, 10.0, 9.9, 10.1)
        val c = QualityEngine.capability(v, lsl = 9.4, usl = 10.6)!!
        assertEquals(10.0, c.mean, 1e-9)
        assertEquals(0.141421, c.sigmaOverall, 1e-5)
        assertEquals(0.18 / 1.128, c.sigmaWithin, 1e-9)
        assertEquals(1.2 / (6 * 0.18 / 1.128), c.cp!!, 1e-9)
        assertEquals(c.cp!!, c.cpk!!, 1e-9) // centred
        assertEquals(1.2 / (6 * 0.141421), c.pp!!, 1e-4)
        assertEquals(0, c.outOfSpecCount)
    }

    @Test fun capability_offCentreAndOneSided() {
        val v = listOf(10.3, 10.4, 10.35, 10.45, 10.4)
        val two = QualityEngine.capability(v, 9.4, 10.6)!!
        assertTrue(two.cpk!! < two.cp!!)
        val upperOnly = QualityEngine.capability(v, null, 10.6)!!
        assertNull(upperOnly.cp)
        assertNotNull(upperOnly.cpk)
        assertEquals(two.cpk!!, upperOnly.cpk!!, 1e-9) // the upper limit is the nearer one
        assertNull(QualityEngine.capability(listOf(1.0), 0.0, 2.0))
    }

    @Test fun normal_andSigmaLevel() {
        assertEquals(0.5, QualityEngine.normalCdf(0.0), 1e-7)
        assertEquals(0.841345, QualityEngine.normalCdf(1.0), 1e-6)
        assertEquals(1.959964, QualityEngine.normalInv(0.975), 1e-5)
        assertEquals(6.0, QualityEngine.sigmaLevel(3.4)!!, 0.01)
        assertEquals(4.0, QualityEngine.sigmaLevel(6210.0)!!, 0.01)
        assertEquals(3.0, QualityEngine.sigmaLevel(66807.0)!!, 0.01)
        assertEquals(500.0, QualityEngine.dpmo(5.0, 1000.0, 10.0)!!, 1e-9)
    }

    @Test fun oee_isProductOfThreeFactors() {
        // 480 min planned, 60 down, 1 s ideal cycle, 20 000 made, 19 600 good
        val o = QualityEngine.oee(480.0, 60.0, 1.0, 20000.0, 19600.0)!!
        assertEquals(420.0 / 480.0, o.availability, 1e-9)
        assertEquals(20000.0 / 60.0 / 420.0, o.performance, 1e-9)
        assertEquals(0.98, o.quality, 1e-9)
        assertEquals(o.availability * o.performance * o.quality, o.oee, 1e-12)
        assertNull(QualityEngine.oee(480.0, 60.0, 1.0, 100.0, 120.0))
    }

    @Test fun isoTables() {
        assertEquals(11.0, QualityEngine.clearanceHoleMedium(10.0)!!, 0.0)
        assertEquals(13.5, QualityEngine.clearanceHoleMedium(12.0)!!, 0.0)
        // ISO 4014 M10: s 16 h13 → −0.27, k 6.4 js14 → ±0.18, l 60 js15 → ±0.6
        assertEquals(0.27, QualityEngine.itGrade(13, 16.0)!!, 0.0)
        assertEquals(0.36, QualityEngine.itGrade(14, 6.4)!!, 0.0)
        assertEquals(1.20, QualityEngine.itGrade(15, 60.0)!!, 0.0)
        assertEquals(listOf(9.98, 10.02, -0.5), QualityEngine.parseValues("9.98, 10.02\n−0.5 abc"))
    }
}
