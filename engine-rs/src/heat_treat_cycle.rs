//! Port of HeatTreatCycle.kt: timed furnace cycles for quench & temper, precipitation hardening and the soft anneals.

use crate::catalog::{HeatTreatmentSpec, MaterialInfo};
use crate::gen_enums::{MaterialFamily, StainlessType};
use crate::jfmt;
use crate::poka_yoke;
use crate::soft_anneal::{self, AnnealStage};
use crate::steel;
use serde::{Deserialize, Serialize};

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FurnaceCycle {
    pub title: String,
    pub stages: Vec<AnnealStage>,
    pub basis: String,
    pub sources: Vec<String>,
    pub cautions: Vec<String>,
}

impl FurnaceCycle {
    pub fn total_low(&self) -> f64 {
        self.stages.iter().map(|s| s.hours_low).sum()
    }
    pub fn total_high(&self) -> f64 {
        self.stages.iter().map(|s| s.hours_high).sum()
    }
    pub fn total_text(&self) -> String {
        format!("{} – {} h", jfmt::f(self.total_low(), 1), jfmt::f(self.total_high(), 1))
    }
}

fn st(name: impl Into<String>, detail: impl Into<String>, lo: f64, hi: f64) -> AnnealStage {
    AnnealStage { name: name.into(), detail: detail.into(), hours_low: lo, hours_high: hi }
}
fn strs(v: &[&str]) -> Vec<String> {
    v.iter().map(|s| s.to_string()).collect()
}
fn f(v: f64) -> String {
    jfmt::f(v, 1)
}
/// Kotlin `contains(x, ignoreCase = true)`.
pub(crate) fn contains_ic(s: &str, x: &str) -> bool {
    s.to_lowercase().contains(&x.to_lowercase())
}
fn if_blank<'a>(s: &'a str, d: &'a str) -> &'a str {
    if s.trim().is_empty() {
        d
    } else {
        s
    }
}

/// AMS 2759/1: temper soak is 2 h minimum, plus 1 h for each 25 mm of section over the first 25 mm.
pub fn temper_hours(section_mm: f64) -> f64 {
    2.0 + ((section_mm - 25.4) / 25.4).max(0.0).ceil()
}

/// Austenitize / solution soak: 1 h per 25 mm of section, never under 1 h.
pub fn soak_hours(section_mm: f64) -> f64 {
    1.0f64.max((section_mm / 25.4 * 2.0).ceil() / 2.0)
}

const QT_SOURCES: [&str; 4] = [
    "AMS 2759/1: tempering soak not less than 2 h, plus 1 h for each inch (25 mm) of thickness or part thereof over the first inch",
    "AMS 2759: thickness is the minimum dimension of the heaviest section of the part or nested load; with load thermocouples the soak runs from the load, and is never under 1 h",
    "Shop rule for austenitizing: 1 h per inch (25 mm) of section, minimum 1 h, timed from when the load reaches temperature",
    "ISO 4042 / ASTM F1940: electroplated parts at or above 1000 MPa are baked within 4 h of plating",
];

/// Quench & temper for a steel fastener of this section; None for grades that are not quenched and tempered.
pub fn quench_temper(material: &MaterialInfo, section_mm: f64, spec: Option<&HeatTreatmentSpec>) -> Option<FurnaceCycle> {
    if poka_yoke::material_family(material) != MaterialFamily::CARBON_ALLOY_STEEL {
        return None;
    }
    if !steel::can_spheroidise(material) {
        return Some(solution_treat_and_age(material, section_mm));
    }
    let ht = spec.unwrap_or(&material.heat_treatment_spec);
    let aust = &ht.austenitizing_or_solution_temp_c;
    if aust.trim().is_empty() || contains_ic(aust, "N/A") || contains_ic(aust, "Not applicable") {
        return None;
    }
    let soak = soak_hours(section_mm);
    let temper = temper_hours(section_mm);
    let mut stages: Vec<AnnealStage> = Vec::new();
    if !ht.preheat_temp_c.trim().is_empty() && !contains_ic(&ht.preheat_temp_c, "N/A") {
        stages.push(st("1. Preheat", format!("{} — equalise before the final heat so a heavy section does not crack", ht.preheat_temp_c), 0.5, 1.0));
    }
    let n = |s: &Vec<AnnealStage>| s.len() + 1;
    stages.push(st(
        format!("{}. Heat to austenitize", n(&stages)),
        format!("{}{}", aust, if !ht.atmosphere.trim().is_empty() { format!(" • {}", ht.atmosphere) } else { " • protective or neutral atmosphere".into() }),
        0.5,
        1.5,
    ));
    stages.push(st(
        format!("{}. Austenitize soak", n(&stages)),
        format!("{} h at temperature = 1 h per 25 mm of {} mm section, minimum 1 h. Time it from the load thermocouple, not the setpoint", f(soak), f(section_mm)),
        soak,
        soak,
    ));
    stages.push(st(
        format!("{}. Quench", n(&stages)),
        format!("{} — transfer without delay; a slow transfer loses hardness at the surface", if_blank(&ht.quench_medium, "Per the grade's quenchant")),
        0.1,
        0.2,
    ));
    stages.push(st(
        format!("{}. Temper within 1 h of the quench", n(&stages)),
        format!("{} — untempered martensite cracks if it is left to stand", if_blank(&ht.tempering_or_aging_temp_c, "Per class")),
        0.3,
        1.0,
    ));
    stages.push(st(
        format!("{}. Temper soak", n(&stages)),
        format!("{} h = AMS 2759/1: 2 h minimum, plus 1 h per 25 mm over the first 25 mm ({} mm section)", f(temper), f(section_mm)),
        temper,
        temper,
    ));
    if ht.is_double_temper {
        stages.push(st(
            format!("{}. Second temper", n(&stages)),
            "Cool to room temperature between tempers, then repeat — the second temper conditions the martensite formed on cooling from the first",
            temper,
            temper,
        ));
    }
    stages.push(st(format!("{}. Cool & unload", n(&stages)), if_blank(&ht.cooling_method, "Still air"), 0.5, 1.5));
    if ht.hydrogen_baking_required {
        stages.push(st(
            format!("{}. Hydrogen bake (after plating)", n(&stages)),
            format!(
                "{}, {}, started within 4 h of plating (ISO 4042 / ASTM F1940). Not part of the furnace cycle above — it runs after the coating line",
                if_blank(&ht.baking_temp_c, "190 °C – 220 °C"),
                if_blank(&ht.baking_soak_time, "8 to 24 h")
            ),
            8.0,
            24.0,
        ));
    }
    Some(FurnaceCycle {
        title: "Quench & temper — complete cycle".into(),
        stages,
        basis: format!("Soak and temper worked out for a {} mm ruling section (the shank Ø, not the head)", f(section_mm)),
        sources: strs(&QT_SOURCES),
        cautions: strs(&[
            "Section size sets the hours. For a fastener the ruling section is the shank; a flange or a heavy head soaks longer than the shank does.",
            "Soak time starts when the LOAD reaches temperature. A furnace at setpoint with a cold charge in it has not started soaking.",
            "Temper temperature, not time, sets the final hardness. Do not shorten the temper to hit a hardness: drop the temperature instead.",
        ]),
    })
}

const ALUMINIUM_SOURCES: [&str; 4] = [
    "6061 supplier procedure: heat to 413 °C (775 °F), hold 2 – 3 h, then cool no faster than 28 °C/h (50 °F/h) to 260 °C (500 °F)",
    "Same 413 °C / 2 – 3 h / 28 °C per h cycle is published for 7075; a 7075 plate datasheet instead asks for 10 °C/h, so confirm against the spec you work to",
    "7xxx patent describing the conventional MIL route: 2 h at 413 °C, then 232 °C for 6 h; under 30 °C/h gives a full O condition",
    "5xxx are not heat treatable: the anneal only recrystallises, and the cooling rate does not matter",
];

fn aluminium_series(key: &str) -> Option<char> {
    let u = key.to_uppercase();
    let rest = u.strip_prefix("AL").unwrap_or(&u);
    rest.chars().next().filter(|c| c.is_numeric())
}

/// The soft-condition cycle for anything that is not spheroidize annealed.
pub fn soft_anneal(material: &MaterialInfo, section_mm: f64) -> Option<FurnaceCycle> {
    match poka_yoke::material_family(material) {
        MaterialFamily::ALUMINUM => Some(aluminium(material)),
        MaterialFamily::COPPER_ALLOY => Some(copper(material)),
        MaterialFamily::STAINLESS => match poka_yoke::stainless_type(material) {
            Some(StainlessType::MARTENSITIC) | Some(StainlessType::FERRITIC) => Some(sub_critical_stainless_anneal(material, section_mm)),
            _ => solution_anneal(material, section_mm),
        },
        MaterialFamily::CARBON_ALLOY_STEEL => {
            if !steel::can_spheroidise(material) {
                Some(maraging_solution_anneal(material, section_mm))
            } else {
                None
            }
        }
        MaterialFamily::NICKEL_SUPERALLOY => superalloy_anneal(material, section_mm),
        _ => None,
    }
}

fn superalloy_anneal(material: &MaterialInfo, section_mm: f64) -> Option<FurnaceCycle> {
    let mill = soft_anneal::mill_anneal(&material.key)?;
    let soak = soak_hours(section_mm);
    Some(FurnaceCycle {
        title: "Solution (mill) anneal — complete cycle".into(),
        stages: vec![
            st("1. Charge & heat", format!("{} • vacuum or argon, or the surface oxidises", mill.temperature_c), 0.5, 1.5),
            st(
                "2. Soak",
                format!("{} — {} h by the shop rule of 1 h per 25 mm of {} mm section, minimum 1 h", if_blank(mill.hold, "Hold not stated in the source"), f(soak), f(section_mm)),
                soak,
                soak,
            ),
            st("3. Cool", if_blank(mill.cooling, "Cooling not stated in the source"), 0.1, 0.5),
        ],
        basis: format!("Mill anneal of {} for a {} mm section, to soften the wire between blows", material.name, f(section_mm)),
        sources: vec![format!("{} ({})", mill.source, mill.confidence)],
        cautions: strs(&[
            "This is a solution anneal: the softening comes from dissolving the hardening phases and cooling fast enough to keep them dissolved. A slow or furnace cool does the opposite.",
            "Never age before heading: the age goes on the finished part.",
        ]),
    })
}

/// Solution treat & age for Inconel 718, A286 and Waspaloy; None for other grades.
pub fn precipitation_harden(material: &MaterialInfo, section_mm: f64) -> Option<FurnaceCycle> {
    match material.key.as_str() {
        "INCONEL718" => Some(inconel718(section_mm)),
        "A286" => Some(a286(section_mm)),
        "WASPALOY" => Some(waspaloy(section_mm)),
        _ => None,
    }
}

fn a286(section_mm: f64) -> FurnaceCycle {
    FurnaceCycle {
        title: "Solution treat & age — complete cycle".into(),
        stages: vec![
            st("1. Charge & heat", "982 °C (1800 °F) for AMS 5731 / 5732, or 899 °C (1650 °F) for AMS 5737 • vacuum, argon or protective gas", 0.5, 1.5),
            st("2. Solution soak", format!("About 1 h at temperature ({} mm section)", f(section_mm)), 1.0, 1.0),
            st("3. Quench", "Oil or water", 0.1, 0.3),
            st("4. Age 718 °C (1325 °F)", "Heat to 718 °C and hold 16 h (AMS window 704 – 760 °C, at least 16 h)", 16.5, 17.0),
            st("5. Air cool", "To room temperature", 0.5, 1.0),
        ],
        basis: format!("A286 solution treatment and ageing for a {} mm section", f(section_mm)),
        sources: strs(&[
            "Aircraft Materials A286 (AMS 5731 / 5732 / 5737): 1800 °F or 1650 °F, about 1 h, water or oil quench",
            "High Temp Metals A286: solution 1800 °F 1 h oil quench, age 1325 °F 16 h air cool",
            "Rolled Alloys A-286: 1800 °F gives the best creep-rupture strength, 1650 °F better ductility and room-temperature strength",
        ]),
        cautions: strs(&[
            "Pick the solution temperature from the drawing: 982 °C and 899 °C give different properties (AMS 5732 vs AMS 5737).",
            "Hardness acceptance values differ between sources (248 – 341 HB and 277 – 363 HB quoted): use the AMS sheet on the drawing.",
        ]),
    }
}

fn waspaloy(section_mm: f64) -> FurnaceCycle {
    FurnaceCycle {
        title: "Solution treat, stabilize & age — complete cycle".into(),
        stages: vec![
            st("1. Charge & heat", "996 – 1038 °C (1825 – 1900 °F), 1010 °C typical • vacuum or argon. Heat through 650 – 870 °C fast", 0.5, 1.5),
            st("2. Solution soak", "4 h (Special Metals heat treatment B; Haynes uses 2 h, Upmet 1 h)", 4.0, 4.0),
            st("3. Oil quench", "Or fast gas / water", 0.1, 0.3),
            st("4. Stabilize 845 °C (1550 °F)", "4 h, then air cool", 4.5, 5.0),
            st("5. Age 760 °C (1400 °F)", "16 h, then air cool", 16.5, 17.0),
        ],
        basis: format!("Waspaloy heat treatment for room- and high-temperature tensile properties, {} mm section", f(section_mm)),
        sources: strs(&[
            "Special Metals Waspaloy, heat treatment B: 995 – 1035 °C 4 h oil quench; 845 °C 4 h AC; 760 °C 16 h AC; 34 – 44 HRC",
            "Haynes Waspaloy: 1825 °F 2 h, then 1550 °F 4 h and 1400 °F 16 h",
            "Upmet AMS 5706: supplied solution treated 1825 – 1900 °F for 1 h",
        ]),
        cautions: strs(&[
            "For creep / stress-rupture parts Special Metals uses a different route (1975 °F 4 h AC, 1550 °F 24 h, 1400 °F 16 h): follow the drawing.",
            "Waspaloy is prone to strain-age cracking: heat cold-worked parts through 650 – 870 °C as fast as possible.",
        ]),
    }
}

fn inconel718(section_mm: f64) -> FurnaceCycle {
    let soak = soak_hours(section_mm);
    let fc_hours = (718.0 - 621.0) / 55.0;
    let hold621 = 18.0 - 8.0 - fc_hours;
    FurnaceCycle {
        title: "Solution treat & age — complete cycle".into(),
        stages: vec![
            st("1. Charge & heat", "927 – 1010 °C (1700 – 1850 °F), 955 – 980 °C typical • vacuum or argon", 0.5, 1.5),
            st("2. Solution soak", format!("Time commensurate with section (AMS 5662) — {} h by the shop rule for a {} mm section", f(soak), f(section_mm)), soak, soak),
            st("3. Rapid cool", "Water, or fast gas / air (air cool or faster). The faster the cool, the stronger the ageing response", 0.1, 0.3),
            st("4. Age 718 °C (1325 °F)", "Heat to 718 °C and hold 8 h", 8.5, 9.0),
            st("5. Furnace cool", "55 °C/h (100 °F/h) from 718 °C to 621 °C (1150 °F)", fc_hours, fc_hours),
            st("6. Hold 621 °C (1150 °F)", format!("Until the total ageing time reaches 18 h (≈ {} h here)", f(hold621)), hold621, hold621),
            st("7. Air cool", "To room temperature", 0.5, 1.0),
        ],
        basis: format!("AMS 5662 solution treatment + AMS 5663 precipitation treatment for a {} mm section", f(section_mm)),
        sources: strs(&[
            "Special Metals INCONEL 718: solution 1700 – 1850 °F, rapid cool usually in water; age 1325 °F 8 h, furnace cool to 1150 °F, hold for a total ageing time of 18 h, air cool",
            "ASTM A1014: precipitation treatment at 1325 °F (718 °C) followed by a furnace-cooled step to 1150 °F (621 °C)",
            "NASA PRC-2003: a controlled 100 °F/h cool may replace the furnace cool",
            "voestalpine 718: about 28 HRC max annealed, about 36 – 43 HRC aged",
        ]),
        cautions: strs(&[
            "The 18 h is the total precipitation time, including the furnace cool from 718 to 621 °C.",
            "Do not furnace cool from the solution temperature: slow cooling precipitates δ / γ″ in the solution step and weakens the age.",
            "AMS 5664 is a different cycle with a higher solution temperature: use it only when the drawing calls for it.",
            "Aerospace 718 bolts are usually thread rolled after this treatment: check the drawing / bolt specification.",
        ]),
    }
}

fn sub_critical_stainless_anneal(material: &MaterialInfo, section_mm: f64) -> FurnaceCycle {
    let ty = poka_yoke::stainless_type(material);
    let soak = soak_hours(section_mm);
    let id = format!("{} {}", material.key, material.name).to_uppercase();
    let nickel_bearing = id.contains("431");
    if ty == Some(StainlessType::FERRITIC) {
        return FurnaceCycle {
            title: "Ferritic anneal — complete cycle".into(),
            stages: vec![
                st("1. Charge & heat", "Ramp to 705 – 790 °C under protective atmosphere", 0.5, 1.5),
                st(
                    "2. Anneal soak",
                    format!("{} h = 1 h per 25 mm of {} mm section, minimum 1 h — recrystallises the cold work; a ferritic does not transformation-harden", f(soak), f(section_mm)),
                    soak,
                    soak,
                ),
                st("3. Air cool", "Cool briskly through 540 – 400 °C: holding or creeping through that band causes 475 °C embrittlement in a 17 % Cr ferritic", 0.5, 1.0),
            ],
            basis: format!("Ferritic stainless anneal for a {} mm section", f(section_mm)),
            sources: strs(&[
                "AISI 430 anneal: 705 – 790 °C, air cool; the grade does not harden by quenching",
                "475 °C embrittlement: 17 % Cr ferritics embrittle on slow cooling or holding at 400 – 540 °C",
            ]),
            cautions: strs(&[
                "Do not furnace-cool slowly through 540 – 400 °C — that is the 475 °C embrittlement band.",
                "The spec card's 970 – 1000 °C figure is not for this grade's softening: a ferritic gains nothing there but grain growth.",
            ]),
        };
    }
    let stages = if nickel_bearing {
        vec![
            st("1. Charge & heat", "Ramp to 620 – 660 °C under protective atmosphere", 0.5, 1.5),
            st(
                "2. Sub-critical soak",
                format!("{} – {} h — the nickel keeps austenite stable, so a 431-type never anneals fully soft: expect about 230 – 270 HB, not a carbon-steel softness", f(soak + 1.0), f(soak + 3.0)),
                soak + 1.0,
                soak + 3.0,
            ),
            st("3. Air cool", "Still air; the sub-critical hold does not re-austenitise", 0.5, 1.0),
        ]
    } else {
        vec![
            st("1. Charge & heat", "Ramp to 845 – 885 °C under protective atmosphere", 0.5, 1.5),
            st("2. Anneal soak", format!("{} h = 1 h per 25 mm of {} mm section, minimum 1 h", f(soak), f(section_mm)), soak, soak),
            st(
                "3. Furnace cool ≤ 30 °C/h to 600 °C",
                "The slow leg IS the anneal: carbide precipitates and coarsens on the way down. Air cooling from temperature re-hardens the wire instead",
                (865.0 - 600.0) / 30.0,
                (865.0 - 600.0) / 20.0,
            ),
            st("4. Air cool below 600 °C", "The rate no longer matters", 0.5, 1.5),
        ]
    };
    FurnaceCycle {
        title: "Martensitic soft anneal — complete cycle".into(),
        stages,
        basis: format!(
            "{}, {} mm section",
            if nickel_bearing { "Sub-critical anneal for a nickel-bearing martensitic (431-type)" } else { "Full anneal for a 410/420-type martensitic" },
            f(section_mm)
        ),
        sources: strs(&[
            "AISI 410 / 420 full anneal: 830 – 885 °C, furnace cool not over about 30 °C/h to roughly 600 °C, then air — lowest hardness",
            "Process (sub-critical) anneal: 650 – 760 °C air cool where partial softening is enough",
            "AISI 431: full annealing is not effective because of the nickel; sub-critical treatment is the published route",
        ]),
        cautions: strs(&[
            "The 970 – 1020 °C + oil quench on this grade's spec card is the HARDENING treatment, run after forming. Quenching from temperature is how this grade is made hard, not soft.",
            "Do not confuse this with an austenitic solution anneal: here the slow cool softens, and a rapid quench hardens — the exact opposite.",
        ]),
    }
}

fn maraging_solution_anneal(material: &MaterialInfo, section_mm: f64) -> FurnaceCycle {
    let soak = soak_hours(section_mm);
    FurnaceCycle {
        title: "Solution anneal — complete cycle".into(),
        stages: vec![
            st("1. Charge & heat", "Ramp to 815 – 830 °C, protective atmosphere or vacuum", 0.5, 1.5),
            st("2. Solution soak", format!("{} h = 1 h per 25 mm of {} mm section, minimum 1 h", f(soak), f(section_mm)), soak, soak),
            st("3. Air cool", "No quench. The steel air-cools to a soft, tough nickel martensite around 30 HRC — that IS the headable condition", 0.5, 1.5),
            st(
                "4. Age AFTER forming",
                format!(
                    "{} — ageing is where all the strength comes from, and it is done to the finished part",
                    if_blank(&material.heat_treatment_spec.tempering_or_aging_temp_c, "480 °C, 3 – 6 h")
                ),
                3.0,
                6.0,
            ),
        ],
        basis: format!("Maraging solution treatment for a {} mm section; the age is listed because it belongs after forming", f(section_mm)),
        sources: strs(&[
            "AMS 6512 / maraging practice: solution anneal about 815 °C for 1 h per 25 mm, air cool; age 480 °C for 3 – 6 h",
            "Maraging martensite is soft and ductile as cooled — hardening is entirely by precipitation during the age",
        ]),
        cautions: strs(&[
            "There is no quench and no tempering. The quench-and-temper stage list for the other steels does not apply to a maraging grade.",
            "Head and machine in the solution-annealed state; age only the finished part.",
        ]),
    }
}

fn solution_treat_and_age(material: &MaterialInfo, section_mm: f64) -> FurnaceCycle {
    let mut base = maraging_solution_anneal(material, section_mm);
    base.title = "Solution treat & age — complete cycle".into();
    base.basis = format!("Maraging post-forming treatment for a {} mm section — ageing replaces quench and temper", f(section_mm));
    base
}

const SOLUTION_SOURCES: [&str; 3] = [
    "Shop rule for solution annealing: 1 h per inch (25 mm) of section, minimum 30 min at temperature — hold only as long as needed, because austenitic grain grows the whole time",
    "ISO 3506 / ASTM A276: the austenitic grades are supplied solution annealed; strength comes from cold work, not heat treatment",
    "AMS 2759: with load thermocouples the soak runs from the load, never under 1 h",
];

fn solution_anneal(material: &MaterialInfo, section_mm: f64) -> Option<FurnaceCycle> {
    let ht = &material.heat_treatment_spec;
    let temp = &ht.austenitizing_or_solution_temp_c;
    if temp.trim().is_empty() || contains_ic(temp, "N/A") {
        return None;
    }
    let soak = 0.5f64.max((section_mm / 25.4 * 2.0).ceil() / 2.0);
    let ku = material.key.to_uppercase();
    let is_ph = ku.contains("PH") || ku == "CUSTOM_455" || ku == "AM350";
    let mut stages = vec![
        st("1. Charge & heat", format!("{} — bright anneal (H₂/N₂) or vacuum, or the surface scales and has to be pickled", temp), 0.5, 1.5),
        st(
            "2. Solution soak",
            format!("{} h = 1 h per 25 mm of {} mm section, minimum 30 min. Do not overhold: every extra hour coarsens the grain and the wire heads worse, not better", f(soak), f(section_mm)),
            soak,
            soak,
        ),
        st(
            "3. Rapid quench",
            format!(
                "{} — through 850 – 450 °C as fast as the section allows. A slow cool here precipitates chromium carbide and sensitises the grain boundaries",
                if_blank(&ht.quench_medium, "Water or forced gas")
            ),
            0.1,
            0.3,
        ),
    ];
    if is_ph && !ht.tempering_or_aging_temp_c.trim().is_empty() && !contains_ic(&ht.tempering_or_aging_temp_c, "N/A") {
        stages.push(st(
            "4. Age AFTER forming",
            format!(
                "{} for {} — a precipitation-hardening grade is headed in the solution-treated (Condition A) state and aged afterwards",
                ht.tempering_or_aging_temp_c,
                if_blank(&ht.tempering_or_aging_time_hours, "the grade's aging time")
            ),
            1.0,
            4.0,
        ));
    }
    let mut cautions = strs(&[
        "This is not an anneal in the steel sense. Cooling slowly does not soften a stainless — it sensitises it. The quench IS the softening step.",
        "Hold the minimum that fully solutions the section. Unlike a spheroidize anneal, longer is worse.",
    ]);
    if is_ph {
        cautions.push("Condition A is the headable state. Age the formed part; ageing the wire first makes it uncrackable only on paper.".into());
    }
    Some(FurnaceCycle {
        title: "Solution anneal — complete cycle".into(),
        stages,
        basis: format!(
            "Solution soak worked out for a {} mm ruling section{}",
            f(section_mm),
            if is_ph { "; the age is listed because it belongs after forming, not before" } else { "" }
        ),
        sources: strs(&SOLUTION_SOURCES),
        cautions,
    })
}

fn aluminium(material: &MaterialInfo) -> FurnaceCycle {
    let series = aluminium_series(&material.key);
    let heat_treatable = matches!(series, Some('2') | Some('6') | Some('7'));
    let cool_to = if series == Some('7') { 230.0 } else { 260.0 };
    let stages = if heat_treatable {
        vec![
            st("1. Charge & heat", "Ramp to 413 °C (775 °F). Aluminium has a narrow window: 30 °C over and the surface starts to melt", 0.5, 1.5),
            st("2. Soak", "2 – 3 h at 413 °C ± 8 °C — dissolves and coarsens the precipitate so the alloy goes fully soft", 2.0, 3.0),
            st(
                format!("3. Controlled cool to {} °C", jfmt::f(cool_to, 0)),
                "≤ 28 °C/h (50 °F/h). This slow leg IS the anneal: cool faster and the alloy part-solution-treats and comes out harder than it went in",
                (413.0 - cool_to) / 28.0,
                (413.0 - cool_to) / 28.0,
            ),
            st("4. Air cool", format!("Below {} °C the rate no longer matters", jfmt::f(cool_to, 0)), 0.5, 1.5),
        ]
    } else {
        vec![
            st("1. Charge & heat", "Ramp to 343 °C (650 °F)", 0.5, 1.0),
            st("2. Soak", "2 h at 343 °C — 5xxx is not heat treatable, so this only recrystallises the cold work", 2.0, 2.0),
            st("3. Cool", "Still air; the cooling rate does not affect the temper", 0.5, 1.0),
        ]
    };
    let mut cautions = vec!["The controlled cool is the whole point. Quenching from the anneal gives a part-solution-treated part that is HARDER than before and keeps age-hardening on the shelf for days.".to_string()];
    if series == Some('7') {
        cautions.push("Sources disagree on the cooling rate for 7xxx: 28 °C/h is the common figure, one plate datasheet asks for 10 °C/h. Use the slower rate if the spec does not say.".into());
    }
    cautions.push("Head within the annealed window: 2xxx and 7xxx start natural ageing as soon as they are cool.".into());
    FurnaceCycle {
        title: "O-temper anneal — complete cycle".into(),
        stages,
        basis: if heat_treatable {
            // Kotlin prints a null Char as "null"; heat_treatable implies a series here.
            format!("Published 413 °C O-temper cycle for {}xxx aluminium", series.map(|c| c.to_string()).unwrap_or_else(|| "null".into()))
        } else {
            "Recrystallisation anneal for 5xxx (not heat treatable)".into()
        },
        sources: strs(&ALUMINIUM_SOURCES),
        cautions,
    }
}

fn copper(material: &MaterialInfo) -> FurnaceCycle {
    let is_beryllium = material.key.to_uppercase().contains("CUBE") || material.name.to_uppercase().contains("BERYLLIUM");
    if is_beryllium {
        FurnaceCycle {
            title: "Solution anneal — complete cycle".into(),
            stages: vec![
                st("1. Charge & heat", "Ramp to the grade's solution temperature under protective gas", 0.5, 1.5),
                st("2. Solution soak", "15 min – 1 h for thin sections, 1 – 3 h for heavy sections", 0.25, 3.0),
                st("3. Water quench", "Immediate — beryllium copper is precipitation hardening, so the soft state is the quenched state", 0.1, 0.2),
                st("4. Age after forming", "Age only once the part is formed; ageing before heading defeats the purpose", 2.0, 3.0),
            ],
            basis: "Precipitation-hardening copper: the solution-treated condition is the formable one".into(),
            sources: strs(&["Materion copper-beryllium heat treatment guidance: 15 min – 1 h thin sections, 1 – 3 h heavy sections"]),
            cautions: strs(&["Beryllium copper dust and fume are a health hazard — handle per the supplier's safety data sheet."]),
        }
    } else {
        FurnaceCycle {
            title: "Soft anneal — complete cycle".into(),
            stages: vec![
                st("1. Charge & purge", "Protective gas; nitrogen is enough for copper alloys and keeps the surface bright", 0.3, 0.6),
                st(
                    "2. Heat & soak",
                    "0.5 – 3 h in the alloy's soft-anneal band (300 – 650 °C for the coppers). Take the band for THIS alloy from ASTM B601 or the supplier datasheet before setting the furnace",
                    0.5,
                    3.0,
                ),
                st("3. Cool", "Cooling rate is not critical for the solid-solution copper alloys", 0.5, 1.5),
            ],
            basis: "Generic copper soft-anneal envelope — this alloy's own band is not in the catalogue".into(),
            sources: strs(&["Deutsches Kupferinstitut: soft anneal 300 – 650 °C for 0.5 – 3 h (Cu-ETP), 425 – 650 °C (Cu-OF), under protective gas"]),
            cautions: strs(&[
                "This is the generic copper envelope, not this alloy's cycle. Brass, phosphor bronze, silicon bronze and cupronickel each anneal in a different band — get it from ASTM B601 or the mill.",
                "Soft annealing runs well above the recrystallisation temperature; a light anneal sits just above it.",
            ]),
        }
    }
}
