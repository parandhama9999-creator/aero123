# Annealing Cycles — All Catalogue Materials

Generated from AeroFast Pro's own material catalogue.
Every value is either taken from the catalogue or derived from it; nothing is invented.
Rows marked **ENTER PER GRADE** are values the catalogue does not carry.

> **Titanium: use MILL_ANNEAL_CYCLES.md, not the titanium rows below.** The titanium temperatures in
> the tables here are the catalogue's **solution-treatment** temperatures, which are roughly 200 °C
> above the mill-anneal and are a hardening step, not an anneal. MILL_ANNEAL_CYCLES.md carries the
> corrected, sourced mill-anneal cycles and is the one the app implements (`SoftAnnealEngine`), so
> the app is right even where these tables are not. The titanium rows are kept only so the catalogue
> value each grade carries can still be traced.

## Summary by route

| Route | Grades | Anneal in-house? |
|---|---:|---|
| Sub-critical spheroidize anneal | 25 | yes |
| Solution anneal + rapid cool | 15 | yes |
| Full anneal to O temper | 8 | no — supplied annealed |
| Mill-annealed — supplied ready to head | 18 | no — supplied annealed |
| Anneal required — wire supplied hard | 3 | yes |

---

## Sub-critical spheroidize anneal

*Convert lamellar pearlite to globular cementite so the wire can be headed without cracking*

| Grade | Family | Temperature | Soak | Cooling | Target Rm (annealed) | Target hardness | Standard |
|---|---|---|---|---|---|---|---|
| **1008**<br><sub>Mild Steel (AISI 1008/1010)</sub> | carbon alloy steel | 698 – 718 °C (just below A1; baseline A1 = 723 °C) | Coil-mass driven — set from furnace load, not part Ø | Slow furnace cool through the transformation range, then still air | 380 – 440 MPa (Annealed Drawn Wire) | 112 – 130 HB (derived: Rm / 3.38)<br>≈ 63 – 71 HRB (ASTM A370 conversion) | EN 10263-2 / EN 10263-4 (steel rod & wire for cold heading) |
| **1022**<br><sub>AISI 1022 / SWCH22A Case-Hardening Steel (tapping & thread-forming screws)</sub> | carbon alloy steel | 698 – 718 °C (just below A1; baseline A1 = 723 °C) | Coil-mass driven — set from furnace load, not part Ø | Slow furnace cool through the transformation range, then still air | 440 – 520 MPa (Annealed Wire — Rm estimated from 1018) | 130 – 154 HB (derived: Rm / 3.38)<br>≈ 71 – 81 HRB (ASTM A370 conversion) | EN 10263-2 / EN 10263-4 (steel rod & wire for cold heading) |
| **10B21**<br><sub>10B21 / 10B38 Boron Steel</sub> | carbon alloy steel | 698 – 718 °C (just below A1; baseline A1 = 723 °C) | Coil-mass driven — set from furnace load, not part Ø | Slow furnace cool through the transformation range, then still air | 480 – 560 MPa (Spheroidized Annealed Wire, >85% globular) | 142 – 166 HB (derived: Rm / 3.38)<br>≈ 76 – 85 HRB (ASTM A370 conversion) | EN 10263-2 / EN 10263-4 (steel rod & wire for cold heading) |
| **16KHGSN**<br><sub>16KHGSN (GOST 16ХГСН Russian Aero Steel)</sub> | carbon alloy steel | 698 – 718 °C (just below A1; baseline A1 = 723 °C) | Coil-mass driven — set from furnace load, not part Ø | Slow furnace cool through the transformation range, then still air | 560 – 650 MPa (Annealed Wire) | 166 – 192 HB (derived: Rm / 3.38)<br>≈ 85 – 91 HRB (ASTM A370 conversion) | EN 10263-2 / EN 10263-4 (steel rod & wire for cold heading) |
| **18CRMO4**<br><sub>18CrMo4 / 1.7243 Case-Hardening Steel (EN 10084 / EN 10263-3)</sub> | carbon alloy steel | 698 – 718 °C (just below A1; baseline A1 = 723 °C) | Coil-mass driven — set from furnace load, not part Ø | Slow furnace cool through the transformation range, then still air | 540 – 620 MPa (Spheroidized Annealed Wire — Rm estimated from AISI_4130) | 160 – 183 HB (derived: Rm / 3.38)<br>≈ 83 – 90 HRB (ASTM A370 conversion) | EN 10263-2 / EN 10263-4 (steel rod & wire for cold heading) |
| **20MNB4**<br><sub>20MnB4 / 1.5525 Mn-B Boron Steel (EN 10263-4)</sub> | carbon alloy steel | 698 – 718 °C (just below A1; baseline A1 = 723 °C) | Coil-mass driven — set from furnace load, not part Ø | Slow furnace cool through the transformation range, then still air | 480 – 560 MPa (Spheroidized Annealed Wire — Rm estimated from 10B21) | 142 – 166 HB (derived: Rm / 3.38)<br>≈ 76 – 85 HRB (ASTM A370 conversion) | EN 10263-2 / EN 10263-4 (steel rod & wire for cold heading) |
| **23MNB4**<br><sub>23MnB4 / 1.5535 Mn-B Boron Steel (EN 10263-4)</sub> | carbon alloy steel | 698 – 718 °C (just below A1; baseline A1 = 723 °C) | Coil-mass driven — set from furnace load, not part Ø | Slow furnace cool through the transformation range, then still air | 480 – 560 MPa (Spheroidized Annealed Wire — Rm estimated from 10B21) | 142 – 166 HB (derived: Rm / 3.38)<br>≈ 76 – 85 HRB (ASTM A370 conversion) | EN 10263-2 / EN 10263-4 (steel rod & wire for cold heading) |
| **300M**<br><sub>300M Ultra-High Strength (AMS 6417)</sub> | carbon alloy steel | 698 – 718 °C (just below A1; baseline A1 = 723 °C) | Coil-mass driven — set from furnace load, not part Ø | Slow furnace cool through the transformation range, then still air | 820 – 900 MPa (Vacuum Arc Remelted VAR Wire) | 243 – 266 HB (derived: Rm / 3.38) | EN 10263-2 / EN 10263-4 (steel rod & wire for cold heading) |
| **30KHGSA**<br><sub>30KHGSA (GOST 4543 30ХГСА Aero Steel)</sub> | carbon alloy steel | 698 – 718 °C (just below A1; baseline A1 = 723 °C) | Coil-mass driven — set from furnace load, not part Ø | Slow furnace cool through the transformation range, then still air | 580 – 650 MPa (Annealed Wire) | 172 – 192 HB (derived: Rm / 3.38)<br>≈ 86 – 91 HRB (ASTM A370 conversion) | EN 10263-2 / EN 10263-4 (steel rod & wire for cold heading) |
| **32CRB4**<br><sub>32CrB4 / 1.7076 Cr-B Boron Steel (EN 10263-4)</sub> | carbon alloy steel | 698 – 718 °C (just below A1; baseline A1 = 723 °C) | Coil-mass driven — set from furnace load, not part Ø | Slow furnace cool through the transformation range, then still air | 540 – 620 MPa (Spheroidized Annealed Wire — Rm estimated from AISI_51B37) | 160 – 183 HB (derived: Rm / 3.38)<br>≈ 83 – 90 HRB (ASTM A370 conversion) | EN 10263-2 / EN 10263-4 (steel rod & wire for cold heading) |
| **34CRMO4**<br><sub>34CrMo4 / 1.7220 Cr-Mo Steel (EN 10263-4)</sub> | carbon alloy steel | 698 – 718 °C (just below A1; baseline A1 = 723 °C) | Coil-mass driven — set from furnace load, not part Ø | Slow furnace cool through the transformation range, then still air | 580 – 660 MPa (Spheroidized Annealed Wire — Rm estimated from 4135) | 172 – 195 HB (derived: Rm / 3.38)<br>≈ 86 – 92 HRB (ASTM A370 conversion) | EN 10263-2 / EN 10263-4 (steel rod & wire for cold heading) |
| **35NCD16**<br><sub>35NCD16 / EN 36NiCrMo16 (AFNOR Aero High-Strength)</sub> | carbon alloy steel | 698 – 718 °C (just below A1; baseline A1 = 723 °C) | Coil-mass driven — set from furnace load, not part Ø | Slow furnace cool through the transformation range, then still air | 680 – 760 MPa (Soft Annealed Wire) | 201 – 225 HB (derived: Rm / 3.38) | EN 10263-2 / EN 10263-4 (steel rod & wire for cold heading) |
| **36CRB4**<br><sub>36CrB4 / 1.7077 Cr-B Boron Steel (EN 10263-4)</sub> | carbon alloy steel | 698 – 718 °C (just below A1; baseline A1 = 723 °C) | Coil-mass driven — set from furnace load, not part Ø | Slow furnace cool through the transformation range, then still air | 540 – 620 MPa (Spheroidized Annealed Wire — Rm estimated from AISI_51B37) | 160 – 183 HB (derived: Rm / 3.38)<br>≈ 83 – 90 HRB (ASTM A370 conversion) | EN 10263-2 / EN 10263-4 (steel rod & wire for cold heading) |
| **4135**<br><sub>AISI 4135 / 35CrMo Alloy</sub> | carbon alloy steel | 698 – 718 °C (just below A1; baseline A1 = 723 °C) | Coil-mass driven — set from furnace load, not part Ø | Slow furnace cool through the transformation range, then still air | 580 – 660 MPa (Annealed / Spheroidized Wire) | 172 – 195 HB (derived: Rm / 3.38)<br>≈ 86 – 92 HRB (ASTM A370 conversion) | EN 10263-2 / EN 10263-4 (steel rod & wire for cold heading) |
| **4140**<br><sub>AISI 4140 / EN24 Alloy</sub> | carbon alloy steel | 698 – 718 °C (just below A1; baseline A1 = 723 °C) | Coil-mass driven — set from furnace load, not part Ø | Slow furnace cool through the transformation range, then still air | 650 – 730 MPa (Spheroidized Wire Coil) | 192 – 216 HB (derived: Rm / 3.38)<br>≈ 91 – 96 HRB (ASTM A370 conversion) | EN 10263-2 / EN 10263-4 (steel rod & wire for cold heading) |
| **41CR4**<br><sub>41Cr4 / 1.7035 / SAE 5140 Cr Steel (EN 10263-4)</sub> | carbon alloy steel | 698 – 718 °C (just below A1; baseline A1 = 723 °C) | Coil-mass driven — set from furnace load, not part Ø | Slow furnace cool through the transformation range, then still air | 650 – 730 MPa (Spheroidized Annealed Wire — Rm estimated from 4140) | 192 – 216 HB (derived: Rm / 3.38)<br>≈ 91 – 96 HRB (ASTM A370 conversion) | EN 10263-2 / EN 10263-4 (steel rod & wire for cold heading) |
| **42CRMO4**<br><sub>42CrMo4 / EN19 (ISO 898-1 10.9)</sub> | carbon alloy steel | 698 – 718 °C (just below A1; baseline A1 = 723 °C) | Coil-mass driven — set from furnace load, not part Ø | Slow furnace cool through the transformation range, then still air | 580 – 660 MPa (Spheroidized Wire) | 172 – 195 HB (derived: Rm / 3.38)<br>≈ 86 – 92 HRB (ASTM A370 conversion) | EN 10263-2 / EN 10263-4 (steel rod & wire for cold heading) |
| **4340**<br><sub>AISI 4340 / 35NCD16</sub> | carbon alloy steel | 698 – 718 °C (just below A1; baseline A1 = 723 °C) | Coil-mass driven — set from furnace load, not part Ø | Slow furnace cool through the transformation range, then still air | 720 – 800 MPa (Vacuum Degassed VAR Wire) | 213 – 237 HB (derived: Rm / 3.38) | EN 10263-2 / EN 10263-4 (steel rod & wire for cold heading) |
| **8740**<br><sub>AISI 8740 Aircraft Alloy (AMS 6322)</sub> | carbon alloy steel | 698 – 718 °C (just below A1; baseline A1 = 723 °C) | Coil-mass driven — set from furnace load, not part Ø | Slow furnace cool through the transformation range, then still air | 600 – 700 MPa (Spheroidized Annealed Wire) | 178 – 207 HB (derived: Rm / 3.38)<br>≈ 88 – 94 HRB (ASTM A370 conversion) | EN 10263-2 / EN 10263-4 (steel rod & wire for cold heading) |
| **AISI_4130**<br><sub>AISI 4130 / EN 25CrMo4 (AMS 6370)</sub> | carbon alloy steel | 698 – 718 °C (just below A1; baseline A1 = 723 °C) | Coil-mass driven — set from furnace load, not part Ø | Slow furnace cool through the transformation range, then still air | 540 – 620 MPa (Annealed Wire) | 160 – 183 HB (derived: Rm / 3.38)<br>≈ 83 – 90 HRB (ASTM A370 conversion) | EN 10263-2 / EN 10263-4 (steel rod & wire for cold heading) |
| **AISI_51B37**<br><sub>AISI 51B37 / EN 38CrB4 Boron Steel</sub> | carbon alloy steel | 698 – 718 °C (just below A1; baseline A1 = 723 °C) | Coil-mass driven — set from furnace load, not part Ø | Slow furnace cool through the transformation range, then still air | 540 – 620 MPa (Spheroidized Wire) | 160 – 183 HB (derived: Rm / 3.38)<br>≈ 83 – 90 HRB (ASTM A370 conversion) | EN 10263-2 / EN 10263-4 (steel rod & wire for cold heading) |
| **EN24**<br><sub>EN24 / 34CrNiMo6 (BS 970 817M40)</sub> | carbon alloy steel | 698 – 718 °C (just below A1; baseline A1 = 723 °C) | Coil-mass driven — set from furnace load, not part Ø | Slow furnace cool through the transformation range, then still air | 620 – 700 MPa (Spheroidized Annealed Wire) | 183 – 207 HB (derived: Rm / 3.38)<br>≈ 90 – 94 HRB (ASTM A370 conversion) | EN 10263-2 / EN 10263-4 (steel rod & wire for cold heading) |
| **EN24_ESR**<br><sub>EN24 ESR Pure (Electro-Slag Remelted 34CrNiMo6)</sub> | carbon alloy steel | 698 – 718 °C (just below A1; baseline A1 = 723 °C) | Coil-mass driven — set from furnace load, not part Ø | Slow furnace cool through the transformation range, then still air | 600 – 680 MPa (Ultra-Clean ESR Wire) | 178 – 201 HB (derived: Rm / 3.38)<br>≈ 88 – 93 HRB (ASTM A370 conversion) | EN 10263-2 / EN 10263-4 (steel rod & wire for cold heading) |
| **H11_H13**<br><sub>H11 / H13 Tool Steel (AMS 6487)</sub> | carbon alloy steel | 698 – 718 °C (just below A1; baseline A1 = 723 °C) | Coil-mass driven — set from furnace load, not part Ø | Slow furnace cool through the transformation range, then still air | 720 – 820 MPa (Annealed Wire Coil) | 213 – 243 HB (derived: Rm / 3.38) | EN 10263-2 / EN 10263-4 (steel rod & wire for cold heading) |
| **SCM435**<br><sub>SCM435 Cr-Mo Steel (JIS G4053)</sub> | carbon alloy steel | 698 – 718 °C (just below A1; baseline A1 = 723 °C) | Coil-mass driven — set from furnace load, not part Ø | Slow furnace cool through the transformation range, then still air | 580 – 660 MPa (Spheroidized Annealed Wire — Rm estimated from 4135) | 172 – 195 HB (derived: Rm / 3.38)<br>≈ 86 – 92 HRB (ASTM A370 conversion) | EN 10263-2 / EN 10263-4 (steel rod & wire for cold heading) |

---

## Solution anneal + rapid cool

*Dissolve carbides and restore ductile austenite; rapid cool prevents sensitisation*

| Grade | Family | Temperature | Soak | Cooling | Target Rm (annealed) | Target hardness | Standard |
|---|---|---|---|---|---|---|---|
| **12KH18N10T**<br><sub>12KH18N10T / AISI 321 Stainless (GOST 5632 Ti-Stabilized)</sub> | stainless | 1020°C – 1080°C Solution Annealed | Hold until fully solutioned, then cool without delay | Rapid — water quench or forced gas; slow cooling sensitises the grain boundaries | 510 – 620 MPa (Solution Annealed) | 151 – 183 HB (derived: Rm / 3.38)<br>≈ 79 – 90 HRB (ASTM A370 conversion) | GOST 5632 12Х18Н10Т / AISI 321 |
| **13_8PH**<br><sub>13-8 Mo PH Stainless (AMS 5629)</sub> | stainless | Condition A Solution: 927°C ± 10°C | Hold until fully solutioned, then cool without delay | Rapid — water quench or forced gas; slow cooling sensitises the grain boundaries | 900 – 1000 MPa (Condition A Wire) | 266 – 296 HB (derived: Rm / 3.38) | AMS 5629 / AMS 5864 / AMS 2759/3 |
| **14KH17N2**<br><sub>14KH17N2 / AISI 431 Stainless (GOST 5632 14Х17Н2)</sub> | stainless | 980°C – 1020°C | Hold until fully solutioned, then cool without delay | Rapid — water quench or forced gas; slow cooling sensitises the grain boundaries | 680 – 780 MPa (Annealed Wire) | 201 – 231 HB (derived: Rm / 3.38) | GOST 5632 14Х17Н2 / AISI 431 |
| **15_5PH**<br><sub>15-5PH AMS 5659 Stainless</sub> | stainless | 1038°C ± 10°C (Condition A Solution) | Hold until fully solutioned, then cool without delay | Rapid — water quench or forced gas; slow cooling sensitises the grain boundaries | 950 – 1050 MPa (Condition A Solution Treated) | 281 – 311 HB (derived: Rm / 3.38) | AMS 5659 / AMS 5862 / AMS 2759/3 |
| **17_4PH**<br><sub>17-4PH AMS 5643 SS</sub> | stainless | Condition A Solution: 1040°C ± 15°C | Hold until fully solutioned, then cool without delay | Rapid — water quench or forced gas; slow cooling sensitises the grain boundaries | 950 – 1050 MPa (Condition A Solution Annealed) | 281 – 311 HB (derived: Rm / 3.38) | AMS 5643 / AMS 2759/3 |
| **20X13**<br><sub>20X13 / 20Kh13 / AISI 420 Martensitic (GOST 5632)</sub> | stainless | 980°C – 1020°C | Hold until fully solutioned, then cool without delay | Rapid — water quench or forced gas; slow cooling sensitises the grain boundaries | 500 – 600 MPa (Annealed Wire) | 148 – 178 HB (derived: Rm / 3.38)<br>≈ 78 – 88 HRB (ASTM A370 conversion) | GOST 5632 20Х13 / AISI 420 |
| **30X13**<br><sub>30X13 / 30Kh13 / AISI 420B Martensitic (GOST 5632)</sub> | stainless | 1000°C – 1020°C | Hold until fully solutioned, then cool without delay | Rapid — water quench or forced gas; slow cooling sensitises the grain boundaries | 560 – 660 MPa (Annealed Wire) | 166 – 195 HB (derived: Rm / 3.38)<br>≈ 85 – 92 HRB (ASTM A370 conversion) | GOST 5632 30Х13 / AISI 420B |
| **AM350**<br><sub>AM-350 / AM-355 (AMS 5548/5745)</sub> | stainless | 1050°C ± 10°C (Condition H Solution) | Hold until fully solutioned, then cool without delay | Rapid — water quench or forced gas; slow cooling sensitises the grain boundaries | 850 – 950 MPa (Annealed Condition H) | 251 – 281 HB (derived: Rm / 3.38) | AMS 5548 / AMS 5745 |
| **CUSTOM_455**<br><sub>Custom 455 / 465 (AMS 5617/5936)</sub> | stainless | 830°C – 845°C (Solution Anneal Condition A) | Hold until fully solutioned, then cool without delay | Rapid — water quench or forced gas; slow cooling sensitises the grain boundaries | 900 – 1020 MPa (Condition A Wire) | 266 – 302 HB (derived: Rm / 3.38) | AMS 5617 / AMS 5936 / AMS 2759/3 |
| **NITRONIC60**<br><sub>Nitronic 60 (AMS 5848 / S21800)</sub> | stainless | 1065°C ± 15°C | Hold until fully solutioned, then cool without delay | Rapid — water quench or forced gas; slow cooling sensitises the grain boundaries | 700 – 780 MPa (Annealed Wire) | 207 – 231 HB (derived: Rm / 3.38) | AMS 5848 / ASTM A276 / UNS S21800 |
| **SS204**<br><sub>SS204Cu Austenitic SS</sub> | stainless | 1020°C – 1050°C (Solution Anneal) | Hold until fully solutioned, then cool without delay | Rapid — water quench or forced gas; slow cooling sensitises the grain boundaries | 650 – 720 MPa (Soft Annealed Wire) | 192 – 213 HB (derived: Rm / 3.38)<br>≈ 91 – 96 HRB (ASTM A370 conversion) | ASTM A493 / ISO 3506-1 |
| **SS304**<br><sub>SS304 / A2-70 Stainless</sub> | stainless | 1040°C – 1080°C (Carbide Solution) | Hold until fully solutioned, then cool without delay | Rapid — water quench or forced gas; slow cooling sensitises the grain boundaries | 550 – 650 MPa (Bright Annealed Wire) | 163 – 192 HB (derived: Rm / 3.38)<br>≈ 84 – 91 HRB (ASTM A370 conversion) | ISO 3506-1 Class A2-70 / ASTM F593 |
| **SS316**<br><sub>SS316 / A4-80 Stainless</sub> | stainless | 1060°C – 1100°C | Hold until fully solutioned, then cool without delay | Rapid — water quench or forced gas; slow cooling sensitises the grain boundaries | 580 – 680 MPa (Annealed Wire Coil) | 172 – 201 HB (derived: Rm / 3.38)<br>≈ 86 – 93 HRB (ASTM A370 conversion) | ISO 3506-1 Class A4-80 / ASTM F593 Group 2 |
| **SS316L**<br><sub>SS316L Low Carbon Stainless</sub> | stainless | 1050°C – 1080°C | Hold until fully solutioned, then cool without delay | Rapid — water quench or forced gas; slow cooling sensitises the grain boundaries | 520 – 600 MPa (Annealed Wire Coil) | 154 – 178 HB (derived: Rm / 3.38)<br>≈ 81 – 88 HRB (ASTM A370 conversion) | ISO 3506-1 Class A4-70 / ASTM F593 |
| **SS410**<br><sub>SS410 Martensitic SS</sub> | stainless | 970°C – 1000°C | Hold until fully solutioned, then cool without delay | Rapid — water quench or forced gas; slow cooling sensitises the grain boundaries | 520 – 600 MPa (Annealed Wire) | 154 – 178 HB (derived: Rm / 3.38)<br>≈ 81 – 88 HRB (ASTM A370 conversion) | ASTM A493 / ISO 3506-1 Class C1 |

---

## Full anneal to O temper

*Fully soften to the annealed (O) condition*

| Grade | Family | Temperature | Soak | Cooling | Target Rm (annealed) | Target hardness | Standard |
|---|---|---|---|---|---|---|---|
| **AL2024**<br><sub>Alloy 2024-T4 / T351 (AMS 4120)</sub> | aluminum | **ENTER PER GRADE** | **ENTER PER GRADE** | Controlled slow cool to the fully soft condition | 280 – 340 MPa (Condition O Annealed Wire) | **ENTER PER GRADE** | Supplier datasheet |
| **AL2117**<br><sub>Al 2117-T4 Rivet Alloy (AD)</sub> | aluminum | **ENTER PER GRADE** | **ENTER PER GRADE** | Controlled slow cool to the fully soft condition | Supplied T4 — driven as received (no anneal) | **ENTER PER GRADE** | Supplier datasheet |
| **AL2219**<br><sub>Alloy 2219-T6 / T851 (AMS 4162)</sub> | aluminum | **ENTER PER GRADE** | **ENTER PER GRADE** | Controlled slow cool to the fully soft condition | 260 – 320 MPa (Condition O Annealed Wire) | **ENTER PER GRADE** | Supplier datasheet |
| **AL5056**<br><sub>Al 5056-H32 Rivet Alloy (B)</sub> | aluminum | **ENTER PER GRADE** | **ENTER PER GRADE** | Controlled slow cool to the fully soft condition | Supplied H32 — used as fabricated (no anneal) | **ENTER PER GRADE** | Supplier datasheet |
| **AL6061**<br><sub>Alloy 6061-T6 Structural Al (AMS 4117)</sub> | aluminum | **ENTER PER GRADE** | **ENTER PER GRADE** | Controlled slow cool to the fully soft condition | 180 – 220 MPa (Condition O Wire) | **ENTER PER GRADE** | Supplier datasheet |
| **AL7050**<br><sub>Al 7050-T73 Rivet / Fastener Alloy</sub> | aluminum | **ENTER PER GRADE** | **ENTER PER GRADE** | Controlled slow cool to the fully soft condition | Supplied T73 — driven as received (no anneal) | **ENTER PER GRADE** | Supplier datasheet |
| **AL7075**<br><sub>Alloy 7075-T6 / T73 (AMS 4122)</sub> | aluminum | **ENTER PER GRADE** | **ENTER PER GRADE** | Controlled slow cool to the fully soft condition | 320 – 380 MPa (Condition O Wire Coil) | **ENTER PER GRADE** | Supplier datasheet |
| **CUBE2**<br><sub>Beryllium Copper CuBe2 / C17200 (AMS 4650)</sub> | copper alloy | **ENTER PER GRADE** | **ENTER PER GRADE** | Controlled slow cool to the fully soft condition | 450 – 550 MPa (Condition A Wire) | **ENTER PER GRADE** | Supplier datasheet |

---

## Mill-annealed — supplied ready to head

*Wire is bought in the annealed / Condition A state; no in-house anneal*

| Grade | Family | Temperature | Soak | Cooling | Target Rm (annealed) | Target hardness | Standard |
|---|---|---|---|---|---|---|---|
| **10KH11N23T3MR**<br><sub>10KH11N23T3MR / EP33 (GOST ЭП33 Aero Superalloy)</sub> | nickel superalloy | ≈ 1000 – 1050 °C (unconfirmed) | **ENTER PER GRADE** | Air | 680 – 780 MPa (Sol. Annealed Wire) | **ENTER PER GRADE** | GOST ЭП33 / OST 1 90005 |
| **A286**<br><sub>A286 Superalloy (AMS 5731)</sub> | nickel superalloy | 982 °C (1800 °F); 899 °C (1650 °F) for more ductility | 1 h | Water or oil quench | 650 – 750 MPa (Solution Annealed Bar/Wire) | **ENTER PER GRADE** | AMS 5731 / AMS 5732 / NAS624 / MS21250 |
| **HASTELLOY_C276**<br><sub>Hastelloy C-276 (AMS 5750)</sub> | nickel superalloy | 1121 °C (2050 °F) | **ENTER PER GRADE** | Water quench (fast air only below 10 mm) | 750 – 820 MPa | **ENTER PER GRADE** | AMS 5750 / ASTM B574 / UNS N10276 |
| **INCONEL625**<br><sub>Inconel 625 (AMS 5666 / N06625)</sub> | nickel superalloy | 982 °C typical (Grade 1, 871 °C min) | **ENTER PER GRADE** | Air cool | 880 – 960 MPa (Annealed Wire) | **ENTER PER GRADE** | AMS 5666 / ASTM B446 / UNS N06625 |
| **INCONEL718**<br><sub>Inconel 718 (AMS 5662)</sub> | nickel superalloy | 927 – 1010 °C (1700 – 1850 °F), 980 °C typical | **ENTER PER GRADE** | Air cool or faster | 920 – 1020 MPa (AMS 5662 Annealed Wire) | **ENTER PER GRADE** | AMS 5662 / AMS 5664 / AMS 2759/3 / NAS624 |
| **INCONEL_600**<br><sub>Inconel 600 / UNS N06600 (AMS 5665 Ni-Cr-Fe)</sub> | nickel superalloy | 1010 °C (1850 °F) | 15 min | **ENTER PER GRADE** | 550 – 680 MPa (Annealed Wire) | **ENTER PER GRADE** | AMS 5665 / UNS N06600 |
| **INCONEL_X750**<br><sub>Inconel X-750 / UNS N07750 (AMS 5667 / 5698 / 5699)</sub> | nickel superalloy | 982 °C ± 14 °C (1800 °F) | **ENTER PER GRADE** | Air cool or faster | Rm not published for headable stock (Solution Annealed) | **ENTER PER GRADE** | AMS 5667 / AMS 5698 / AMS 5699 |
| **MONEL_K500**<br><sub>Monel K-500 (QQ-N-286 / N05500)</sub> | nickel superalloy | 982 – 1038 °C (1800 – 1900 °F) | **ENTER PER GRADE** | Water quench | 680 – 780 MPa (Annealed Wire) | **ENTER PER GRADE** | QQ-N-286 / AMS 4676 / UNS N05500 |
| **MP159**<br><sub>MP159 Multiphase Co-Ni (AMS 5842 / AMS 5843)</sub> | nickel superalloy | 1038 – 1052 °C (1900 – 1925 °F) | 4 – 8 h | Water quench | Rm not published for headable stock (Solution Treated, AMS 5842) | **ENTER PER GRADE** | AMS 5842 / AMS 5843 |
| **RENE41**<br><sub>René 41 Superalloy (AMS 5545/5712)</sub> | nickel superalloy | 1066 – 1080 °C (1950 – 1975 °F) | 4 h | Air cool | 920 – 1020 MPa (Solution Annealed Bar) | **ENTER PER GRADE** | AMS 5545 / AMS 5712 / UNS N07041 |
| **TI_3AL25V**<br><sub>Ti-3Al-2.5V Grade 9 (AMS 4943 – 4946)</sub> | titanium | N/A — supplied annealed | N/A | N/A | Rm not published for headable stock (Annealed) | **ENTER PER GRADE** | AMS 4943 – 4946 / ASTM B265 Gr 9 |
| **TI_6242**<br><sub>Ti-6Al-2Sn-4Zr-2Mo (AMS 4975/4976)</sub> | titanium | 704 – 843 °C (1300 – 1550 °F) | **ENTER PER GRADE** | Air cool | 880 – 940 MPa (Annealed Wire, H2 < 125 ppm) | **ENTER PER GRADE** | AMS 4975 / AMS 4976 / AMS 2801 |
| **TI_BETA_C**<br><sub>Beta-C Titanium Ti-3Al-8V-6Cr-4Mo-4Zr (AMS 4957 / 4958)</sub> | titanium | 788 – 927 °C (1450 – 1700 °F) solution treat | **ENTER PER GRADE** | **ENTER PER GRADE** | Rm not published for headable stock (Solution Treated Wire, AMS 4957) | **ENTER PER GRADE** | AMS 4957 / AMS 4958 |
| **TI_GR1**<br><sub>Titanium Grade 1 / 2 CP</sub> | titanium | 649 – 760 °C (1200 – 1400 °F) | 6 min – 2 h | Air cool | 420 – 520 MPa (CP Grade 2 Wire) | **ENTER PER GRADE** | ASTM B348 / AMS 4921 |
| **TI_GR23**<br><sub>Titanium Gr 23 Ti-6Al-4V ELI (AMS 4930)</sub> | titanium | 704 – 732 °C (1300 – 1350 °F) | 1 – 8 h | Air cool | 860 – 920 MPa (ELI Annealed Wire, H2 < 80 ppm) | **ENTER PER GRADE** | AMS 4930 / ASTM F136 / AMS 2759/4 |
| **TI_GR5**<br><sub>Titanium Gr 5 (Ti-6Al-4V)</sub> | titanium | 704 – 788 °C (1300 – 1450 °F) | 2 h | Air cool | 890 – 960 MPa (Annealed Wire, max H2: 125 ppm) | **ENTER PER GRADE** | AMS 4928 / AMS 4967 / AMS 2759/4 |
| **VT16**<br><sub>VT-16 Titanium Alloy (GOST ВТ16 Ti-3Al-5Mo-4.5V)</sub> | titanium | 770 – 790 °C | 2 h | Furnace cool 1 – 5 °C/min to 500 °C, then air | 750 – 850 MPa (Annealed α+β Wire) | **ENTER PER GRADE** | GOST ВТ16 / OST 1 90013 |
| **WASPALOY**<br><sub>Waspaloy (AMS 5708)</sub> | nickel superalloy | 1049 °C (1920 °F) | 4 h | Air cool | 850 – 950 MPa (Solution Treated Wire) | **ENTER PER GRADE** | AMS 5708 / UNS N07001 |

---

## Anneal required — wire supplied hard

*Catalogue condition is cold drawn / half-hard, which is not a headable state*

| Grade | Family | Temperature | Soak | Cooling | Target Rm (annealed) | Target hardness | Standard |
|---|---|---|---|---|---|---|---|
| **1018**<br><sub>Low Carbon (AISI 1018/1020)</sub> | carbon alloy steel | **ENTER PER GRADE** | **ENTER PER GRADE** | **ENTER PER GRADE** | 440 – 520 MPa (Cold Drawn Rod) | 130 – 154 HB (derived: Rm / 3.38)<br>≈ 71 – 81 HRB (ASTM A370 conversion) | EN 10263 |
| **BRASS_C360**<br><sub>Free-Cutting Brass C36000 (ASTM B16)</sub> | copper alloy | **ENTER PER GRADE** | **ENTER PER GRADE** | **ENTER PER GRADE** | 380 – 420 MPa (Half-Hard Rod) | **ENTER PER GRADE** | EN 10263 |
| **MP35N**<br><sub>MP35N Multiphase (AMS 5844/5758)</sub> | nickel superalloy | **ENTER PER GRADE** | **ENTER PER GRADE** | **ENTER PER GRADE** | 1150 – 1300 MPa (Cold Drawn Fastener Wire) | **ENTER PER GRADE** | EN 10263 |

---

## Aerospace grades — formability reference

Low Z and low spm mean the grade work-hardens fast and cannot take many blows between anneals.

The last column is the anneal for re-annealing between blows. For mill-annealed grades it is the
researched mill anneal (sources in MILL_ANNEAL_CYCLES.md) — not the catalogue's solution-treat
temperature, which for titanium is ~200 °C hotter and is a hardening step.

| Grade | Route | Z % | n | spm | Anneal between blows |
|---|---|---:|---:|---:|---|
| **TI_BETA_C** | Mill-annealed — supplied ready to head | 15.0 | 0.1 | 45 | 788 – 927 °C (1450 – 1700 °F) solution treat |
| **RENE41** | Mill-annealed — supplied ready to head | 18.0 | 0.13 | 25 | 1066 – 1080 °C (1950 – 1975 °F) |
| **WASPALOY** | Mill-annealed — supplied ready to head | 20.0 | 0.14 | 30 | 1049 °C (1920 °F) |
| **INCONEL718** | Mill-annealed — supplied ready to head | 22.0 | 0.13 | 35 | 927 – 1010 °C (1700 – 1850 °F), 980 °C typical |
| **INCONEL_X750** | Mill-annealed — supplied ready to head | 22.0 | 0.13 | 35 | 982 °C ± 14 °C (1800 °F) |
| **TI_GR5** | Mill-annealed — supplied ready to head | 25.0 | 0.1 | 45 | 704 – 788 °C (1300 – 1450 °F) |
| **MP35N** | Anneal required — wire supplied hard | 25.0 | 0.11 | 20 | **ENTER PER GRADE** <sub>(catalogue says: Work Hardened Cold Drawn Matrix)</sub> |
| **CUBE2** | Full anneal to O temper | 25.0 | 0.14 | 65 | **ENTER PER GRADE** <sub>(catalogue says: Solution Heat Treat: 780°C – 800°C)</sub> |
| **MP159** | Mill-annealed — supplied ready to head | 25.0 | 0.11 | 20 | 1038 – 1052 °C (1900 – 1925 °F) |
| **TI_6242** | Mill-annealed — supplied ready to head | 28.0 | 0.11 | 40 | 704 – 843 °C (1300 – 1550 °F) |
| **CUSTOM_455** | Solution anneal + rapid cool | 32.0 | 0.1 | 50 | 830°C – 845°C (Solution Anneal Condition A) |
| **AL7075** | Full anneal to O temper | 32.0 | 0.16 | 150 | **ENTER PER GRADE** <sub>(catalogue says: Solution Heat Treat: 470°C ± 5°C)</sub> |
| **AL7050** | Full anneal to O temper | 32.0 | 0.16 | 150 | **ENTER PER GRADE** <sub>(catalogue says: Solution 477°C (890°F); 466 – 471°C allowed in some specs)</sub> |
| **A286** | Mill-annealed — supplied ready to head | 35.0 | 0.2 | 50 | 982 °C (1800 °F); 899 °C (1650 °F) for more ductility |
| **H11_H13** | Sub-critical spheroidize anneal | 35.0 | 0.12 | 65 | 698 – 718 °C (just below A1; baseline A1 = 723 °C) |
| **AL2219** | Full anneal to O temper | 35.0 | 0.18 | 155 | **ENTER PER GRADE** <sub>(catalogue says: Solution Heat Treat: 535°C ± 5°C)</sub> |
| **MONEL_K500** | Mill-annealed — supplied ready to head | 35.0 | 0.18 | 45 | 982 – 1038 °C (1800 – 1900 °F) |
| **TI_GR23** | Mill-annealed — supplied ready to head | 35.0 | 0.1 | 50 | 704 – 732 °C (1300 – 1350 °F) |
| **TI_3AL25V** | Mill-annealed — supplied ready to head | 35.0 | 0.1 | 50 | **ENTER PER GRADE** <sub>(catalogue says: Anneal per AMS 4943 – 4946 / mill cert (not in public sources); β-transus 935°C)</sub> |
| **300M** | Sub-critical spheroidize anneal | 38.0 | 0.11 | 60 | 698 – 718 °C (just below A1; baseline A1 = 723 °C) |
| **AM350** | Solution anneal + rapid cool | 38.0 | 0.12 | 60 | 1050°C ± 10°C (Condition H Solution) |
| **17_4PH** | Solution anneal + rapid cool | 40.0 | 0.11 | 65 | Condition A Solution: 1040°C ± 15°C |
| **TI_GR1** | Mill-annealed — supplied ready to head | 40.0 | 0.18 | 70 | 649 – 760 °C (1200 – 1400 °F) |
| **AL2024** | Full anneal to O temper | 40.0 | 0.2 | 160 | **ENTER PER GRADE** <sub>(catalogue says: Solution Heat Treat: 493°C ± 5°C)</sub> |
| **INCONEL625** | Mill-annealed — supplied ready to head | 40.0 | 0.22 | 35 | 982 °C typical (Grade 1, 871 °C min) |
| **AL2117** | Full anneal to O temper | 40.0 | 0.2 | 160 | **ENTER PER GRADE** <sub>(catalogue says: Solution heat treat — temperature per AMS / MIL spec (not found in public sources))</sub> |
| **AL5056** | Full anneal to O temper | 40.0 | 0.19 | 180 | **ENTER PER GRADE** <sub>(catalogue says: N/A — non-heat-treatable (5xxx))</sub> |
| **4340** | Sub-critical spheroidize anneal | 45.0 | 0.12 | 80 | 698 – 718 °C (just below A1; baseline A1 = 723 °C) |
| **15_5PH** | Solution anneal + rapid cool | 45.0 | 0.11 | 65 | 1038°C ± 10°C (Condition A Solution) |
| **13_8PH** | Solution anneal + rapid cool | 45.0 | 0.11 | 55 | Condition A Solution: 927°C ± 10°C |
| **35NCD16** | Sub-critical spheroidize anneal | 45.0 | 0.15 | 85 | 698 – 718 °C (just below A1; baseline A1 = 723 °C) |
| **10KH11N23T3MR** | Mill-annealed — supplied ready to head | 45.0 | 0.32 | 75 | ≈ 1000 – 1050 °C (unconfirmed) |
| **8740** | Sub-critical spheroidize anneal | 48.0 | 0.14 | 95 | 698 – 718 °C (just below A1; baseline A1 = 723 °C) |
| **30KHGSA** | Sub-critical spheroidize anneal | 48.0 | 0.16 | 95 | 698 – 718 °C (just below A1; baseline A1 = 723 °C) |
| **14KH17N2** | Solution anneal + rapid cool | 48.0 | 0.17 | 100 | 980°C – 1020°C |
| **30X13** | Solution anneal + rapid cool | 48.0 | 0.18 | 105 | 1000°C – 1020°C |
| **NITRONIC60** | Solution anneal + rapid cool | 50.0 | 0.28 | 55 | 1065°C ± 15°C |
| **HASTELLOY_C276** | Mill-annealed — supplied ready to head | 50.0 | 0.25 | 30 | 1121 °C (2050 °F) |
| **16KHGSN** | Sub-critical spheroidize anneal | 50.0 | 0.18 | 105 | 698 – 718 °C (just below A1; baseline A1 = 723 °C) |
| **VT16** | Mill-annealed — supplied ready to head | 50.0 | 0.12 | 80 | 770 – 790 °C |
| **INCONEL_600** | Mill-annealed — supplied ready to head | 50.0 | 0.38 | 80 | 1010 °C (1850 °F) |
| **EN24_ESR** | Sub-critical spheroidize anneal | 55.0 | 0.17 | 100 | 698 – 718 °C (just below A1; baseline A1 = 723 °C) |
| **AISI_4130** | Sub-critical spheroidize anneal | 55.0 | 0.2 | 135 | 698 – 718 °C (just below A1; baseline A1 = 723 °C) |
| **12KH18N10T** | Solution anneal + rapid cool | 60.0 | 0.42 | 120 | 1020°C – 1080°C Solution Annealed |

---

## Gaps — values the catalogue cannot supply

61 of 69 grades have at least one value to fill in.

| Grade | Missing |
|---|---|
| **1008** | • Exact A1 for this grade<br>• Soak hours — driven by coil mass and furnace load, not part diameter |
| **1018** | • Anneal cycle — none in the catalogue, because this grade is listed in the hard condition |
| **1022** | • Exact A1 for this grade<br>• Soak hours — driven by coil mass and furnace load, not part diameter |
| **10B21** | • Exact A1 for this grade<br>• Soak hours — driven by coil mass and furnace load, not part diameter |
| **10KH11N23T3MR** | • Target hardness — no steel Rm/HB correlation for this family; enter per grade<br>• Hold time — not stated in the source<br>• Confirm the anneal cycle against the GOST / TU for this grade |
| **13_8PH** | • Target hardness in HRB — derived HB falls outside the A370 HRB range |
| **14KH17N2** | • Target hardness in HRB — derived HB falls outside the A370 HRB range |
| **15_5PH** | • Target hardness in HRB — derived HB falls outside the A370 HRB range |
| **16KHGSN** | • Exact A1 for this grade<br>• Soak hours — driven by coil mass and furnace load, not part diameter |
| **17_4PH** | • Target hardness in HRB — derived HB falls outside the A370 HRB range |
| **18CRMO4** | • Exact A1 for this grade<br>• Soak hours — driven by coil mass and furnace load, not part diameter |
| **20MNB4** | • Exact A1 for this grade<br>• Soak hours — driven by coil mass and furnace load, not part diameter |
| **23MNB4** | • Exact A1 for this grade<br>• Soak hours — driven by coil mass and furnace load, not part diameter |
| **300M** | • Target hardness in HRB — derived HB falls outside the A370 HRB range<br>• Exact A1 for this grade<br>• Soak hours — driven by coil mass and furnace load, not part diameter |
| **30KHGSA** | • Exact A1 for this grade<br>• Soak hours — driven by coil mass and furnace load, not part diameter |
| **32CRB4** | • Exact A1 for this grade<br>• Soak hours — driven by coil mass and furnace load, not part diameter |
| **34CRMO4** | • Exact A1 for this grade<br>• Soak hours — driven by coil mass and furnace load, not part diameter |
| **35NCD16** | • Target hardness in HRB — derived HB falls outside the A370 HRB range<br>• Exact A1 for this grade<br>• Soak hours — driven by coil mass and furnace load, not part diameter |
| **36CRB4** | • Exact A1 for this grade<br>• Soak hours — driven by coil mass and furnace load, not part diameter |
| **4135** | • Exact A1 for this grade<br>• Soak hours — driven by coil mass and furnace load, not part diameter |
| **4140** | • Exact A1 for this grade<br>• Soak hours — driven by coil mass and furnace load, not part diameter |
| **41CR4** | • Exact A1 for this grade<br>• Soak hours — driven by coil mass and furnace load, not part diameter |
| **42CRMO4** | • Exact A1 for this grade<br>• Soak hours — driven by coil mass and furnace load, not part diameter |
| **4340** | • Target hardness in HRB — derived HB falls outside the A370 HRB range<br>• Exact A1 for this grade<br>• Soak hours — driven by coil mass and furnace load, not part diameter |
| **8740** | • Exact A1 for this grade<br>• Soak hours — driven by coil mass and furnace load, not part diameter |
| **A286** | • Target hardness — no steel Rm/HB correlation for this family; enter per grade |
| **AISI_4130** | • Exact A1 for this grade<br>• Soak hours — driven by coil mass and furnace load, not part diameter |
| **AISI_51B37** | • Exact A1 for this grade<br>• Soak hours — driven by coil mass and furnace load, not part diameter |
| **AL2024** | • Target hardness — no steel Rm/HB correlation for this family; enter per grade<br>• Anneal temperature and soak — not in the catalogue for this family |
| **AL2117** | • Target hardness — no steel Rm/HB correlation for this family; enter per grade<br>• Anneal temperature and soak — not in the catalogue for this family |
| **AL2219** | • Target hardness — no steel Rm/HB correlation for this family; enter per grade<br>• Anneal temperature and soak — not in the catalogue for this family |
| **AL5056** | • Target hardness — no steel Rm/HB correlation for this family; enter per grade<br>• Anneal temperature and soak — not in the catalogue for this family |
| **AL6061** | • Target hardness — no steel Rm/HB correlation for this family; enter per grade<br>• Anneal temperature and soak — not in the catalogue for this family |
| **AL7050** | • Target hardness — no steel Rm/HB correlation for this family; enter per grade<br>• Anneal temperature and soak — not in the catalogue for this family |
| **AL7075** | • Target hardness — no steel Rm/HB correlation for this family; enter per grade<br>• Anneal temperature and soak — not in the catalogue for this family |
| **AM350** | • Target hardness in HRB — derived HB falls outside the A370 HRB range |
| **BRASS_C360** | • Target hardness — no steel Rm/HB correlation for this family; enter per grade<br>• Anneal cycle — none in the catalogue, because this grade is listed in the hard condition |
| **CUBE2** | • Target hardness — no steel Rm/HB correlation for this family; enter per grade<br>• Anneal temperature and soak — not in the catalogue for this family |
| **CUSTOM_455** | • Target hardness in HRB — derived HB falls outside the A370 HRB range |
| **EN24** | • Exact A1 for this grade<br>• Soak hours — driven by coil mass and furnace load, not part diameter |
| **EN24_ESR** | • Exact A1 for this grade<br>• Soak hours — driven by coil mass and furnace load, not part diameter |
| **H11_H13** | • Target hardness in HRB — derived HB falls outside the A370 HRB range<br>• Exact A1 for this grade<br>• Soak hours — driven by coil mass and furnace load, not part diameter |
| **HASTELLOY_C276** | • Target hardness — no steel Rm/HB correlation for this family; enter per grade<br>• Hold time — not stated in the source |
| **INCONEL625** | • Target hardness — no steel Rm/HB correlation for this family; enter per grade<br>• Hold time — not stated in the source |
| **INCONEL718** | • Target hardness — no steel Rm/HB correlation for this family; enter per grade<br>• Hold time — not stated in the source |
| **INCONEL_600** | • Target hardness — no steel Rm/HB correlation for this family; enter per grade<br>• Cooling — not stated in the source |
| **INCONEL_X750** | • Target hardness — no steel Rm/HB correlation for this family; enter per grade<br>• Hold time — not stated in the source |
| **MONEL_K500** | • Target hardness — no steel Rm/HB correlation for this family; enter per grade<br>• Hold time — not stated in the source |
| **MP159** | • Target hardness — no steel Rm/HB correlation for this family; enter per grade |
| **MP35N** | • Target hardness — no steel Rm/HB correlation for this family; enter per grade<br>• Anneal cycle — none in the catalogue, because this grade is listed in the hard condition |
| **NITRONIC60** | • Target hardness in HRB — derived HB falls outside the A370 HRB range |
| **RENE41** | • Target hardness — no steel Rm/HB correlation for this family; enter per grade |
| **SCM435** | • Exact A1 for this grade<br>• Soak hours — driven by coil mass and furnace load, not part diameter |
| **TI_3AL25V** | • Target hardness — no steel Rm/HB correlation for this family; enter per grade<br>• Mill-anneal cycle — not researched for this grade |
| **TI_6242** | • Target hardness — no steel Rm/HB correlation for this family; enter per grade<br>• Hold time — not stated in the source |
| **TI_BETA_C** | • Target hardness — no steel Rm/HB correlation for this family; enter per grade<br>• Hold time — not stated in the source<br>• Cooling — not stated in the source |
| **TI_GR1** | • Target hardness — no steel Rm/HB correlation for this family; enter per grade |
| **TI_GR23** | • Target hardness — no steel Rm/HB correlation for this family; enter per grade |
| **TI_GR5** | • Target hardness — no steel Rm/HB correlation for this family; enter per grade |
| **VT16** | • Target hardness — no steel Rm/HB correlation for this family; enter per grade |
| **WASPALOY** | • Target hardness — no steel Rm/HB correlation for this family; enter per grade |

