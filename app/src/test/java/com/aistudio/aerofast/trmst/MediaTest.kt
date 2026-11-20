package com.aistudio.aerofast.trmst

import com.aistudio.aerofast.trmst.data.*
import com.aistudio.aerofast.trmst.util.DrawingExporter
import com.aistudio.aerofast.trmst.util.Yuv
import com.aistudio.aerofast.trmst.ui.screens.drawHeadingFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private val m10 = FastenerCatalog.sizeIndexFor(UnitSystem.METRIC, 10.0)
private fun shcs() = CalculationEngine.calculate(FastenerType.SHCS, UnitSystem.METRIC, ProductStandard.ISO_4762, m10, 1.5, 50.0, "6g",
    FastenerCatalog.MATERIALS.getValue("4140"), CoatingType.ZN_NI, 10000, propertyClass = PropertyClass.CLASS_12_9)

/** The animation is keyed on the app's own stations. */
class HeadingAnimationTest {
    private val plan = shcs().headingPlan!!

    @Test fun startsOnTheSlugAndEndsOnTheFinishedPart() {
        val first = HeadingAnimation.frameAt(plan, 0.0)
        assertEquals(0.0, first.morph, 0.0)
        assertEquals(plan.stations[0].heightMm, HeadingAnimation.outline(plan, first).last().first, 1e-6)
        val last = HeadingAnimation.frameAt(plan, HeadingAnimation.durationSec(plan))
        assertEquals(plan.stations.lastIndex, last.toIdx)
        assertEquals(1.0, last.morph, 0.0)
        assertEquals(plan.stations.last().heightMm, HeadingAnimation.outline(plan, last).last().first, 1e-6)
        assertEquals(HeadingAnimation.blowCount(plan), last.blowCount)
    }

    @Test fun punchTouchesWhileFormingAndLiftsBetweenBlows() {
        val mid = HeadingAnimation.frameAt(plan, HeadingAnimation.HOLD_START_SEC + HeadingAnimation.SEC_PER_BLOW * 0.5)
        assertEquals(0.0, mid.punchGapMm, 1e-9)
        assertTrue(mid.morph > 0.0 && mid.morph < 1.0)
        assertTrue(HeadingAnimation.frameAt(plan, HeadingAnimation.HOLD_START_SEC + 0.01).punchGapMm > 0.0)
    }

    @Test fun layersStayInOrderByVolume() {
        plan.stations.forEach { st ->
            val pts = HeadingAnimation.byVolume(st)
            assertTrue(pts.zipWithNext().all { (a, b) -> b.first >= a.first - 1e-9 })
            assertEquals(st.heightMm, pts.last().first, 1e-6)
        }
    }

    @Test fun yuvBt601LimitedRange() {
        assertEquals(235, Yuv.y(255, 255, 255)); assertEquals(128, Yuv.u(255, 255, 255)); assertEquals(128, Yuv.v(255, 255, 255))
        assertEquals(16, Yuv.y(0, 0, 0))
        assertTrue(Yuv.v(255, 0, 0) > 200)   // red pushes V up
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MediaAiParseTest {
    @Test fun modelsAreSortedIntoImageAndVideoByWhatTheyCanDo() {
        val json = """{"models":[
            {"name":"models/gemini-9-flash","supportedGenerationMethods":["generateContent"]},
            {"name":"models/gemini-9-flash-image","displayName":"Gemini 9 Flash Image","supportedGenerationMethods":["generateContent","countTokens"]},
            {"name":"models/imagen-9","supportedGenerationMethods":["predict"]},
            {"name":"models/veo-9-generate","supportedGenerationMethods":["predictLongRunning"]}]}"""
        val m = MediaAi.classify(json)
        assertEquals(listOf("gemini-9-flash-image"), m.image.map { it.id })
        assertEquals(listOf("veo-9-generate"), m.video.map { it.id })
    }

    @Test fun imageComesFromInlineDataEitherSpelling() {
        val b64 = android.util.Base64.encodeToString(byteArrayOf(1, 2, 3), android.util.Base64.NO_WRAP)
        val camel = MediaAi.parseImage("""{"candidates":[{"content":{"parts":[{"text":"here"},{"inlineData":{"mimeType":"image/png","data":"$b64"}}]}}]}""")!!
        assertEquals(3, camel.bytes.size); assertEquals("image/png", camel.mimeType); assertEquals("here", camel.note)
        assertNotNull(MediaAi.parseImage("""{"candidates":[{"content":{"parts":[{"inline_data":{"mime_type":"image/jpeg","data":"$b64"}}]}}]}"""))
        assertEquals(null, MediaAi.parseImage("""{"candidates":[{"content":{"parts":[{"text":"no image"}]}}]}"""))
    }

    @Test fun videoOperationStates() {
        assertTrue(MediaAi.parseOperation("""{"name":"models/veo/operations/1","done":false}""") is VideoJob.Running)
        val ready = MediaAi.parseOperation("""{"name":"op","done":true,"response":{"generateVideoResponse":{"generatedSamples":[{"video":{"uri":"https://x/v.mp4"}}]}}}""")
        assertEquals("https://x/v.mp4", (ready as VideoJob.Ready).uri)
        assertTrue(MediaAi.parseOperation("""{"name":"op","done":true,"error":{"message":"quota"}}""") is VideoJob.Failed)
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class DrawingExportTest {
    private val out = java.io.File("/tmp/claude-0/-home-user/14769dbc-781e-52c3-9693-6d52aa3bc7ce/scratchpad/render")

    @Test fun progressionPartAndToolImagesRender() {
        val r = shcs()
        val prog = DrawingExporter.progressionImage(r)!!
        assertEquals(2400, prog.width)
        val part = DrawingExporter.partImage(r)!!
        val st = ToolingDesign.build(r)!!.stations.last()
        val tool = DrawingExporter.toolSectionImage(r, st)
        if (out.isDirectory) {
            listOf("export_progression.png" to prog, "export_part.png" to part, "export_tool_st4.png" to tool).forEach { (n, b) ->
                java.io.File(out, n).outputStream().use { b.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            }
            val plan = r.headingPlan!!
            listOf(0.5, 2.2, 3.4, 6.0, 11.0).forEach { t ->
                val frame = DrawingExporter.renderBitmap(720, 960, androidx.compose.ui.graphics.Color.Black) {
                    drawHeadingFrame(plan, HeadingAnimation.frameAt(plan, t), 30f, "M10 SHCS")
                }
                java.io.File(out, "anim_t$t.png").outputStream().use { frame.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
        // The drawing is not blank: the part colour appears
        assertTrue((0 until prog.height step 7).any { y -> (0 until prog.width step 7).any { x -> prog.getPixel(x, y) != prog.getPixel(0, 0) } })
    }

    @Test fun pngFilesOnePerStationPlusProgressionAndPart() {
        val r = shcs()
        val files = DrawingExporter.pngFiles(RuntimeEnvironment.getApplication(), r, MachineSetup())
        assertEquals(2 + ToolingDesign.build(r)!!.stations.size, files.size)
        assertTrue(files.all { it.length() > 1000 })
    }

    @Test fun pdfPackIsWritten() {
        // Robolectric has no native PdfDocument ("document is closed" on the first page): skipped here, checked on a phone
        val f = try { DrawingExporter.pdfFile(RuntimeEnvironment.getApplication(), shcs(), MachineSetup()) }
            catch (e: IllegalStateException) { org.junit.Assume.assumeTrue("PdfDocument not available under Robolectric", false); return }
        assertTrue(f.exists())
    }
}
