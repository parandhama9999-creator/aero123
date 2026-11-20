package com.aistudio.aerofast.trmst

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTests {

  @Test
  fun `test calculation engine turning blank for M10`() {
    val material = com.aistudio.aerofast.trmst.data.FastenerCatalog.MATERIALS["10B21"]!!
    val result = com.aistudio.aerofast.trmst.data.CalculationEngine.calculate(
      fastenerType = com.aistudio.aerofast.trmst.data.FastenerType.HEX_BOLT,
      unitSystem = com.aistudio.aerofast.trmst.data.UnitSystem.METRIC,
      standard = com.aistudio.aerofast.trmst.data.ProductStandard.ISO_4014,
      nominalSizeIndex = com.aistudio.aerofast.trmst.data.FastenerCatalog.METRIC_SIZES.indexOfFirst { it.d == 10.0 },
      pitchValue = 1.5,
      lengthMm = 60.0,
      threadClass = "6g",
      material = material,
      coating = com.aistudio.aerofast.trmst.data.CoatingType.ZN_NI,
      batchQty = 10000,
      propertyClass = com.aistudio.aerofast.trmst.data.PropertyClass.CLASS_10_9
    )
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
    val chain = com.aistudio.aerofast.trmst.data.GeminiAiService.MODEL_FALLBACK_CHAIN
    assertEquals("gemini-3.5-flash", chain[0])
    assertEquals("gemini-3.8-flash", chain[1])
    assertEquals("gemini-3.1-flash-lite-preview", chain[2])
  }

  @Test
  fun `test detected dimension data model`() {
    val dim = com.aistudio.aerofast.trmst.data.DetectedDimension("Body Diameter", "6.20 mm", "NOM")
    assertEquals("Body Diameter", dim.feature)
    assertEquals("6.20 mm", dim.nominal)
    assertEquals("NOM", dim.tolerance)
  }

  @Test
  fun `test multi-language localization dictionary`() {
    val hiTitle = com.aistudio.aerofast.trmst.util.AppLocalization.getTabTitle(1, com.aistudio.aerofast.trmst.util.AppLanguage.HI)
    assertEquals("1. आकार निर्धारण", hiTitle)

    val teTitle = com.aistudio.aerofast.trmst.util.AppLocalization.getTabTitle(1, com.aistudio.aerofast.trmst.util.AppLanguage.TE)
    assertEquals("1. పరిమాణం", teTitle)

    val esTitle = com.aistudio.aerofast.trmst.util.AppLocalization.getTabTitle(5, com.aistudio.aerofast.trmst.util.AppLanguage.ES)
    assertEquals("5. Co-Piloto IA", esTitle)

    val deTitle = com.aistudio.aerofast.trmst.util.AppLocalization.getTabTitle(4, com.aistudio.aerofast.trmst.util.AppLanguage.DE)
    assertEquals("4. KI-Scanner", deTitle)

    val jaTitle = com.aistudio.aerofast.trmst.util.AppLocalization.getTabTitle(6, com.aistudio.aerofast.trmst.util.AppLanguage.JA)
    assertEquals("6. 8D NPD エンジン", jaTitle)

    val zhTitle = com.aistudio.aerofast.trmst.util.AppLocalization.getTabTitle(5, com.aistudio.aerofast.trmst.util.AppLanguage.ZH)
    assertEquals("5. AI副驾驶", zhTitle)

    val ruTitle = com.aistudio.aerofast.trmst.util.AppLocalization.getTabTitle(3, com.aistudio.aerofast.trmst.util.AppLanguage.RU)
    assertEquals("3. ТО и Ковка", ruTitle)

    val frTitle = com.aistudio.aerofast.trmst.util.AppLocalization.getTabTitle(6, com.aistudio.aerofast.trmst.util.AppLanguage.FR)
    assertEquals("6. Moteur 8D NPD", frTitle)

    assertEquals("Tab 8", com.aistudio.aerofast.trmst.util.AppLocalization.getTabTitle(8, com.aistudio.aerofast.trmst.util.AppLanguage.EN))
  }
}
