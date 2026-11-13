package com.example.data

/**
 * Second extension of the alloy catalogue: the commercial and specialty grades that fastener plants actually
 * buy but that were missing from the base catalogue and from [ResearchedMaterials] — medium-carbon and boron
 * cold-heading steels, the stainless families beyond 304/316/410, duplex, maraging, more nickel alloys,
 * more titanium and aluminium grades, and the copper alloys.
 *
 * Every grade is built by copying its closest catalogue grade ([MaterialInfo.estimatedFrom]) and overriding
 * only the strength and heat-treatment values for the new alloy. Machining data (Vc, feed, DOC), flow-stress
 * K and n, price and tooling stay the base grade's values and are shown in the app as "estimated from <grade>".
 *
 * HONESTY NOTE: the strengths below are the minima / typicals as tabulated in the governing product standard
 * (ASTM, EN, JIS, AMS, ISO 3506). They are entered as engineering reference values and are NOT verified
 * against a mill certificate inside this build; every grade carries that caveat in [MaterialInfo.dataSources]
 * so it shows on screen and in the exported report. Use a mill certificate for release documentation.
 */
object ExtendedMaterials {

    private const val VERIFY = "Entered from the governing product standard's tabulated minima — verify against the mill certificate before release"

    /** Copies [base] and overrides the strengths and the heat-treatment headline for a new grade. */
    private fun grade(
        base: MaterialInfo,
        key: String,
        name: String,
        uts: Double,
        yieldMpa: Double,
        elongation: Double,
        z: Double,
        wire: String,
        heatTreatment: String,
        hardness: String,
        standard: String,
        sources: List<String>,
        density: Double = base.density,
        aero: Boolean = base.isAero,
        aust: String = base.heatTreatmentSpec.austenitizingOrSolutionTempC,
        quench: String = base.heatTreatmentSpec.quenchMedium,
        temper: String = base.heatTreatmentSpec.temperingOrAgingTempC,
        aging: Boolean = base.heatTreatmentSpec.isAging,
        headingSpm: Int = base.headingSpm
    ): MaterialInfo = base.copy(
        key = key,
        name = name,
        density = density,
        uts = uts,
        isAero = aero,
        hasHighHardness = uts >= 1000.0,
        headingSpm = headingSpm,
        heatTreatment = heatTreatment,
        rawMaterialSourcingUtsMpa = wire,
        postForgingTargetUtsMpa = uts,
        yieldStrengthMpa = yieldMpa,
        minElongationPct = elongation,
        reductionOfAreaZPct = z,
        heatTreatmentSpec = base.heatTreatmentSpec.copy(
            austenitizingOrSolutionTempC = aust,
            quenchMedium = quench,
            temperingOrAgingTempC = temper,
            targetHardnessHrc = hardness,
            targetUtsMpa = "${uts.toInt()} MPa",
            hydrogenBakingRequired = uts >= 1000.0 && !aero,
            hydrogenBakingCycle = if (uts >= 1000.0 && !aero)
                "190°C – 220°C for 8 to 24 hrs within 4 hrs of plating (ISO 4042)"
            else "Not required below 1000 MPa",
            standardReference = standard,
            isAging = aging
        ),
        estimatedFrom = base.key,
        dataSources = sources + VERIFY
    )

    fun extend(cat: Map<String, MaterialInfo>): Map<String, MaterialInfo> {
        fun b(k: String) = cat.getValue(k)

        val grades = listOf(
            // ---------------- Medium carbon and cold-heading carbon steels ----------------
            grade(
                b("1018"), "1035", "Medium Carbon (AISI 1035 / C35E)", 585.0, 370.0, 18.0, 40.0,
                wire = "540 – 620 MPa (Spheroidized Annealed Wire, EN 10263-2)",
                heatTreatment = "Normalize 870°C, or Q&T: 860°C -> water/oil -> temper 540°C – 660°C.",
                hardness = "17 – 24 HRC (Q&T) / 90 – 100 HRB (normalized)",
                standard = "ASTM A29 / EN 10083-2 C35E / ISO 898-1 Class 5.8 – 6.8",
                sources = listOf("ASTM A29/A29M carbon steel bar", "EN 10083-2 C35E"),
                aust = "860°C ± 10°C", quench = "Water or oil", temper = "540°C – 660°C"
            ),
            grade(
                b("1018"), "1045", "Medium Carbon (AISI 1045 / C45E)", 655.0, 450.0, 12.0, 35.0,
                wire = "600 – 700 MPa (Cold Drawn / Annealed Wire)",
                heatTreatment = "Q&T: 840°C – 870°C -> water or oil -> temper 550°C – 660°C.",
                hardness = "22 – 28 HRC (Q&T)",
                standard = "ASTM A29 / EN 10083-2 C45E / SAE J429 Grade 5 capable",
                sources = listOf("ASTM A29/A29M carbon steel bar", "EN 10083-2 C45E"),
                aust = "840°C – 870°C", quench = "Water or oil", temper = "550°C – 660°C", headingSpm = 130
            ),
            grade(
                b("1018"), "EN8", "EN8 / 080M40 Medium Carbon (BS 970)", 700.0, 465.0, 16.0, 40.0,
                wire = "620 – 720 MPa (Cold Drawn Rod)",
                heatTreatment = "Q&T: 830°C – 860°C -> oil -> temper 550°C – 660°C.",
                hardness = "20 – 28 HRC",
                standard = "BS 970 Part 1 080M40 / EN 10083-2 C40E",
                sources = listOf("BS 970-1 080M40 (EN8) bar"),
                aust = "830°C – 860°C", quench = "Oil", temper = "550°C – 660°C", headingSpm = 130
            ),
            grade(
                b("10B21"), "10B33", "Boron Steel (AISI 10B33)", 1040.0, 940.0, 9.0, 48.0,
                wire = "480 – 580 MPa (Spheroidized Annealed Boron Wire)",
                heatTreatment = "Austenitize 860°C – 900°C -> oil or polymer quench -> temper 400°C – 480°C.",
                hardness = "32 – 39 HRC (Class 10.9)",
                standard = "SAE J403 10B33 / ISO 898-1 Class 10.9",
                sources = listOf("SAE J403 boron steel composition", "ISO 898-1 Class 10.9 minima"),
                aust = "860°C – 900°C", quench = "Oil or polymer", temper = "400°C – 480°C"
            ),
            grade(
                b("10B21"), "30MNB4", "30MnB4 / 1.5526 Mn-B Steel (EN 10263-4)", 1040.0, 940.0, 9.0, 48.0,
                wire = "500 – 600 MPa (Spheroidized Annealed Wire, EN 10263-4)",
                heatTreatment = "Austenitize 860°C – 890°C -> water or polymer quench -> temper 420°C – 500°C.",
                hardness = "32 – 39 HRC (Class 10.9)",
                standard = "EN 10263-4 30MnB4 / ISO 898-1 Class 10.9",
                sources = listOf("EN 10263-4 cold heading boron steel", "ISO 898-1 Class 10.9 minima"),
                aust = "860°C – 890°C", quench = "Water or polymer", temper = "420°C – 500°C"
            ),
            grade(
                b("4135"), "AISI_4037", "Mo Alloy Steel (AISI 4037)", 900.0, 790.0, 12.0, 50.0,
                wire = "520 – 620 MPa (Spheroidized Annealed Wire)",
                heatTreatment = "Austenitize 845°C -> oil quench -> temper 480°C – 600°C.",
                hardness = "28 – 34 HRC",
                standard = "SAE J404 4037 / ISO 898-1 Class 9.8 – 10.9",
                sources = listOf("SAE J404 alloy steel composition"),
                aust = "845°C ± 10°C", quench = "Oil", temper = "480°C – 600°C"
            ),
            grade(
                b("4135"), "EN16", "EN16 / 605M36 Mn-Mo Steel (BS 970)", 930.0, 755.0, 13.0, 50.0,
                wire = "560 – 660 MPa (Annealed Wire)",
                heatTreatment = "Austenitize 850°C – 880°C -> oil quench -> temper 550°C – 660°C.",
                hardness = "28 – 34 HRC",
                standard = "BS 970-1 605M36 (EN16) / ISO 898-1 Class 10.9",
                sources = listOf("BS 970-1 605M36 bar"),
                aust = "850°C – 880°C", quench = "Oil", temper = "550°C – 660°C"
            ),
            grade(
                b("4140"), "SCM440", "SCM440 Cr-Mo Steel (JIS G4105)", 1030.0, 835.0, 12.0, 45.0,
                wire = "600 – 700 MPa (Spheroidized Annealed Wire)",
                heatTreatment = "Austenitize 840°C – 880°C -> oil quench -> temper 540°C – 650°C.",
                hardness = "32 – 39 HRC (Class 10.9 / 12.9 at low temper)",
                standard = "JIS G4105 SCM440 / ISO 898-1 Class 10.9 – 12.9",
                sources = listOf("JIS G4105 SCM440 heat treatment and minima"),
                aust = "840°C – 880°C", quench = "Oil", temper = "540°C – 650°C"
            ),
            grade(
                b("4340"), "34CRNIMO6", "34CrNiMo6 / 1.6582 Ni-Cr-Mo Steel (EN 10083-3)", 1100.0, 900.0, 10.0, 45.0,
                wire = "680 – 800 MPa (Annealed Bar)",
                heatTreatment = "Austenitize 830°C – 860°C -> oil quench -> temper 540°C – 660°C (double temper for heavy sections).",
                hardness = "34 – 40 HRC",
                standard = "EN 10083-3 34CrNiMo6 / ISO 898-1 Class 12.9",
                sources = listOf("EN 10083-3 34CrNiMo6 quenched and tempered minima"),
                aust = "830°C – 860°C", quench = "Oil", temper = "540°C – 660°C"
            ),
            grade(
                b("1018"), "16MNCR5", "16MnCr5 / 1.7131 Case Hardening Steel (EN 10084)", 800.0, 590.0, 10.0, 40.0,
                wire = "440 – 540 MPa (Annealed Wire — core strength before carburizing)",
                heatTreatment = "Carburize 900°C – 930°C (case 0.2 – 0.6 mm) -> oil quench -> temper 170°C – 200°C.",
                hardness = "Case 58 – 64 HRC, core 30 – 42 HRC",
                standard = "EN 10084 16MnCr5 / DIN 7500 thread-forming screws",
                sources = listOf("EN 10084 case hardening steel", "DIN 7500 case hardness for thread-forming screws"),
                aust = "Carburizing: 900°C – 930°C", quench = "Oil", temper = "170°C – 200°C (stress relief of case)"
            ),

            // ---------------- Stainless: austenitic beyond 304 / 316 ----------------
            grade(
                b("SS304"), "SS302", "Stainless 302 / 1.4319 Austenitic (AISI 302)", 620.0, 275.0, 40.0, 55.0,
                wire = "520 – 620 MPa (Annealed / Lightly Drawn Wire)",
                heatTreatment = "Solution anneal 1010°C – 1120°C -> rapid cool. Strength from cold work only.",
                hardness = "≤ 92 HRB annealed; A2-70 by cold drawing",
                standard = "ASTM A493 / ISO 3506-1 A2-70",
                sources = listOf("ASTM A493 stainless wire for cold heading", "ISO 3506-1 A2 class minima")
            ),
            grade(
                b("SS304"), "SS303", "Stainless 303 / 1.4305 Free-Machining (AISI 303)", 620.0, 240.0, 35.0, 45.0,
                wire = "520 – 620 MPa (Annealed Bar — machining grade, not for cold heading)",
                heatTreatment = "Solution anneal 1010°C – 1120°C -> rapid cool. Sulphur addition: machining only, poor formability.",
                hardness = "≤ 92 HRB annealed",
                standard = "ASTM A582 / ISO 3506-1 A1 (free-machining, not for A2 duties)",
                sources = listOf("ASTM A582 free-machining stainless bar", "ISO 3506-1 A1 restrictions")
            ),
            grade(
                b("SS304"), "SS305", "Stainless 305 / 1.4303 Low Work-Hardening (AISI 305)", 585.0, 260.0, 40.0, 60.0,
                wire = "480 – 580 MPa (Annealed Heading Wire — the preferred cold-heading austenitic)",
                heatTreatment = "Solution anneal 1010°C – 1120°C -> rapid cool. High Ni keeps work hardening low for deep recesses.",
                hardness = "≤ 88 HRB annealed; A2-70 by cold drawing",
                standard = "ASTM A493 Type 305 / ISO 3506-1 A2-70",
                sources = listOf("ASTM A493 Type 305 cold heading wire", "ISO 3506-1 A2 class minima"),
                headingSpm = (b("SS304").headingSpm * 1.15).toInt()
            ),
            grade(
                b("SS316"), "SS321", "Stainless 321 / 1.4541 Ti-Stabilised (AISI 321)", 620.0, 240.0, 35.0, 50.0,
                wire = "515 – 620 MPa (Annealed Wire)",
                heatTreatment = "Solution anneal 1010°C – 1065°C -> rapid cool. Ti stabilised: no sensitisation to 870°C.",
                hardness = "≤ 95 HRB annealed",
                standard = "ASTM A193 B8T / ISO 3506-1 A3",
                sources = listOf("ASTM A193/A193M Grade B8T", "ASTM A240 Type 321")
            ),
            grade(
                b("SS316"), "SS347", "Stainless 347 / 1.4550 Nb-Stabilised (AISI 347)", 620.0, 240.0, 35.0, 50.0,
                wire = "515 – 620 MPa (Annealed Wire)",
                heatTreatment = "Solution anneal 1010°C – 1065°C -> rapid cool. Nb/Ta stabilised for high-temperature service.",
                hardness = "≤ 95 HRB annealed",
                standard = "ASTM A193 B8C / ISO 3506-1 A3",
                sources = listOf("ASTM A193/A193M Grade B8C", "ASTM A240 Type 347")
            ),
            grade(
                b("SS316"), "ALLOY_20", "Alloy 20 / 20Cb-3 (UNS N08020)", 585.0, 240.0, 30.0, 45.0,
                wire = "550 – 650 MPa (Annealed Bar)",
                heatTreatment = "Solution anneal 925°C – 1010°C -> rapid cool. Not hardenable by heat treatment.",
                hardness = "≤ 95 HRB annealed",
                standard = "ASTM B473 / ASTM A276 UNS N08020",
                sources = listOf("ASTM B473 UNS N08020 bar and wire"),
                density = 8.08
            ),

            // ---------------- Stainless: ferritic, martensitic, duplex, PH ----------------
            grade(
                b("SS410"), "SS430", "Stainless 430 / 1.4016 Ferritic (AISI 430)", 600.0, 410.0, 20.0, 40.0,
                wire = "450 – 550 MPa (Annealed Wire)",
                heatTreatment = "Anneal 790°C – 845°C -> slow cool. Not hardenable: strength from cold work (ISO 3506 class F1).",
                hardness = "≤ 89 HRB (ferritic, no martensite)",
                standard = "ASTM A276 Type 430 / ISO 3506-1 Class F1",
                sources = listOf("ASTM A276 Type 430", "ISO 3506-1 Class F1-45 / F1-60 minima")
            ),
            grade(
                b("SS410"), "SS431", "Stainless 431 / 1.4057 Martensitic Ni-Cr (AISI 431)", 900.0, 700.0, 12.0, 35.0,
                wire = "620 – 760 MPa (Annealed Bar)",
                heatTreatment = "Austenitize 980°C – 1065°C -> oil quench -> temper 580°C – 650°C.",
                hardness = "28 – 36 HRC",
                standard = "ASTM A276 Type 431 / ISO 3506-1 Class C3-80",
                sources = listOf("ASTM A276 Type 431", "ISO 3506-1 Class C3-80 minima"),
                aust = "980°C – 1065°C", quench = "Oil", temper = "580°C – 650°C"
            ),
            grade(
                b("SS316"), "DUPLEX_2205", "Duplex 2205 / 1.4462 (UNS S32205)", 760.0, 550.0, 25.0, 40.0,
                wire = "700 – 850 MPa (Solution Annealed Bar)",
                heatTreatment = "Solution anneal 1020°C – 1100°C -> water quench. Never temper: 475°C and sigma embrittlement.",
                hardness = "≤ 31 HRC (solution annealed)",
                standard = "ASTM A276 / ASTM A479 UNS S32205 / ISO 3506-1 Class D",
                sources = listOf("ASTM A479 UNS S32205", "ISO 3506-1 Class D duplex minima"),
                density = 7.80,
                aust = "Solution: 1020°C – 1100°C", quench = "Water", temper = "None — do not temper (sigma / 475°C embrittlement)"
            ),
            grade(
                b("SS316"), "SUPER_DUPLEX_2507", "Super Duplex 2507 / 1.4410 (UNS S32750)", 830.0, 550.0, 15.0, 35.0,
                wire = "800 – 950 MPa (Solution Annealed Bar)",
                heatTreatment = "Solution anneal 1025°C – 1125°C -> water quench. Never temper.",
                hardness = "≤ 32 HRC (solution annealed)",
                standard = "ASTM A479 UNS S32750 / NORSOK M-650",
                sources = listOf("ASTM A479 UNS S32750 super duplex bar"),
                density = 7.80,
                aust = "Solution: 1025°C – 1125°C", quench = "Water", temper = "None — do not temper"
            ),
            grade(
                b("17_4PH"), "17_7PH", "17-7PH Precipitation Hardening (AMS 5528 / UNS S17700)", 1310.0, 1170.0, 6.0, 25.0,
                wire = "900 – 1100 MPa (Condition A Wire)",
                heatTreatment = "Condition A -> austenite conditioning 760°C / 90 min -> cool to 15°C -> age 565°C / 90 min (CH900 / RH950 routes).",
                hardness = "41 – 48 HRC (RH950)",
                standard = "AMS 5528 / AMS 5678 17-7PH",
                sources = listOf("AMS 5528 17-7PH sheet and wire", "ASTM A313 Type 631 spring wire"),
                density = 7.81, aero = true,
                aust = "Conditioning: 760°C (TH1050) / 955°C (RH950)", quench = "Air cool, then sub-zero to 15°C (RH950)",
                temper = "Aging: 510°C – 565°C", aging = true
            ),
            grade(
                b("A286"), "A453_660", "ASTM A453 Grade 660 Bolting (A286-type, UNS S66286)", 895.0, 585.0, 15.0, 20.0,
                wire = "800 – 950 MPa (Solution Treated Bar)",
                heatTreatment = "Solution 980°C ± 15°C / 1 hr -> oil quench -> age 720°C / 16 hrs (Class A).",
                hardness = "24 – 35 HRC",
                standard = "ASTM A453 Grade 660 Class A / B",
                sources = listOf("ASTM A453/A453M Grade 660 bolting"),
                aero = false, aust = "Solution: 980°C ± 15°C", quench = "Oil", temper = "Aging: 720°C / 16 hrs", aging = true
            ),
            grade(
                b("300M"), "MARAGING_250", "Maraging C250 (AMS 6512 / UNS K92890)", 1725.0, 1655.0, 8.0, 35.0,
                wire = "1000 – 1100 MPa (Solution Annealed Bar — soft, machinable)",
                heatTreatment = "Solution anneal 815°C / 1 hr -> air cool -> age 480°C / 3 – 6 hrs (no quench distortion).",
                hardness = "48 – 52 HRC",
                standard = "AMS 6512 / AMS 6520 Maraging 250",
                sources = listOf("AMS 6512 maraging steel 250 bar"),
                density = 8.00, aero = true,
                aust = "Solution: 815°C / 1 hr", quench = "Air cool", temper = "Aging: 480°C / 3 – 6 hrs", aging = true
            ),

            // ---------------- Nickel and cobalt alloys ----------------
            grade(
                b("WASPALOY"), "NIMONIC_90", "Nimonic 90 (UNS N07090)", 1200.0, 810.0, 20.0, 25.0,
                wire = "1000 – 1200 MPa (Solution Treated Bar)",
                heatTreatment = "Solution 1080°C / 8 hrs -> air cool -> age 700°C / 16 hrs.",
                hardness = "32 – 40 HRC",
                standard = "BS HR 601 / UNS N07090 (Special Metals datasheet range)",
                sources = listOf("Special Metals NIMONIC alloy 90 datasheet"),
                density = 8.19, aero = true,
                aust = "Solution: 1080°C / 8 hrs", quench = "Air cool", temper = "Aging: 700°C / 16 hrs", aging = true
            ),
            grade(
                b("MONEL_K500"), "MONEL_400", "Monel 400 (UNS N04400)", 550.0, 240.0, 35.0, 45.0,
                wire = "480 – 620 MPa (Annealed Bar)",
                heatTreatment = "Anneal 870°C – 980°C -> rapid cool. Not age hardenable: strength from cold work only.",
                hardness = "≤ 75 HRB annealed; 85 – 95 HRB cold drawn",
                standard = "ASTM B164 / ASTM F468 UNS N04400",
                sources = listOf("ASTM B164 nickel-copper alloy rod", "ASTM F468 nonferrous bolting"),
                density = 8.80,
                aust = "Anneal: 870°C – 980°C", quench = "Rapid cool (no hardening)", temper = "Not applicable — solid solution alloy",
                aging = false
            ),
            grade(
                b("HASTELLOY_C276"), "HASTELLOY_B3", "Hastelloy B-3 (UNS N10675)", 760.0, 350.0, 40.0, 45.0,
                wire = "700 – 850 MPa (Solution Annealed Bar)",
                heatTreatment = "Solution anneal 1065°C -> water quench. Not age hardenable.",
                hardness = "≤ 100 HRB annealed",
                standard = "ASTM B335 / ASTM B574 UNS N10675",
                sources = listOf("ASTM B335 nickel-molybdenum alloy rod"),
                density = 9.22,
                aust = "Solution: 1065°C", quench = "Water", temper = "Not applicable — solid solution alloy", aging = false
            ),
            grade(
                b("INCONEL_600"), "INCOLOY_825", "Incoloy 825 (UNS N08825)", 690.0, 310.0, 30.0, 40.0,
                wire = "620 – 760 MPa (Annealed Bar)",
                heatTreatment = "Anneal 940°C – 1010°C -> rapid cool. Not age hardenable.",
                hardness = "≤ 95 HRB annealed",
                standard = "ASTM B425 / ASTM B408 UNS N08825",
                sources = listOf("ASTM B425 UNS N08825 rod and bar"),
                density = 8.14,
                aust = "Anneal: 940°C – 1010°C", quench = "Rapid cool", temper = "Not applicable — solid solution alloy", aging = false
            ),

            // ---------------- Titanium ----------------
            grade(
                b("TI_GR1"), "TI_GR2", "Titanium Grade 2 CP (UNS R50400)", 345.0, 275.0, 20.0, 30.0,
                wire = "345 – 480 MPa (Annealed Bar)",
                heatTreatment = "Mill anneal 650°C – 760°C / 1 – 2 hrs -> air cool. Not hardenable.",
                hardness = "≤ 80 HRB",
                standard = "ASTM B348 Grade 2 / ASTM F467 – F468",
                sources = listOf("ASTM B348 Grade 2 titanium bar"),
                density = 4.51, aero = false,
                aust = "Anneal: 650°C – 760°C", quench = "Air cool", temper = "Not applicable — CP titanium", aging = false
            ),
            grade(
                b("TI_GR1"), "TI_GR7", "Titanium Grade 7 Ti-0.15Pd (UNS R52400)", 345.0, 275.0, 20.0, 30.0,
                wire = "345 – 480 MPa (Annealed Bar)",
                heatTreatment = "Mill anneal 650°C – 760°C -> air cool. Pd addition for crevice corrosion resistance.",
                hardness = "≤ 80 HRB",
                standard = "ASTM B348 Grade 7",
                sources = listOf("ASTM B348 Grade 7 titanium-palladium bar"),
                density = 4.51, aero = false,
                aust = "Anneal: 650°C – 760°C", quench = "Air cool", temper = "Not applicable", aging = false
            ),
            grade(
                b("TI_GR1"), "TI_GR12", "Titanium Grade 12 Ti-0.3Mo-0.8Ni (UNS R53400)", 483.0, 345.0, 18.0, 30.0,
                wire = "483 – 620 MPa (Annealed Bar)",
                heatTreatment = "Mill anneal 705°C – 790°C -> air cool.",
                hardness = "≤ 90 HRB",
                standard = "ASTM B348 Grade 12",
                sources = listOf("ASTM B348 Grade 12 titanium bar"),
                density = 4.51, aero = false,
                aust = "Anneal: 705°C – 790°C", quench = "Air cool", temper = "Not applicable", aging = false
            ),
            grade(
                b("TI_GR5"), "TI_6AL6V2SN", "Ti-6Al-6V-2Sn (AMS 4978 / UNS R56620)", 1030.0, 965.0, 8.0, 20.0,
                wire = "930 – 1100 MPa (Annealed Bar)",
                heatTreatment = "Solution 885°C – 900°C -> water quench -> age 540°C – 595°C / 4 – 8 hrs (STA).",
                hardness = "36 – 42 HRC",
                standard = "AMS 4978 / ASTM B348 Grade 6Al-6V-2Sn",
                sources = listOf("AMS 4978 Ti-6Al-6V-2Sn bar"),
                density = 4.54, aero = true,
                aust = "Solution: 885°C – 900°C", quench = "Water", temper = "Aging: 540°C – 595°C / 4 – 8 hrs", aging = true
            ),
            grade(
                b("TI_GR5"), "TI_5553", "Ti-5Al-5Mo-5V-3Cr (Ti-5553, near-beta)", 1250.0, 1150.0, 7.0, 18.0,
                wire = "1100 – 1250 MPa (Solution Treated Bar)",
                heatTreatment = "Solution 820°C – 850°C (below beta transus) -> air or water cool -> age 580°C – 620°C / 8 hrs.",
                hardness = "40 – 44 HRC",
                standard = "OEM / AMS 6946-type Ti-5553 landing gear and fastener spec",
                sources = listOf("Ti-5553 near-beta alloy producer datasheet range"),
                density = 4.65, aero = true,
                aust = "Solution: 820°C – 850°C", quench = "Air or water", temper = "Aging: 580°C – 620°C / 8 hrs", aging = true
            ),

            // ---------------- Aluminium ----------------
            grade(
                b("AL6061"), "AL6082", "Aluminium 6082-T6 (EN AW-6082)", 340.0, 290.0, 8.0, 15.0,
                wire = "310 – 350 MPa (T6 Extruded / Drawn Rod)",
                heatTreatment = "Solution 525°C -> water quench -> age 175°C / 8 hrs (T6).",
                hardness = "95 – 110 HB",
                standard = "EN 573 / EN 755 EN AW-6082 T6",
                sources = listOf("EN 755-2 EN AW-6082 T6 mechanical properties"),
                density = 2.70,
                aust = "Solution: 525°C ± 5°C", quench = "Water", temper = "Aging: 175°C / 8 hrs (T6)", aging = true
            ),
            grade(
                b("AL6061"), "AL6262", "Aluminium 6262-T9 Free-Machining (UNS A96262)", 400.0, 380.0, 5.0, 10.0,
                wire = "380 – 420 MPa (T9 Cold Worked and Aged Rod)",
                heatTreatment = "Solution 530°C -> water quench -> cold work -> age 175°C / 8 hrs (T9).",
                hardness = "110 – 120 HB",
                standard = "ASTM B211 Alloy 6262-T9",
                sources = listOf("ASTM B211 aluminium alloy bar and rod 6262"),
                density = 2.72,
                aust = "Solution: 530°C", quench = "Water", temper = "Aging: 175°C / 8 hrs (T9)", aging = true
            ),
            grade(
                b("AL2024"), "AL2014", "Aluminium 2014-T6 (UNS A92014)", 483.0, 414.0, 7.0, 12.0,
                wire = "450 – 500 MPa (T6 Rod)",
                heatTreatment = "Solution 500°C – 507°C -> cold water quench -> age 160°C / 18 hrs (T6).",
                hardness = "135 – 145 HB",
                standard = "ASTM B211 / AMS 4121 Alloy 2014-T6",
                sources = listOf("AMS 4121 2014-T6 bar and rod"),
                density = 2.80, aero = true,
                aust = "Solution: 500°C – 507°C", quench = "Cold water", temper = "Aging: 160°C / 18 hrs (T6)", aging = true
            ),
            grade(
                b("AL7075"), "AL7175", "Aluminium 7175-T736 (UNS A97175)", 572.0, 503.0, 7.0, 12.0,
                wire = "540 – 600 MPa (T736 Forged Stock)",
                heatTreatment = "Solution 470°C – 480°C -> water quench -> two-step age 107°C / 8 hrs + 163°C / 8 hrs (T736).",
                hardness = "150 – 160 HB",
                standard = "AMS 4149 / AMS-QQ-A-367 Alloy 7175-T736",
                sources = listOf("AMS 4149 7175-T74 / T736 forging stock"),
                density = 2.80, aero = true,
                aust = "Solution: 470°C – 480°C", quench = "Water", temper = "Aging: 107°C / 8 hrs + 163°C / 8 hrs (T736)", aging = true
            ),
            grade(
                b("AL6061"), "AL5052", "Aluminium 5052-H32 (EN AW-5052)", 260.0, 195.0, 12.0, 20.0,
                wire = "230 – 280 MPa (H32 Strain Hardened)",
                heatTreatment = "Not heat treatable: strength from strain hardening (H32 = quarter hard, stabilised).",
                hardness = "60 – 70 HB",
                standard = "ASTM B209 / EN 573 EN AW-5052 H32",
                sources = listOf("ASTM B209 Alloy 5052-H32"),
                density = 2.68,
                aust = "Not applicable — strain hardened", quench = "Not applicable",
                temper = "Stabilising anneal 150°C (H32)", aging = false
            ),

            // ---------------- Copper alloys ----------------
            grade(
                b("BRASS_C360"), "PHOS_BRONZE_C510", "Phosphor Bronze C51000 (CuSn5)", 470.0, 390.0, 10.0, 20.0,
                wire = "420 – 520 MPa (Hard Drawn Wire)",
                heatTreatment = "Not hardenable: strength from cold work. Stress relieve 190°C – 230°C / 1 hr.",
                hardness = "85 – 95 HRB (hard drawn)",
                standard = "ASTM B139 Alloy C51000",
                sources = listOf("ASTM B139 phosphor bronze rod and bar"),
                density = 8.86,
                aust = "Not applicable — cold worked", quench = "Not applicable",
                temper = "Stress relief: 190°C – 230°C / 1 hr", aging = false
            ),
            grade(
                b("BRASS_C360"), "SIL_BRONZE_C655", "Silicon Bronze C65500 (CuSi3Mn)", 450.0, 240.0, 20.0, 30.0,
                wire = "415 – 500 MPa (Half Hard Rod)",
                heatTreatment = "Not hardenable: strength from cold work. Anneal 480°C – 700°C -> any cooling rate.",
                hardness = "75 – 90 HRB",
                standard = "ASTM B98 Alloy C65500 / ASTM F468",
                sources = listOf("ASTM B98 silicon bronze rod and bar", "ASTM F468 nonferrous bolting"),
                density = 8.53,
                aust = "Anneal: 480°C – 700°C", quench = "Any rate (no hardening)",
                temper = "Stress relief: 230°C / 1 hr", aging = false
            ),
            grade(
                b("BRASS_C360"), "CUNI_C715", "Copper-Nickel 70/30 C71500 (CuNi30Mn1Fe)", 380.0, 140.0, 30.0, 40.0,
                wire = "345 – 415 MPa (Annealed Rod)",
                heatTreatment = "Not hardenable: anneal 700°C – 820°C -> any cooling rate. Strength from cold work.",
                hardness = "≤ 75 HRB annealed",
                standard = "ASTM B151 Alloy C71500 / ASTM F468",
                sources = listOf("ASTM B151 copper-nickel rod", "ASTM F468 nonferrous bolting"),
                density = 8.94,
                aust = "Anneal: 700°C – 820°C", quench = "Any rate (no hardening)",
                temper = "Not applicable — solid solution alloy", aging = false
            )
        )

        return grades.associateBy { it.key }
    }
}
