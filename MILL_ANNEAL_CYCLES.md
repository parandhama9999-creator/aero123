# Mill Annealing Cycles — 14 Mill-Annealed Grades

Researched from producer and supplier datasheets, 31 Oct 2026. Every row cites its source.
°F values are as published; °C converted. **Always confirm against the governing AMS / ASTM / GOST spec
and the mill certificate before setting a furnace** — sources disagree on hold times in several cases, noted below.

---

## ⚠️ Correction to ANNEAL_CYCLES.md

The earlier report listed the catalogue's heat-treat temperature as the *intermediate anneal cycle*.
**For the titanium grades that is wrong.** The catalogue holds the **solution-treatment** temperature
(the first step of solution-treat-and-age hardening), not an anneal:

| Grade | Catalogue value (solution treat) | Published mill anneal | Gap |
|---|---|---|---|
| Ti-6Al-4V (Gr 5) | 955 °C | **704 – 788 °C** | ~170 – 250 °C too hot |
| Ti-6Al-4V ELI (Gr 23) | 925 – 940 °C | **704 – 732 °C** | ~200 °C too hot |
| Ti-6242 | 970 °C | **704 – 843 °C** | ~130 – 270 °C too hot |
| Ti CP Gr 1/2 | 520 – 540 °C *(stress relief)* | **649 – 760 °C** | too cold — doesn't recrystallise |

Heating Ti-6Al-4V to 955 °C between blows is a solution treatment, not a softening anneal.
Use the mill-anneal values below for intermediate annealing of titanium.

For the nickel and iron-base superalloys, the catalogue value and the published anneal agree
(the solution anneal *is* the softening anneal for those), with the exceptions flagged in the table.

---

## Titanium

| Grade | Mill anneal | Hold | Cooling | Catalogue | Source |
|---|---|---|---|---|---|
| **Ti-6Al-4V (Gr 5)** | **704 – 788 °C** (1300 – 1450 °F) | 2 h | Air cool | ✗ 955 °C is solution treat | [Rolled Alloys 6Al-4V datasheet](https://www.rolledalloys.ca/wp-content/uploads/6AL4V_Data-sheet.pdf) |
| **Ti-6Al-4V ELI (Gr 23)** | **704 – 732 °C** (1300 – 1350 °F) | 1 – 8 h | Air cool | ✗ 925 – 940 °C is solution treat | [ATI 6-4 ELI datasheet](https://www.atimaterials.com/Products/Documents/datasheets/titanium/alloyed/ati_6-4eli_tds_en_v1.pdf) — AMS 4930 (bar & wire, annealed) |
| **Ti-6Al-2Sn-4Zr-2Mo (6242)** | **704 – 843 °C** (1300 – 1550 °F) mill anneal | — | Air cool | ✗ 970 °C | [TIMET 6242 datasheet](https://www.timet.com/assets/local/documents/datasheets/alphaalloys/6242.pdf) |
| ↳ 6242 duplex anneal | 899 °C (1650 °F) → then 788 °C (1450 °F) | 30 min + 15 min | Air cool both stages | | TIMET; [Aerospace Metals 6242 duplex](https://www.aerospacemetals.com/wp-content/uploads/2023/06/Titanium-Ti-6Al-2Sn-4Zr-2Mo-0.1Si-Duplex-Annealed.pdf) gives 910 °C / 796 °C |
| **Ti CP Grade 1 / 2** | **649 – 760 °C** (1200 – 1400 °F) | 6 min – 2 h | Air cool | ✗ 520 – 540 °C is stress relief | [Corrosion Materials Ti Gr 1–4](https://corrosionmaterials.com/wp-content/uploads/docs/TitaniumGrade1-4.pdf); [Rolled Alloys CP Gr 2](https://www.rolledalloys.ca/wp-content/uploads/titanium-CP3-GR-2_data-sheet-rolled-alloys.pdf) gives up to 788 °C (1450 °F), 15 min – 2 h |
| **VT16 (Ti-3Al-5Mo-4.5V)** | **770 – 790 °C** full anneal | 2 h | **Furnace cool 1 – 5 °C/min to 500 °C**, then air | catalogue 800 – 820 °C is solution treat | TsKBA standard process, [meganorm.ru](https://meganorm.ru/Data2/0/4294805/4294805886.htm) |

Titanium notes:
- Mill anneal is a *recovery / partial recrystallisation* anneal — the general-purpose condition titanium wire is sold in.
- CP grades cannot be hardened by heat treatment, so anneal is the only thermal step.
- Use vacuum or inert atmosphere above ~600 °C to avoid alpha-case (oxygen-embrittled surface) — this cracks on heading.

---

## Nickel & iron-base superalloys

| Grade | Anneal / solution anneal | Hold | Cooling | Catalogue | Source |
|---|---|---|---|---|---|
| **Inconel 718** | **927 – 1010 °C** (1700 – 1850 °F); 980 °C typical | — | Air cool or faster | ✓ 955 – 980 °C | [HPAlloys 718](https://www.hpalloys.com/Alloys/718); [Total Materia](https://www.totalmateria.com/en-us/material/3241752) (980 °C). Governing spec: AMS 5662 |
| **A286** | **982 °C** (1800 °F) — max creep strength<br>or **899 °C** (1650 °F) — better ductility | 1 h | Water or oil quench | ✓ 980 °C | [Aircraft Materials A286](https://aircraftmaterials.com/data/alstst/A286.html); [UPMET A286](https://www.upmet.com/products/nickel-alloys/alloy-a286). Governing: AMS 5731 / 5732 / 5737 |
| **Waspaloy** | **1049 °C** (1920 °F) | 4 h | Air cool | ✓ 1010 – 1040 °C | [Alloy Wire](https://www.alloywire.us.com/?p=28140); γ′ solvus 1016 – 1027 °C per [High Temp Metals](https://www.hightempmetals.com/techdata/hitempWaspaloydata.php). Haynes: **heat as fast as possible** to avoid strain-age cracking ([Haynes](https://haynesintl.com/en/datasheet/haynes-waspaloy-alloy)) |
| **René 41** | **1066 – 1080 °C** (1950 – 1975 °F) | 4 h | Air cool (water quench preferred before welding) | ⚠ 1080 – 1120 °C — upper end above sources | [Alfa Chemistry](https://alloys.alfa-chemistry.com/rene-41.html); [High Temp Metals](https://hightempmetals.com/techdata/hitempRene41data.php). Rolled Alloys gives 1177 °C (2150 °F) — outlier |
| **Inconel 625** | **Grade 1 (annealed): 871 °C min** (1600 °F); typical 927 – 1038 °C, **982 °C most used**<br>Grade 2 (solution): 1093 °C (2000 °F) | — | Air cool | ⚠ 1090 – 1150 °C is **Grade 2** solution | [Langley Alloys](https://www.langleyalloys.com/knowledge-advice/what-is-inconel-625-grade-2/); [Rickard Metals](https://rickardmetals.com/?p=512). Governing: AMS 5666 |
| **Inconel 600** | **1010 °C** (1850 °F); brief 1040 °C OK without grain coarsening | 15 min | — | ✓ 1010 – 1050 °C | [Total Materia (cites Special Metals)](https://www.totalmateria.com/en-us/material/3241703); [Austral Wright](https://australwright.com.au/nickel-alloys-inconel-alloy-600-n06600/) |
| **Hastelloy C-276** | **1121 °C** (2050 °F) | — | **Rapid**: water quench; fast air only if < 10 mm | ✓ 1120 °C | [HPAlloys C-276](https://www.hpalloys.com/docs/hpalloy_c276.pdf); [AZoM](https://www.azom.com/article.aspx?ArticleID=7804) |
| **Monel K-500** | **982 – 1038 °C** (1800 – 1900 °F) | — | Water quench | ✓ 980 °C | [High Temp Metals](https://hightempmetals.com/techdata/hitempMonelK500data.php); [Metal Suppliers Online](https://www.suppliersonline.com/propertypages/monelk500.asp) (1038 °C, WQ) |
| **EP33 (10Kh11N23T3MR)** | ~1000 – 1050 °C (low confidence) | — | Air | ? 1080 – 1110 °C — **cannot confirm** | Only source: a garbled table in a TsKBA amendment, [meganorm.ru](https://meganorm.ru/Data2/0/4294813/4294813864.htm). **Get the GOST/TU for this grade.** |

Nickel-alloy notes:
- Inconel 625 Grade 1 vs Grade 2 matters for heading: **Grade 1 is the softer, finer-grain condition** you want for cold forming. The catalogue's 1090 – 1150 °C is the Grade 2 solution cycle, which coarsens grain.
- Waspaloy and René 41 are prone to **strain-age cracking**: heat through 650 – 870 °C quickly.
- C-276: slow cooling precipitates carbides and destroys corrosion resistance — quench.

---

## Confidence

| | Grades |
|---|---|
| **Producer / supplier datasheet, specific values** | Ti-6Al-4V, Ti-6Al-4V ELI, Ti-6242, Ti CP, VT16 |
| **Multiple secondary sources, consistent** | Inconel 718, A286, Waspaloy, Inconel 600, C-276, Monel K-500 |
| **Secondary, with a disagreement noted** | René 41, Inconel 625 |
| **Low — verify before use** | EP33 |

None of these came from the AMS documents themselves (they are paywalled). The AMS spec on your
purchase order and the mill certificate govern.
