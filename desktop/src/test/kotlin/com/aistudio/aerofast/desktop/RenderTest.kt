package com.aistudio.aerofast.desktop

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.aistudio.aerofast.trmst.data.PropertyClass
import org.jetbrains.skia.EncodedImageFormat
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Renders each tab off-screen; set RENDER_DIR to keep the PNGs. Also proves the window composes without a display. */
class RenderTest {
    private fun shot(name: String, tab: Int, start: PartInputs = PartInputs()) {
        val scene = ImageComposeScene(1360, 900, Density(1f)) { AeroFastDesktop(DesktopPlatform { null }, tab, start) }
        scene.render(0)
        val img = scene.render(1_000_000_000L)
        val png = img.encodeToData(EncodedImageFormat.PNG)!!.bytes
        scene.close()
        assertTrue(png.size > 10_000)
        System.getenv("RENDER_DIR")?.let { File(it).apply { mkdirs() }.resolve("$name.png").writeBytes(png) }
    }

    @Test fun results() = shot("1_results", 0)
    @Test fun tooling() = shot("2_tooling", 1)
    @Test fun heat718() = shot("3_heat_718", 2, PartInputs(materialKey = "INCONEL718", propertyClass = PropertyClass.AERO_1380, lengthText = "40"))
    @Test fun quality() = shot("4_quality", 3)
}
