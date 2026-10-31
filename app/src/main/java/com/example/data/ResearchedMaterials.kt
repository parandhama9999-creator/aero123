package com.example.data

/**
 * Automotive and aerospace grades researched from producer / supplier datasheets (Oct 2026).
 *
 * Each grade is built by copying its closest existing catalogue grade ([MaterialInfo.estimatedFrom]) and then
 * overriding only the values that were found in a source ([MaterialInfo.dataSources]). Everything not
 * overridden — cutting speed, feed, flow-stress K and n, price, tooling, heading speed — is the base grade's
 * value and is shown in the app as "estimated from <grade>".
 *
 * Finished strengths for property-class bolt steels are the ISO 898-1 minima for that class.
 */
object ResearchedMaterials {

    private const val ISO_898 = "ISO 898-1 property-class minima (Rm, Rp0.2, A, Z)"

    private data class IsoClass(val rm: Double, val rp: Double, val a: Double, val z: Double, val hrc: String)
    private val C8_8 = IsoClass(800.0, 640.0, 12.0, 52.0, "22–32 HRC (Class 8.8, d ≤ 16)")
    private val C10_9 = IsoClass(1040.0, 940.0, 9.0, 48.0, "32–39 HRC (Class 10.9)")

    /** Keeps the base grade's yield/tensile ratio when only tensile strength was sourced. */
    private fun scaledYield(base: MaterialInfo, rm: Double) =
        if (base.postForgingTargetUtsMpa > 0) rm * base.yieldStrengthMpa / base.postForgingTargetUtsMpa else rm * 0.8

    /** Annealed-wire strength was not found for these grades: carry the base grade's figure, labelled. */
    private fun wireFrom(base: MaterialInfo) =
        base.rawMaterialSourcingUtsMpa.substringBefore("(").trim() + " (Spheroidized Annealed Wire — Rm estimated from ${base.key})"

    private fun boltSteel(
        base: MaterialInfo, key: String, name: String, cls: IsoClass,
        aust: String, quench: String, temper: String, std: String, sources: List<String>,
        quenchNote: String? = null
    ): MaterialInfo = base.copy(
        key = key, name = name,
        uts = cls.rm, isAero = false, hasHighHardness = cls.rm >= 1000.0,
        heatTreatment = "Austenitize $aust -> $quench quench -> Temper $temper.",
        rawMaterialSourcingUtsMpa = wireFrom(base),
        postForgingTargetUtsMpa = cls.rm, yieldStrengthMpa = cls.rp,
        minElongationPct = cls.a, reductionOfAreaZPct = cls.z,
        heatTreatmentSpec = base.heatTreatmentSpec.copy(
            austenitizingOrSolutionTempC = aust,
            soakingTimeMinutes = "Per section size (not stated in source)",
            quenchMedium = quench,
            temperingOrAgingTempC = temper,
            temperingOrAgingTimeHours = "Per section size (not stated in source)",
            targetHardnessHrc = cls.hrc,
            hydrogenBakingRequired = cls.rm >= 1000.0,
            hydrogenBakingCycle = if (cls.rm >= 1000.0) "190°C – 220°C for 8 to 24 hrs within 4 hrs of plating (ISO 4042)"
            else "Not required below 1000 MPa",
            standardReference = std,
            quenchDetails = quenchNote ?: base.heatTreatmentSpec.quenchDetails,
            targetUtsMpa = "${cls.rm.toInt()} MPa",
            isAging = false
        ),
        estimatedFrom = base.key,
        dataSources = listOf(ISO_898) + sources
    )

    fun extend(base: Map<String, MaterialInfo>): Map<String, MaterialInfo> {
        fun b(k: String) = base.getValue(k)

        val grades = listOf(
            // ---------------- Automotive: EN 10263-4 boron / Cr-Mo / Cr cold-heading steels ----------------
            boltSteel(b("10B21"), "20MNB4", "20MnB4 / 1.5525 Mn-B Boron Steel (EN 10263-4)", C8_8,
                aust = "900°C – 920°C (Lucefin; Total Materia lists 880 ± 5°C)", quench = "Water or polymer",
                temper = "450°C – 650°C", std = "EN 10263-4 / ISO 898-1 Class 8.8",
                sources = listOf("Lucefin 20MnB4 technical card", "Total Materia 20MnB4")),
            boltSteel(b("10B21"), "23MNB4", "23MnB4 / 1.5535 Mn-B Boron Steel (EN 10263-4)", C10_9,
                aust = "880°C ± 5°C", quench = "Water",
                temper = "420°C – 630°C (indicative, producer sheet)", std = "EN 10263-4 / ISO 898-1 Class 10.9",
                sources = listOf("Total Materia 23MnB4", "Lucefin 23MnB4 technical card"),
                quenchNote = "Avoid long holds at 200 – 400°C (temper embrittlement) — Lucefin"),
            boltSteel(b("AISI_51B37"), "32CRB4", "32CrB4 / 1.7076 Cr-B Boron Steel (EN 10263-4)", C10_9,
                aust = "860°C – 870°C", quench = "Oil, polymer or water",
                temper = "520°C – 650°C", std = "EN 10263-4 / ISO 898-1 Class 10.9",
                sources = listOf("Total Materia 32CrB4", "Lucefin 32CrB4 technical card")),
            boltSteel(b("AISI_51B37"), "36CRB4", "36CrB4 / 1.7077 Cr-B Boron Steel (EN 10263-4)", C10_9,
                aust = "860°C – 870°C (estimated from 32CrB4)", quench = "Oil, polymer or water (estimated from 32CrB4)",
                temper = "520°C – 650°C (estimated from 32CrB4)", std = "EN 10263-4 / ISO 898-1 Class 10.9 / 12.9",
                sources = listOf("No reliable 36CrB4 heat-treat source found — cycle estimated from 32CrB4")),
            boltSteel(b("4135"), "34CRMO4", "34CrMo4 / 1.7220 Cr-Mo Steel (EN 10263-4)", C10_9,
                aust = "830°C – 870°C", quench = "Oil, polymer or water",
                temper = "540°C – 680°C", std = "EN 10263-4 / EN ISO 683-2 / ISO 898-1 Class 10.9 / 12.9",
                sources = listOf("Lucefin 34CrMo4 technical card", "Ovako 34CrMo4 (Ac1 737°C, Ac3 808°C)")),
            boltSteel(b("4140"), "41CR4", "41Cr4 / 1.7035 / SAE 5140 Cr Steel (EN 10263-4)", C10_9,
                aust = "820°C – 860°C (Ovako 830 – 860°C)", quench = "Oil, polymer or water",
                temper = "540°C – 680°C", std = "EN 10263-4 / EN ISO 683-2 / ISO 898-1 Class 10.9",
                sources = listOf("Lucefin 41Cr4 technical card", "Ovako 41Cr4")),
            boltSteel(b("4135"), "SCM435", "SCM435 Cr-Mo Steel (JIS G4053)", C10_9,
                aust = "≈ 850°C (lab tests 850 – 900°C)", quench = "Oil",
                temper = "500°C – 600°C", std = "JIS G4053 / ISO 898-1 Class 10.9 / 12.9",
                sources = listOf("Vendor process note (rough)", "Niigata Prefecture lab tests (800 – 950°C oil, 600°C temper)",
                    "Kyushu Univ. HYDROGENIUS data (900°C oil, 600°C temper)")),

            // ---------------- Automotive: case-hardening grades (tapping / thread-forming screws) ----------------
            b("AISI_4130").copy(
                key = "18CRMO4", name = "18CrMo4 / 1.7243 Case-Hardening Steel (EN 10084 / EN 10263-3)",
                isAero = false, hasHighHardness = true,
                heatTreatment = "Carburize 880 – 980°C -> harden 780 – 820°C oil -> temper 150 – 200°C.",
                rawMaterialSourcingUtsMpa = wireFrom(b("AISI_4130")),
                heatTreatmentSpec = b("AISI_4130").heatTreatmentSpec.copy(
                    austenitizingOrSolutionTempC = "Carburize 880°C – 980°C, then harden 780°C – 820°C (verify — source table garbled)",
                    soakingTimeMinutes = "Set by case depth (0.5 – 1.0 mm typical)",
                    quenchMedium = "Oil, polymer or martempering",
                    temperingOrAgingTempC = "150°C – 200°C",
                    temperingOrAgingTimeHours = "Per section size (not stated in source)",
                    targetHardnessHrc = "Carburized case 0.5 – 1.0 mm; surface hardness per drawing",
                    hydrogenBakingRequired = true,
                    standardReference = "EN 10084 / EN 10263-3",
                    atmosphere = "Carburizing gas (case hardening)",
                    isAging = false
                ),
                estimatedFrom = "AISI_4130",
                dataSources = listOf("Lucefin 18CrMo4 technical card (process table garbled — verify)",
                    "Ovako case-hardening steels (case depth 0.5 – 1.0 mm)")
            ),
            b("1018").copy(
                key = "1022", name = "AISI 1022 / SWCH22A Case-Hardening Steel (tapping & thread-forming screws)",
                isAero = false, hasHighHardness = true,
                heatTreatment = "Carburize ≈ 925°C or carbonitride ≈ 845°C -> quench -> temper 410 – 538°C, 1 h.",
                rawMaterialSourcingUtsMpa = b("1018").rawMaterialSourcingUtsMpa.substringBefore("(").trim() +
                    " (Annealed Wire — Rm estimated from 1018)",
                heatTreatmentSpec = b("1018").heatTreatmentSpec.copy(
                    austenitizingOrSolutionTempC = "Gas carburize ≈ 925°C (1700°F) or carbonitride ≈ 845°C (1550°F)",
                    soakingTimeMinutes = "Set by case depth (ISO 2702)",
                    quenchMedium = "Oil",
                    temperingOrAgingTempC = "410°C – 538°C (770°F – 1000°F)",
                    temperingOrAgingTimeHours = "≈ 1 hr",
                    targetHardnessHrc = "Case hardened per ISO 2702",
                    hydrogenBakingRequired = true,
                    standardReference = "ISO 2702 (heat-treated tapping screws)",
                    atmosphere = "Carburizing or carbonitriding gas",
                    isAging = false
                ),
                estimatedFrom = "1018",
                dataSources = listOf("US patent 6,213,884 (case-hardened self-tapping fasteners, 1022 wire)",
                    "DIN EN ISO 2702:2023 (heat-treated tapping screws)")
            ),

            // ---------------- Aerospace ----------------
            b("MP35N").copy(
                key = "MP159", name = "MP159 Multiphase Co-Ni (AMS 5842 / AMS 5843)",
                density = 8.33, uts = 1793.0, isAero = true, hasHighHardness = true,
                heatTreatment = "Solution 1038 – 1052°C WQ -> work-strengthen -> age 649 – 677°C 4 h.",
                rawMaterialSourcingUtsMpa = "Rm not published for headable stock (Solution Treated, AMS 5842)",
                postForgingTargetUtsMpa = 1793.0, yieldStrengthMpa = 1724.0, minElongationPct = 6.0,
                heatTreatmentSpec = b("MP35N").heatTreatmentSpec.copy(
                    austenitizingOrSolutionTempC = "Solution 1038°C – 1052°C (1900 – 1925°F), 4 – 8 h, WQ, then work-strengthen",
                    soakingTimeMinutes = "4 – 8 hrs",
                    quenchMedium = "Water",
                    temperingOrAgingTempC = "649°C – 677°C (1200 – 1250°F)",
                    temperingOrAgingTimeHours = "4 hrs",
                    targetHardnessHrc = "≥ 44 HRC (AMS 5843)",
                    standardReference = "AMS 5842 / AMS 5843",
                    targetUtsMpa = "1793 MPa (260 ksi) min",
                    isAging = true
                ),
                estimatedFrom = "MP35N",
                dataSources = listOf("Aircraft Materials MP159 (AMS 5841/5842/5843)", "UPMET MP159 datasheet",
                    "Alloys International MP159", "Density sources range 8.14 – 8.37 g/cm³")
            ),
            b("INCONEL718").copy(
                key = "INCONEL_X750", name = "Inconel X-750 / UNS N07750 (AMS 5667 / 5698 / 5699)",
                density = 8.28, uts = 1250.0, isAero = true, hasHighHardness = true,
                heatTreatment = "Solution 982°C AC -> age 843°C 24 h AC + 704°C 20 h AC.",
                rawMaterialSourcingUtsMpa = "Rm not published for headable stock (Solution Annealed)",
                postForgingTargetUtsMpa = 1250.0, yieldStrengthMpa = scaledYield(b("INCONEL718"), 1250.0),
                heatTreatmentSpec = b("INCONEL718").heatTreatmentSpec.copy(
                    austenitizingOrSolutionTempC = "Solution 982°C ± 14°C, AC or faster (1149°C 2 h for spring-temper / high-temp service)",
                    soakingTimeMinutes = "Commensurate with section thickness",
                    quenchMedium = "Air cool or faster",
                    temperingOrAgingTempC = "843°C 24 h AC, then 704°C 20 h AC",
                    temperingOrAgingTimeHours = "24 + 20 hrs",
                    targetHardnessHrc = "Per product spec (AMS 5667 / 5698 / 5699)",
                    standardReference = "AMS 5667 / AMS 5698 / AMS 5699",
                    targetUtsMpa = "1100 – 1380 MPa (1250 typical)",
                    isAging = true
                ),
                estimatedFrom = "INCONEL718",
                dataSources = listOf("Total Materia Inconel X-750 (solution 982°C; spring-temper triple cycle)",
                    "Alloys International X-750 (density 8.28, UTS 1250 MPa)", "Xometry X-750 (UTS 1100 – 1380 MPa)",
                    "Yield scaled from Inconel 718's yield/tensile ratio")
            ),
            b("TI_GR5").copy(
                key = "TI_BETA_C", name = "Beta-C Titanium Ti-3Al-8V-6Cr-4Mo-4Zr (AMS 4957 / 4958)",
                density = 4.82, uts = 1172.0, isAero = true, hasHighHardness = true,
                heatTreatment = "Solution 788 – 927°C -> age 482 – 566°C 6 – 12 h AC.",
                rawMaterialSourcingUtsMpa = "Rm not published for headable stock (Solution Treated Wire, AMS 4957)",
                postForgingTargetUtsMpa = 1172.0, yieldStrengthMpa = 1103.0, minElongationPct = 6.0, reductionOfAreaZPct = 15.0,
                heatTreatmentSpec = b("TI_GR5").heatTreatmentSpec.copy(
                    austenitizingOrSolutionTempC = "Solution 788°C – 927°C (1450 – 1700°F) ± 14°C",
                    soakingTimeMinutes = "Per section thickness",
                    temperingOrAgingTempC = "482°C – 566°C (900 – 1050°F) ± 6°C",
                    temperingOrAgingTimeHours = "6 – 12 hrs, air cool",
                    targetHardnessHrc = "Per AMS 4958",
                    standardReference = "AMS 4957 / AMS 4958",
                    targetUtsMpa = "1172 MPa (STA typical)",
                    isAging = true
                ),
                estimatedFrom = "TI_GR5",
                dataSources = listOf("Total Materia Ti-3Al-8V-6Cr-4Mo-4Zr (solution & aging windows)",
                    "Supplier table: STA 1172 MPa UTS, 1103 MPa yield, 6% A, 15% Z", "Nippon Steel titanium catalogue",
                    "Density 4.82 g/cm³ commonly cited — not verified")
            ),
            b("TI_GR23").copy(
                key = "TI_3AL25V", name = "Ti-3Al-2.5V Grade 9 (AMS 4943 – 4946)",
                density = 4.48, uts = 620.0, isAero = true, hasHighHardness = false,
                heatTreatment = "Annealed condition; optional ST 925°C + age 480°C.",
                rawMaterialSourcingUtsMpa = "Rm not published for headable stock (Annealed)",
                postForgingTargetUtsMpa = 620.0, yieldStrengthMpa = 483.0, minElongationPct = 15.0,
                heatTreatmentSpec = b("TI_GR23").heatTreatmentSpec.copy(
                    austenitizingOrSolutionTempC = "Anneal per AMS 4943 – 4946 / mill cert (not in public sources); β-transus 935°C",
                    soakingTimeMinutes = "Per spec",
                    quenchMedium = "Air cool",
                    temperingOrAgingTempC = "Optional: ST 925°C + age 480°C",
                    temperingOrAgingTimeHours = "Per spec",
                    targetHardnessHrc = "Per spec",
                    standardReference = "AMS 4943 – 4946 / ASTM B265 Gr 9",
                    targetUtsMpa = "620 MPa (90 ksi) min, annealed",
                    isAging = false
                ),
                estimatedFrom = "TI_GR23",
                dataSources = listOf("Ulbrich / Alloys International / Aircraft Materials Ti-3-2.5 (620 MPa, 483 MPa, 15%)",
                    "Aerospace Metals Ti-3Al-2.5V datasheets (alpha annealed; ST 925°C aged 480°C)",
                    "Density 4.48 g/cm³")
            ),
            b("AL2024").copy(
                key = "AL2117", name = "Al 2117-T4 Rivet Alloy (AD)",
                uts = 298.0, isAero = true, hasHighHardness = false,
                heatTreatment = "Solution heat treat + natural age (T4); driven as received.",
                rawMaterialSourcingUtsMpa = "Supplied T4 — driven as received (no anneal)",
                postForgingTargetUtsMpa = 298.0, yieldStrengthMpa = scaledYield(b("AL2024"), 298.0),
                heatTreatmentSpec = b("AL2024").heatTreatmentSpec.copy(
                    austenitizingOrSolutionTempC = "Solution heat treat — temperature per AMS / MIL spec (not found in public sources)",
                    temperingOrAgingTempC = "Natural age (T4)",
                    temperingOrAgingTimeHours = "Room temperature",
                    targetHardnessHrc = "Shear ≥ 179 MPa (26 ksi) — SAE AS7222",
                    standardReference = "SAE AS7222 / NASM5674",
                    targetUtsMpa = "≈ 298 MPa (derived: 26 ksi shear ÷ 0.6)",
                    isAging = false
                ),
                estimatedFrom = "AL2024",
                dataSources = listOf("SAE AS7222 (2117-T4 rivets, 26 ksi min shear)",
                    "Tensile derived from shear ÷ 0.6; density and yield estimated from 2024")
            ),
            b("AL7075").copy(
                key = "AL7050", name = "Al 7050-T73 Rivet / Fastener Alloy",
                density = 2.83, uts = 496.0, isAero = true, hasHighHardness = false,
                heatTreatment = "Solution 477°C -> age 121°C 6 – 8 h + 177°C 11.5 – 12.5 h (T73).",
                rawMaterialSourcingUtsMpa = "Supplied T73 — driven as received (no anneal)",
                postForgingTargetUtsMpa = 496.0, yieldStrengthMpa = 434.0,
                heatTreatmentSpec = b("AL7075").heatTreatmentSpec.copy(
                    austenitizingOrSolutionTempC = "Solution 477°C (890°F); 466 – 471°C allowed in some specs",
                    temperingOrAgingTempC = "121°C 6 – 8 h, then 177°C 11.5 – 12.5 h (T73)",
                    temperingOrAgingTimeHours = "≈ 18 – 21 hrs total",
                    targetHardnessHrc = "Rivet shear 283 MPa (41 ksi) — MS20470 E",
                    standardReference = "AMS 4341 / MS20470 (E)",
                    targetUtsMpa = "496 MPa (T73511 typical)",
                    isAging = true
                ),
                estimatedFrom = "AL7075",
                dataSources = listOf("Aerospace Metals 7050-T73511 datasheet (2.83 g/cm³, 496 / 434 MPa)",
                    "Total Materia / Wikipedia 7050 (solution 477°C, T73 two-step age)",
                    "Military-fasteners MS20470 7050 rivet (41 ksi shear)")
            ),
            b("AL6061").copy(
                key = "AL5056", name = "Al 5056-H32 Rivet Alloy (B)",
                uts = 276.0, isAero = true, hasHighHardness = false,
                heatTreatment = "Non-heat-treatable — strain hardened and stabilized (H32), used as fabricated.",
                rawMaterialSourcingUtsMpa = "Supplied H32 — used as fabricated (no anneal)",
                postForgingTargetUtsMpa = 276.0, yieldStrengthMpa = scaledYield(b("AL6061"), 276.0),
                heatTreatmentSpec = b("AL6061").heatTreatmentSpec.copy(
                    austenitizingOrSolutionTempC = "N/A — non-heat-treatable (5xxx)",
                    soakingTimeMinutes = "N/A",
                    quenchMedium = "N/A",
                    temperingOrAgingTempC = "N/A — H32 strain hardened + stabilized",
                    temperingOrAgingTimeHours = "N/A",
                    targetHardnessHrc = "Rivet shear 165 MPa (24 ksi)",
                    standardReference = "EN 2117 / MS20470 (B)",
                    targetUtsMpa = "≈ 276 MPa (derived: 24 ksi shear ÷ 0.6)",
                    isAging = false
                ),
                estimatedFrom = "AL6061",
                dataSources = listOf("MS20470 / MS20426 5056 rivet listings (24 ksi shear)",
                    "Navy aviation training text (5056 supplied H32, used as fabricated)",
                    "Tensile derived from shear ÷ 0.6; density and yield estimated from 6061")
            )
        )
        return grades.associateBy { it.key }
    }
}
