package com.example

import com.example.data.*
import org.junit.Assert.assertEquals
import org.junit.Test
class BoltForgingTest {
    @Test fun m10HexMatchesHandCalc() {
        val head = HeadDimensions(16.0, 6.4, 14.63, 8.4, "ISO 4014", true)
        val f = BoltForging.compute(FastenerType.HEX_BOLT, UnitSystem.METRIC, 10.0, 1.5, 60.0, 8.962, 9.85, head, 7.85,
            null, null, null, null, null, null)
        assertEquals(26.0, f.threadLengthMm, 1e-9)
        assertEquals(1.66, f.coneLengthMm, 0.01)
        assertEquals(23.04, f.stockToExtrudeMm, 0.01)
        assertEquals(55.38, f.lengthUnderHeadBeforeMm, 0.01)
        assertEquals(1052.0, f.cheeseVolumeMm3!!, 1.0)
        assertEquals(32.42, f.headStockMm!!, 0.01)
        assertEquals(87.80, f.cutOffLengthMm!!, 0.02)
        // slug volume = finished + cheese = cut-off × shank area
        assertEquals(f.cutOffLengthMm!! * Math.PI / 4 * 9.85 * 9.85, f.finishedVolumeMm3!! + f.cheeseVolumeMm3!!, 0.5)
    }
}
