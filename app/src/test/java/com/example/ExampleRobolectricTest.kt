package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AeroFast Pro", appName)
  }

  @Test
  fun `test calculation engine turning blank for M10`() {
    val material = com.example.data.FastenerCatalog.MATERIALS["10B21"]!!
    val result = com.example.data.CalculationEngine.calculate(
      fastenerType = com.example.data.FastenerType.HEX_BOLT,
      unitSystem = com.example.data.UnitSystem.METRIC,
      standard = com.example.data.ProductStandard.ISO_4014,
      nominalSizeIndex = 11, // M10
      pitchValue = 1.5,
      lengthMm = 60.0,
      threadClass = "6g",
      material = material,
      coating = com.example.data.CoatingType.ZN_NI,
      batchQty = 10000,
      propertyClass = com.example.data.PropertyClass.CLASS_10_9
    )
    // Nominal diameter is 10.0, pitch is 1.5, blank target should be around 8.923mm
    assertEquals(10.0, result.nominalDiaMm, 0.001)
    assertEquals(1.5, result.pitchMm, 0.001)
    assertEquals(8.936, result.blankTargetDia, 0.01)
    assertEquals(true, result.hasHydrogenEmbrittlementRisk)
    assertEquals(1040.0, result.postForgingTargetUtsMpa, 0.1)
    assertEquals(940.0, result.yieldStrengthMpa, 0.1)
    assertEquals(true, result.heatTreatmentSpec.hydrogenBakingRequired)
    assertEquals("32–39 HRC (Class 10.9)", result.heatTreatmentSpec.targetHardnessHrc)
  }

  @Test
  fun `test multi-model fallback chain configuration`() {
    val chain = com.example.data.GeminiAiService.MODEL_FALLBACK_CHAIN
    assertEquals("gemini-3.5-flash", chain[0])
    assertEquals("gemini-3.8-flash", chain[1])
    assertEquals("gemini-3.1-flash-lite-preview", chain[2])
  }

  @Test
  fun `test detected dimension data model`() {
    val dim = com.example.data.DetectedDimension("Body Diameter", "6.20 mm", "NOM")
    assertEquals("Body Diameter", dim.feature)
    assertEquals("6.20 mm", dim.nominal)
    assertEquals("NOM", dim.tolerance)
  }

  @Test
  fun `test multi-language localization dictionary`() {
    val hiTitle = com.example.util.AppLocalization.getTabTitle(1, com.example.util.AppLanguage.HI)
    assertEquals("1. आकार निर्धारण", hiTitle)

    val teTitle = com.example.util.AppLocalization.getTabTitle(1, com.example.util.AppLanguage.TE)
    assertEquals("1. పరిమాణం", teTitle)

    val esTitle = com.example.util.AppLocalization.getTabTitle(6, com.example.util.AppLanguage.ES)
    assertEquals("6. Co-Piloto IA", esTitle)

    val deTitle = com.example.util.AppLocalization.getTabTitle(3, com.example.util.AppLanguage.DE)
    assertEquals("3. KI-Scanner", deTitle)

    val jaTitle = com.example.util.AppLocalization.getTabTitle(5, com.example.util.AppLanguage.JA)
    assertEquals("5. 見積もり", jaTitle)

    val zhTitle = com.example.util.AppLocalization.getTabTitle(4, com.example.util.AppLanguage.ZH)
    assertEquals("4. 发那科G代码", zhTitle)

    val ruTitle = com.example.util.AppLocalization.getTabTitle(2, com.example.util.AppLanguage.RU)
    assertEquals("2. ТО и Ковка", ruTitle)

    val frTitle = com.example.util.AppLocalization.getTabTitle(7, com.example.util.AppLanguage.FR)
    assertEquals("7. Moteur 8D NPD", frTitle)
  }
}
