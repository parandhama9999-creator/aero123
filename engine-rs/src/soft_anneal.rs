//! Port of SoftAnnealEngine.kt: the wire-preparation anneal before heading, and the spheroidize furnace cycle.

use crate::catalog::MaterialInfo;
use crate::gen_enums::{AnnealRoute, MaterialFamily, StainlessType};
use crate::jfmt::{self, d as dstr};
use crate::poka_yoke;
use crate::steel;
use crate::tensile::find_numbers;
use serde::{Deserialize, Serialize};

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SoftAnnealSpec {
    pub route: AnnealRoute,
    pub in_house_anneal_needed: bool,
    pub supplied_condition: String,
    pub temperature_c: String,
    pub soak_time: String,
    pub cooling_rate: String,
    pub atmosphere: String,
    pub target_uts_mpa: String,
    pub target_hardness_hb: String,
    pub target_hardness_hrb: String,
    pub structure_target: String,
    pub standard_reference: String,
    pub softening_ratio: Option<f64>,
    pub unsourced: Vec<String>,
    pub notes: Vec<String>,
    pub cycle_source: String,
    pub cycle_confidence: String,
}

#[derive(Clone, Debug, PartialEq)]
pub struct MillAnnealCycle {
    pub temperature_c: &'static str,
    pub hold: &'static str,
    pub cooling: &'static str,
    pub source: &'static str,
    pub confidence: &'static str,
    pub note: &'static str,
}

const HIGH: &str = "High — producer / supplier datasheet";
const MEDIUM: &str = "Medium — consistent secondary sources";
const DISPUTED: &str = "Medium — sources disagree";
const LOW: &str = "Low — verify against GOST / TU before use";

const fn mc(temperature_c: &'static str, hold: &'static str, cooling: &'static str, source: &'static str, confidence: &'static str, note: &'static str) -> MillAnnealCycle {
    MillAnnealCycle { temperature_c, hold, cooling, source, confidence, note }
}

pub const MILL_ANNEAL: &[(&str, MillAnnealCycle)] = &[
    ("TI_GR5", mc("704 – 788 °C (1300 – 1450 °F)", "2 h", "Air cool", "Rolled Alloys 6Al-4V datasheet", HIGH, "")),
    ("TI_GR23", mc("704 – 732 °C (1300 – 1350 °F)", "1 – 8 h", "Air cool", "ATI 6-4 ELI datasheet (AMS 4930, annealed)", HIGH, "")),
    ("TI_6242", mc("704 – 843 °C (1300 – 1550 °F)", "", "Air cool", "TIMET 6242 datasheet", HIGH, "Duplex anneal alternative: 899 °C 30 min AC, then 788 °C 15 min AC.")),
    ("TI_GR1", mc("649 – 760 °C (1200 – 1400 °F)", "6 min – 2 h", "Air cool", "Corrosion Materials / Rolled Alloys CP Ti datasheets", HIGH, "")),
    ("VT16", mc("770 – 790 °C", "2 h", "Furnace cool 1 – 5 °C/min to 500 °C, then air", "TsKBA standard process", HIGH, "")),
    ("INCONEL718", mc(
        "927 – 1010 °C (1700 – 1850 °F), 955 – 980 °C typical",
        "Time commensurate with section size (AMS 5662)",
        "Rapid cool, usually water quench; air cool or faster for thin wire. Never furnace cool",
        "Special Metals INCONEL 718 bulletin; AMS 5662 supplier data sheet; voestalpine 718",
        HIGH,
        "A slow cool or a hold between about 600 and 1000 °C precipitates γ″ / δ and leaves the wire hard: the usual reason a 718 anneal does not soften. Annealed 718 is about 28 HRC max (voestalpine). Above about 990 – 1010 °C δ dissolves and the grain grows fast.",
    )),
    ("A286", mc("982 °C (1800 °F); 899 °C (1650 °F) for more ductility", "1 h", "Water or oil quench", "Aircraft Materials; High Temp Metals; Rolled Alloys (AMS 5731 / 5732 / 5737)", HIGH, "")),
    ("WASPALOY", mc(
        "996 – 1038 °C (1825 – 1900 °F); 1049 °C (1920 °F) quoted for wire",
        "1 – 4 h (Upmet 1 h, Haynes 2 h, Special Metals 4 h)",
        "Rapid: oil or water quench; air cool only for thin wire",
        "Special Metals Waspaloy (heat treatment B); Haynes; Upmet AMS 5706; Alloy Wire",
        HIGH,
        "Heat through 650 – 870 °C as fast as possible to avoid strain-age cracking. A slow cool precipitates γ′ and leaves the wire hard. The γ′ solvus is about 1032 – 1043 °C (1890 – 1910 °F), where grain growth also begins (CarTech).",
    )),
    ("RENE41", mc("1066 – 1080 °C (1950 – 1975 °F)", "4 h", "Air cool", "Alfa Chemistry; High Temp Metals", DISPUTED, "Heat fast through 650 – 870 °C (strain-age cracking). Catalogue hardening cycle tops out at 1120 °C, above these sources.")),
    ("INCONEL625", mc("982 °C typical (Grade 1, 871 °C min)", "", "Air cool", "Langley Alloys; Rickard Metals", DISPUTED, "Grade 1 anneal for cold forming. The catalogue's 1090 – 1150 °C is the Grade 2 solution, which coarsens grain.")),
    ("INCONEL_600", mc("1010 °C (1850 °F)", "15 min", "", "Total Materia (citing Special Metals); Austral Wright", MEDIUM, "Brief exposure to 1040 °C also softens without coarse grain.")),
    ("HASTELLOY_C276", mc("1121 °C (2050 °F)", "", "Water quench (fast air only below 10 mm)", "HPAlloys; AZoM", MEDIUM, "Slow cooling precipitates carbides and destroys corrosion resistance.")),
    ("MONEL_K500", mc("982 – 1038 °C (1800 – 1900 °F)", "", "Water quench", "High Temp Metals; Metal Suppliers Online", MEDIUM, "")),
    ("MP159", mc("1038 – 1052 °C (1900 – 1925 °F)", "4 – 8 h", "Water quench", "Aircraft Materials / UPMET MP159 (AMS 5842)", MEDIUM, "Solution-treated stock is the soft condition; strength comes from work-strengthening plus aging.")),
    ("INCONEL_X750", mc("982 °C ± 14 °C (1800 °F)", "", "Air cool or faster", "Total Materia Inconel X-750", MEDIUM, "")),
    ("TI_BETA_C", mc("788 – 927 °C (1450 – 1700 °F) solution treat", "", "", "Total Materia Ti-3Al-8V-6Cr-4Mo-4Zr", MEDIUM, "Beta alloy: the solution-treated condition is the soft, cold-formable one. Re-solution between blows, age after forming.")),
    ("10KH11N23T3MR", mc("≈ 1000 – 1050 °C (unconfirmed)", "", "Air", "Garbled table in a TsKBA amendment only", LOW, "")),
];

pub fn mill_anneal(key: &str) -> Option<&'static MillAnnealCycle> {
    MILL_ANNEAL.iter().find(|(k, _)| *k == key).map(|(_, v)| v)
}

const A1_PLAIN_CARBON_C: f64 = 723.0;

/// Brinell from tensile strength for steel (ASTM A370 / SAE J417 basis): HB = Rm / 3.38.
pub const HB_PER_MPA: f64 = 3.38;

/// ASTM A370 steel HB -> HRB, piecewise over the annealed range.
pub fn hb_to_hrb(hb: f64) -> Option<f64> {
    const PTS: [(f64, f64); 7] = [(100.0, 56.2), (120.0, 66.7), (140.0, 75.0), (160.0, 83.0), (180.0, 89.0), (200.0, 93.0), (220.0, 97.0)];
    if hb < PTS[0].0 || hb > PTS[PTS.len() - 1].0 {
        return None;
    }
    for i in 0..PTS.len() - 1 {
        let (x0, y0) = PTS[i];
        let (x1, y1) = PTS[i + 1];
        if hb >= x0 && hb <= x1 {
            return Some(y0 + (hb - x0) / (x1 - x0) * (y1 - y0));
        }
    }
    None
}

/// Pulls "480 – 560" out of "480 – 560 MPa (Spheroidized Annealed Wire, >85% globular)".
pub fn parse_uts_range(s: &str) -> Option<(f64, f64)> {
    let range: Vec<f64> = find_numbers(s).into_iter().filter(|&v| v >= 100.0).collect();
    match range.len() {
        0 => None,
        1 => Some((range[0], range[0])),
        _ => Some((range[0], range[1])),
    }
}

fn fmt0(v: f64) -> String {
    jfmt::f(v, 0)
}

/// Kotlin substringAfter / substringBeforeLast: the whole string when the delimiter is missing.
fn substring_after<'a>(s: &'a str, d: &str) -> &'a str {
    s.find(d).map(|i| &s[i + d.len()..]).unwrap_or(s)
}
fn substring_before_last<'a>(s: &'a str, d: &str) -> &'a str {
    s.rfind(d).map(|i| &s[..i]).unwrap_or(s)
}

pub fn build(material: &MaterialInfo) -> SoftAnnealSpec {
    let supplied = material.raw_material_sourcing_uts_mpa.clone();
    let cond = supplied.to_uppercase();
    let family = poka_yoke::material_family(material);
    let solution_temp = material.heat_treatment_spec.austenitizing_or_solution_temp_c.clone();

    let supplied_hard = cond.contains("COLD DRAWN") || cond.contains("HALF-HARD") || cond.contains("HALF HARD");
    use MaterialFamily as MF;
    let route = if supplied_hard {
        AnnealRoute::ANNEAL_REQUIRED
    } else if family == MF::CARBON_ALLOY_STEEL && !steel::can_spheroidise(material) {
        AnnealRoute::SOLUTION_ANNEAL
    } else if family == MF::CARBON_ALLOY_STEEL {
        AnnealRoute::SPHEROIDIZE
    } else if family == MF::STAINLESS {
        AnnealRoute::SOLUTION_ANNEAL
    } else if family == MF::ALUMINUM || cond.contains("CONDITION O") {
        AnnealRoute::O_TEMPER
    } else if family == MF::TITANIUM || family == MF::NICKEL_SUPERALLOY {
        AnnealRoute::MILL_ANNEALED
    } else if family == MF::COPPER_ALLOY {
        AnnealRoute::O_TEMPER
    } else {
        AnnealRoute::MILL_ANNEALED
    };

    let uts = parse_uts_range(&supplied);
    let mut unsourced: Vec<String> = Vec::new();
    let mut notes: Vec<String> = Vec::new();

    let is_steel = family == MF::CARBON_ALLOY_STEEL || family == MF::STAINLESS;
    let mut hb_text = String::new();
    let mut hrb_text = String::new();
    match uts {
        Some((lo_u, hi_u)) if is_steel => {
            let hb_lo = lo_u / HB_PER_MPA;
            let hb_hi = hi_u / HB_PER_MPA;
            hb_text = format!("{} – {} HB (derived: Rm / {})", fmt0(hb_lo), fmt0(hb_hi), dstr(HB_PER_MPA));
            match (hb_to_hrb(hb_lo), hb_to_hrb(hb_hi)) {
                (Some(lo), Some(hi)) => hrb_text = format!("≈ {} – {} HRB (ASTM A370 conversion)", fmt0(lo), fmt0(hi)),
                _ => unsourced.push("Target hardness in HRB — derived HB falls outside the A370 HRB range".into()),
            }
        }
        _ => unsourced.push("Target hardness — no steel Rm/HB correlation for this family; enter per grade".into()),
    }

    let softening = match uts {
        Some((a, b)) if material.post_forging_target_uts_mpa > 0.0 => Some(((a + b) / 2.0) / material.post_forging_target_uts_mpa),
        _ => None,
    };

    let base = |route: AnnealRoute, in_house: bool, temperature_c: String, soak_time: &str, cooling_rate: &str, atmosphere: &str, structure_target: String, standard_reference: String, unsourced: Vec<String>, notes: Vec<String>| SoftAnnealSpec {
        route,
        in_house_anneal_needed: in_house,
        supplied_condition: supplied.clone(),
        temperature_c,
        soak_time: soak_time.into(),
        cooling_rate: cooling_rate.into(),
        atmosphere: atmosphere.into(),
        target_uts_mpa: supplied.clone(),
        target_hardness_hb: hb_text.clone(),
        target_hardness_hrb: hrb_text.clone(),
        structure_target,
        standard_reference,
        softening_ratio: softening,
        unsourced,
        notes,
        cycle_source: String::new(),
        cycle_confidence: String::new(),
    };
    let std_ref = material.heat_treatment_spec.standard_reference.clone();

    let mut spec = match route {
        AnnealRoute::SPHEROIDIZE => {
            let comp = steel::for_material(material);
            let ac1 = comp.as_ref().map(steel::ac1);
            if let (Some(ac1), Some(comp)) = (ac1, comp.as_ref()) {
                notes.push(format!(
                    "A1 = {} °C for this grade, from its nominal composition by Andrews (Mn {}, Ni {}, Si {}, Cr {} %). Plain carbon sits at {} °C: nickel and manganese pull A1 down, silicon and chromium push it up. Confirm against the mill certificate — a heat at the far end of its range moves A1 by a few degrees.",
                    fmt0(ac1), dstr(comp.manganese), dstr(comp.nickel), dstr(comp.silicon), dstr(comp.chromium), fmt0(A1_PLAIN_CARBON_C)
                ));
            } else {
                notes.push(format!(
                    "A1 baseline is {} °C for plain carbon steel; this grade's composition is not on file. Cr, Mo and Si raise A1; Mn and Ni lower it — confirm A1 for this grade before setting the furnace.",
                    fmt0(A1_PLAIN_CARBON_C)
                ));
                unsourced.push("Exact A1 for this grade".into());
            }
            let a1 = ac1.unwrap_or(A1_PLAIN_CARBON_C);
            base(
                route,
                true,
                format!("{} – {} °C (below A1 with a 15 °C margin; A1 = {} °C)", fmt0(a1 - 40.0), fmt0(a1 - 15.0), fmt0(a1)),
                "See the full cycle below — soak is coil-mass driven, not part Ø",
                "Slow furnace cool ≈ 20 °C/h to 550 °C, then still air (published wire practice)",
                "Protective N₂ or endothermic gas — prevent decarburisation of the wire surface",
                if cond.contains("GLOBULAR") { substring_before_last(substring_after(&supplied, "("), ")").to_string() } else { "Spheroidised cementite — globular fraction per EN 10263".into() },
                "EN 10263-2 / EN 10263-4 (steel rod & wire for cold heading)".into(),
                unsourced,
                notes,
            )
        }
        AnnealRoute::SOLUTION_ANNEAL => match poka_yoke::stainless_type(material) {
            Some(StainlessType::MARTENSITIC) => {
                let is431 = format!("{} {}", material.key, material.name).to_uppercase().contains("431");
                notes.push(format!(
                    "Martensitic stainless: the {} on the spec card is the hardening treatment, run after forming. The anneal holds below the critical range and cools slowly — quenching from temperature makes this grade HARD, not soft.",
                    solution_temp
                ));
                if is431 {
                    notes.push("A 431-type never anneals fully soft: its nickel keeps austenite stable, so expect about 230 – 270 HB from the sub-critical treatment.".into());
                }
                base(
                    route,
                    true,
                    if is431 { "620 – 660 °C (sub-critical; full anneal is not effective on a 431-type)".into() } else { format!("845 – 885 °C (full anneal; hardening at {} comes after forming)", solution_temp) },
                    "See the full cycle below",
                    if is431 { "Still air — the sub-critical hold does not re-austenitise" } else { "Furnace cool ≤ 30 °C/h to 600 °C, then air — the slow leg is the anneal" },
                    "Protective atmosphere or vacuum to avoid scaling",
                    "Ferrite with coarse spheroidal carbide — the soft, headable structure".into(),
                    std_ref.clone(),
                    unsourced,
                    notes,
                )
            }
            Some(StainlessType::FERRITIC) => {
                notes.push("Ferritic stainless does not transformation-harden: the anneal only recrystallises the cold work. Cool briskly through 540 – 400 °C — creeping through that band causes 475 °C embrittlement in a 17 % Cr ferritic.".into());
                base(
                    route,
                    true,
                    "705 – 790 °C (recrystallisation anneal)".into(),
                    "See the full cycle below",
                    "Air cool; pass 540 – 400 °C briskly (475 °C embrittlement)",
                    "Protective atmosphere to keep the surface bright",
                    "Recrystallised ferrite, fine equiaxed grain".into(),
                    std_ref.clone(),
                    unsourced,
                    notes,
                )
            }
            _ => {
                notes.push("Austenitic grades are solution annealed, not spheroidised — the goal is the opposite: dissolve carbides and cool fast enough to keep them in solution.".into());
                base(
                    route,
                    true,
                    solution_temp.clone(),
                    "Hold until fully solutioned, then cool without delay",
                    "Rapid — water quench or forced gas; slow cooling sensitises the grain boundaries",
                    "Bright-anneal (H₂/N₂) or vacuum to avoid scaling",
                    "Carbide-free austenite, uniform grain size".into(),
                    std_ref.clone(),
                    unsourced,
                    notes,
                )
            }
        },
        AnnealRoute::O_TEMPER => {
            unsourced.push("Anneal temperature and soak — not in the catalogue for this family".into());
            notes.push("Non-ferrous anneal runs far below steel temperatures. Take the cycle from the supplier datasheet.".into());
            base(
                route,
                !cond.contains("CONDITION O") && !cond.contains("NO ANNEAL"),
                String::new(),
                "",
                "Controlled slow cool to the fully soft condition",
                "Protective or air, per supplier",
                "Fully recrystallised, O temper".into(),
                "Supplier datasheet".into(),
                unsourced,
                notes,
            )
        }
        AnnealRoute::MILL_ANNEALED => {
            notes.push("Bought already annealed — verify the condition on the mill certificate. The cycle shown is the mill anneal, for re-annealing between blows.".into());
            if family == MF::NICKEL_SUPERALLOY {
                notes.push("No spheroidize anneal exists for this alloy: it is austenitic (no ferrite / cementite to ball up) and hardens by precipitation. The solution anneal with a fast cool IS its softest condition.".into());
            }
            let mill = mill_anneal(&material.key);
            match mill {
                None => unsourced.push("Mill-anneal cycle — not researched for this grade".into()),
                Some(m) => {
                    if m.hold.trim().is_empty() {
                        unsourced.push("Hold time — not stated in the source".into());
                    }
                    if m.cooling.trim().is_empty() {
                        unsourced.push("Cooling — not stated in the source".into());
                    }
                    if m.confidence == LOW {
                        unsourced.push("Confirm the anneal cycle against the GOST / TU for this grade".into());
                    }
                    if !m.note.trim().is_empty() {
                        notes.push(m.note.into());
                    }
                }
            }
            let is_ti = family == MF::TITANIUM;
            let mut s = base(
                route,
                false,
                mill.map(|m| m.temperature_c.to_string()).unwrap_or_else(|| "N/A — supplied annealed".into()),
                mill.map(|m| m.hold).unwrap_or("N/A"),
                mill.map(|m| m.cooling).unwrap_or("N/A"),
                if mill.is_none() {
                    "N/A"
                } else if is_ti {
                    "Vacuum or argon — air above ~600 °C forms alpha-case, which cracks on heading"
                } else {
                    "Vacuum, argon or protective gas"
                },
                "As supplied — confirm against the mill certificate".into(),
                std_ref.clone(),
                unsourced,
                notes,
            );
            s.cycle_source = mill.map(|m| m.source.to_string()).unwrap_or_default();
            s.cycle_confidence = mill.map(|m| m.confidence.to_string()).unwrap_or_default();
            s
        }
        AnnealRoute::ANNEAL_REQUIRED => {
            notes.push(if family == MF::CARBON_ALLOY_STEEL {
                "The catalogue lists this wire as cold drawn / half-hard, which is not a headable condition. Either source spheroidize-annealed wire, or anneal before heading.".to_string()
            } else {
                format!(
                    "The catalogue lists this wire in the cold-drawn condition. For {} that is the work-strengthened delivery state, not a headable condition, and it is NOT spheroidize annealed: the soft condition is the solution-treated one. Source solution-treated stock, or take the solution cycle from the alloy's own AMS specification before forming.",
                    material.name
                )
            });
            unsourced.push("Anneal cycle — none in the catalogue, because this grade is listed in the hard condition".into());
            base(route, true, String::new(), "", "", "", "Spheroidised cementite before heading".into(), "EN 10263".into(), unsourced, notes)
        }
    };

    if let Some(sr) = softening {
        if sr < 0.75 && material.flow_stress_coeff_k >= material.post_forging_target_uts_mpa {
            spec.notes.push(format!(
                "Flow-stress check: wire anneals to {} % of the finished UTS, but K ({} MPa) is at or above the finished UTS ({} MPa). Forging tonnage may be overstated — verify K was measured on annealed wire.",
                fmt0(sr * 100.0),
                fmt0(material.flow_stress_coeff_k),
                fmt0(material.post_forging_target_uts_mpa)
            ));
        }
    }
    spec
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AnnealStage {
    pub name: String,
    pub detail: String,
    pub hours_low: f64,
    pub hours_high: f64,
}

impl AnnealStage {
    fn new(name: impl Into<String>, detail: impl Into<String>, lo: f64, hi: f64) -> Self {
        AnnealStage { name: name.into(), detail: detail.into(), hours_low: lo, hours_high: hi }
    }
    pub fn hours_text(&self) -> String {
        if (self.hours_high - self.hours_low).abs() < 0.05 {
            format!("{} h", jfmt::f(self.hours_low, 1))
        } else {
            format!("{} – {} h", jfmt::f(self.hours_low, 1), jfmt::f(self.hours_high, 1))
        }
    }
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AnnealCycle {
    pub stages: Vec<AnnealStage>,
    pub charge_tonnes: f64,
    pub soak_temp_c: String,
    pub basis: String,
    pub sources: Vec<String>,
    pub cautions: Vec<String>,
}

impl AnnealCycle {
    pub fn total_low(&self) -> f64 {
        self.stages.iter().map(|s| s.hours_low).sum()
    }
    pub fn total_high(&self) -> f64 {
        self.stages.iter().map(|s| s.hours_high).sum()
    }
    pub fn total_text(&self) -> String {
        format!("{} – {} h", jfmt::f(self.total_low(), 1), jfmt::f(self.total_high(), 1))
    }
    pub fn hours_per_tonne(&self) -> String {
        if self.charge_tonnes <= 0.0 {
            String::new()
        } else {
            format!("{} h/t", jfmt::f((self.total_low() + self.total_high()) / 2.0 / self.charge_tonnes, 1))
        }
    }
}

pub const DEFAULT_CHARGE_TONNES: f64 = 10.0;

const SOURCES: [&str; 5] = [
    "Lin et al., AISI 1022 cold-heading wire, Taguchi study: 695 – 705 °C, 7 – 8 h soak, 7.5 – 8.5 h furnace cool (optimum 700 °C, 8 h cool)",
    "CN102876859A (screw wire): 710 – 730 °C for 5 – 8 h under inert gas, cool ≈ 20 °C/h to ≈ 550 °C, then ≈ 80 °C/h to ≈ 400 °C, then air",
    "CN109593929B (cold-forging steel): 650 – 690 °C held 6 – 20 h, hold set by section thickness",
    "Singh et al. (AIST), SCM435 wire rod in a bell furnace: whole cycle ≈ 21 – 22 h; alloy grades soak up to 10 h",
    "Bell-furnace practice: a coil stack takes 13 – 25 h to reach 720 °C depending on coil weight, height and position in the stack",
];

fn heat_up_hours(tonnes: f64) -> (f64, f64) {
    let t = jfmt::clamp(tonnes, 1.0, 40.0);
    (jfmt::clamp(4.0 + 0.30 * t, 4.0, 16.0), jfmt::clamp(6.0 + 0.45 * t, 6.0, 25.0))
}

/// The spheroidize furnace cycle; None for any grade that is not spheroidize annealed.
pub fn spheroidize(material: &MaterialInfo, charge_tonnes: f64, wire_dia_mm: f64) -> Option<AnnealCycle> {
    if poka_yoke::material_family(material) != MaterialFamily::CARBON_ALLOY_STEEL || !steel::can_spheroidise(material) {
        return None;
    }
    let ac1 = steel::ac1_for(material).unwrap_or(steel::A1_PLAIN_CARBON_C);
    let soak_lo = ac1 - 40.0;
    let soak_hi = ac1 - 15.0;
    let (heat_lo, heat_hi) = heat_up_hours(charge_tonnes);
    let (hold_lo, hold_hi) = steel::soak_hours(material, wire_dia_mm);
    let slow_cool_to = 550.0;
    let fast_cool_to = 400.0;
    let slow_hours = (soak_lo - slow_cool_to) / 20.0;
    let fast_hours = (slow_cool_to - fast_cool_to) / 80.0;
    let unload_lo = jfmt::clamp(1.0 + 0.08 * charge_tonnes, 1.0, 5.0);
    let unload_hi = jfmt::clamp(2.0 + 0.12 * charge_tonnes, 2.0, 7.0);
    let stages = vec![
        AnnealStage::new("1. Charge & purge", "Load the stack, seal the bell, purge to the protective atmosphere before any heat goes in", 0.5, 1.0),
        AnnealStage::new(
            "2. Heat to soak",
            format!("Ramp to {} – {} °C under N₂ / endothermic gas. Charge-driven: a full stack takes 13 – 25 h, a light one far less", fmt0(soak_lo), fmt0(soak_hi)),
            heat_lo,
            heat_hi,
        ),
        AnnealStage::new("3. Equalise", "Hold until the coldest coil in the stack reaches temperature — the control thermocouple leads the charge, so time from the load couple, not the setpoint", 1.0, 2.0),
        AnnealStage::new(
            "4. Soak (spheroidise)",
            format!(
                "Hold just below A1 to break the lamellar pearlite into globular cementite. This is the stage that decides the structure: too short leaves lamellae, too long coarsens the carbide{}",
                if wire_dia_mm > 0.0 { format!(". Soak adjusted for Ø {} mm wire", jfmt::f(wire_dia_mm, 1)) } else { String::new() }
            ),
            hold_lo,
            hold_hi,
        ),
        AnnealStage::new(
            format!("5. Controlled cool to {} °C", fmt0(slow_cool_to)),
            "≈ 20 °C/h, as in the published wire cycles. A soak below A1 forms no austenite, so this leg is the safeguard: any coil that ran over A1 re-transforms to globular carbide instead of new lamellar pearlite",
            slow_hours,
            slow_hours,
        ),
        AnnealStage::new(format!("6. Cool to {} °C", fmt0(fast_cool_to)), "≈ 80 °C/h below 550 °C; nothing is gained by holding the charge hot", fast_hours, fast_hours),
        AnnealStage::new(
            "7. Cool under cover & unload",
            format!("Stay under the protective atmosphere below {} °C until safe to strip, then still air. Opening hot scales and decarburises the wire surface", fmt0(fast_cool_to)),
            unload_lo,
            unload_hi,
        ),
    ];
    Some(AnnealCycle {
        stages,
        charge_tonnes,
        soak_temp_c: format!("{} – {} °C", fmt0(soak_lo), fmt0(soak_hi)),
        basis: format!("Published bell-furnace cycles for cold-heading wire, with the charge-driven stages scaled to {} t", jfmt::f(charge_tonnes, 1)),
        sources: SOURCES.iter().map(|s| s.to_string()).collect(),
        cautions: vec![
            "A starting schedule for a furnace trial, not a released process. Prove it on the furnace's own load thermocouple before running production.".into(),
            "Soak and ramp are set by coil mass, stack height and position in the stack — not by the part diameter. Two coils in one stack do not see the same cycle.".into(),
            "The soak band is set 40 to 15 °C under this grade's own A1, worked out from its nominal composition. A mill certificate at the far end of the range shifts A1 by a few degrees, so check it: soaking above A1 austenitises and undoes the anneal.".into(),
        ],
    })
}
