//! Ports of ToolingDesign.kt (MachineSetup, ToolingDesign) and ExtrusionDie.kt (ExtrusionData, ExtrusionDie).

use crate::calculation::FastenerCalculationResult;
use crate::catalog::MaterialInfo;
use crate::gen_enums::{HeaderRoute, SectionShape, ToolBasis};
use crate::heading::{self, HeadingPlan, HeadingStation, ProfileSegment};
use crate::jfmt::{self, d as dstr};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::f64::consts::PI;

fn f(v: f64) -> String {
    jfmt::f(v, 2)
}
fn fd(v: f64, dp: usize) -> String {
    jfmt::f(v, dp)
}

/// The header's own tool pockets, typed by the user from the machine manual. None = not entered.
#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MachineSetup {
    pub die_case_od_mm: Option<f64>,
    pub die_case_length_mm: Option<f64>,
    pub punch_case_od_mm: Option<f64>,
    pub punch_case_length_mm: Option<f64>,
    pub ko_stroke_mm: Option<f64>,
    pub insert_od_mm: Option<f64>,
    pub open_extrusion_limit_pct: Option<f64>,
    pub die_angle_deg: Option<f64>,
    pub land_length_mm: Option<f64>,
}

impl MachineSetup {
    pub fn is_empty(&self) -> bool {
        [self.die_case_od_mm, self.die_case_length_mm, self.punch_case_od_mm, self.punch_case_length_mm, self.ko_stroke_mm, self.insert_od_mm].iter().all(|v| v.is_none())
    }
    pub fn open_limit(&self) -> f64 {
        self.open_extrusion_limit_pct.unwrap_or(heading::MAX_DIE_EXTRUSION_PCT)
    }
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct StationExtrusion {
    pub station_index: i32,
    pub from_mm: f64,
    pub to_mm: f64,
    pub reduction_pct: f64,
    pub held_length_mm: f64,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolItem {
    pub name: String,
    pub value: String,
    pub basis: ToolBasis,
}

fn ti(name: impl Into<String>, value: impl Into<String>, basis: ToolBasis) -> ToolItem {
    ToolItem { name: name.into(), value: value.into(), basis }
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct StationTooling {
    pub station: HeadingStation,
    pub in_die: Vec<ProfileSegment>,
    pub above_die: Vec<ProfileSegment>,
    pub items: Vec<ToolItem>,
    pub problems: Vec<String>,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolingSheet {
    pub machine: String,
    pub cut_off: Vec<ToolItem>,
    pub stations: Vec<StationTooling>,
    pub materials: Vec<ToolItem>,
    pub not_decided: Vec<String>,
}

impl ToolingSheet {
    pub fn problems(&self) -> Vec<String> {
        self.stations.iter().flat_map(|st| st.problems.iter().map(move |p| format!("Station {}: {}", st.station.index, p))).collect()
    }
}

/// Published guideline values for forward extrusion of steel.
pub mod extrusion_data {
    use crate::catalog::MaterialInfo;
    pub const OPEN_LOW: f64 = 30.0;
    pub const OPEN_HIGH: f64 = 35.0;
    pub const TRAPPED_LOW: f64 = 70.0;
    pub const TRAPPED_HIGH: f64 = 75.0;
    pub const ANGLE_SOLID_LOW: f64 = 24.0;
    pub const ANGLE_SOLID_HIGH: f64 = 70.0;
    pub const LAND_LOW: f64 = 1.5;
    pub const LAND_HIGH: f64 = 4.5;
    pub const LAND_ALT_LOW: f64 = 0.75;
    pub const LAND_ALT_HIGH: f64 = 3.25;
    pub const PUNCH_LIMIT_MPA: f64 = 2400.0;
    pub const DIE_INTERNAL_LIMIT_MPA: f64 = 1900.0;
    pub const HSS_LOW: f64 = 1800.0;
    pub const HSS_HIGH: f64 = 2200.0;
    pub const PM_HSS: f64 = 2500.0;
    pub const JOHNSON_A: f64 = 0.8;
    pub const JOHNSON_B_LOW: f64 = 1.2;
    pub const JOHNSON_B_HIGH: f64 = 1.5;

    pub fn max_ratio(m: &MaterialInfo) -> Option<f64> {
        if m.key.to_uppercase() == "1018" {
            Some(5.0)
        } else {
            None
        }
    }

    pub const SOURCES: [&str; 7] = [
        "Open 30 – 35 %, trapped 70 – 75 %, 2α 24° – 70° for solids, punch ≈ 2400 MPa / die ≈ 1900 MPa, max extrusion ratio ≈ 5 for AISI 1018 and 3.5 for type 305 stainless: IspatGuru, Cold Extrusion Process for Steel (ispatguru.com)",
        "Trapped shank extrusion up to 75 % vs the 30 % ground rule for open extrusion: Heading Hints (cold-forging guide)",
        "Extrusion angle should not exceed 30° (convention not stated): Carpenter, Heading Hints — A Guide to Cold Forming Specialty Alloys",
        "Reductions approaching 70 – 75 % by trapping the material: Machine Design, Banging out tiny parts (machinedesign.com)",
        "Die land 1.5 – 4.5 mm common practice; 0.75 – 3.25 mm in another source: cold extrusion handbook text (search excerpt)",
        "M2 / M42 punches 1800 – 2200 MPa, PM-HSS about 2500 MPa: MDPI J. Manuf. Mater. Process. 10(2):70",
        "p = σ̄ (a + b ln R), a ≈ 0.8, b ≈ 1.2 – 1.5 (Johnson): extrusion analysis lecture notes (MSU ME477; extrusion force calculators)",
    ];
}

/// Forward-extrusion die for one station.
pub mod extrusion_die {
    use super::extrusion_data as ed;
    use super::*;

    pub fn cone_length_mm(from_mm: f64, to_mm: f64, included_angle_deg: f64) -> f64 {
        (from_mm - to_mm) / 2.0 / (included_angle_deg / 2.0 * PI / 180.0).tan()
    }

    pub fn mean_flow_stress(m: &MaterialInfo, strain: f64) -> f64 {
        if strain <= 0.0 {
            0.0
        } else {
            m.flow_stress_coeff_k * strain.powf(m.strain_hardening_n) / (m.strain_hardening_n + 1.0)
        }
    }

    pub fn pressure_mpa(m: &MaterialInfo, from_mm: f64, to_mm: f64) -> (f64, f64) {
        let ln_r = (from_mm / to_mm).powf(2.0).ln();
        let s = mean_flow_stress(m, ln_r);
        (s * (ed::JOHNSON_A + ed::JOHNSON_B_LOW * ln_r), s * (ed::JOHNSON_A + ed::JOHNSON_B_HIGH * ln_r))
    }

    pub fn items(ex: &StationExtrusion, m: &MaterialInfo, setup: &MachineSetup) -> (Vec<ToolItem>, Vec<String>) {
        let mut items = Vec::new();
        let mut problems = Vec::new();
        let trapped = ex.reduction_pct > setup.open_limit();
        let container = ex.from_mm + heading::stage_clearance(ex.from_mm);
        items.push(ti(
            if trapped { "Container bore (trap)" } else { "Die entry bore" },
            format!("Ø{} mm (billet Ø{} + transfer clearance)", f(container), f(ex.from_mm)),
            ToolBasis::CALCULATED,
        ));
        if trapped {
            items.push(ti(
                "Trap length",
                if ex.held_length_mm > 0.0 { format!("Billet held {} mm in the container before the reduction starts", fd(ex.held_length_mm, 1)) } else { "Hold the billet in the container before the reduction starts".into() },
                ToolBasis::CALCULATED,
            ));
        }
        items.push(ti("Land bore", format!("Ø{} mm (extruded shank)", f(ex.to_mm)), ToolBasis::CALCULATED));
        let (alo, ahi) = (fd(ed::ANGLE_SOLID_LOW, 0), fd(ed::ANGLE_SOLID_HIGH, 0));
        match setup.die_angle_deg {
            Some(angle) => {
                items.push(ti("Die opening angle 2α", format!("{}° (your setting); guideline for solids {}° – {}°", fd(angle, 0), alo, ahi), ToolBasis::MACHINE));
                items.push(ti("Reduction cone length", format!("{} mm at 2α = {}°", f(cone_length_mm(ex.from_mm, ex.to_mm, angle)), fd(angle, 0)), ToolBasis::CALCULATED));
                if angle < ed::ANGLE_SOLID_LOW || angle > ed::ANGLE_SOLID_HIGH {
                    problems.push(format!("die angle 2α {}° is outside the {}° – {}° guideline for solid forward extrusion", fd(angle, 0), alo, ahi));
                }
            }
            None => {
                items.push(ti(
                    "Die opening angle 2α",
                    format!("{}° – {}° for solid forward extrusion; a cold-heading guide says the extrusion angle should not exceed 30° (it does not say whether included or half angle)", alo, ahi),
                    ToolBasis::PUBLISHED,
                ));
                items.push(ti(
                    "Reduction cone length",
                    format!(
                        "{} mm at 2α = {}° … {} mm at 2α = {}°",
                        f(cone_length_mm(ex.from_mm, ex.to_mm, ed::ANGLE_SOLID_HIGH)),
                        ahi,
                        f(cone_length_mm(ex.from_mm, ex.to_mm, ed::ANGLE_SOLID_LOW)),
                        alo
                    ),
                    ToolBasis::CALCULATED,
                ));
            }
        }
        items.push(ti(
            "Land length",
            match setup.land_length_mm {
                Some(l) => format!("{} mm (your setting)", fd(l, 1)),
                None => format!("{} – {} mm common practice (another source {} – {} mm)", fd(ed::LAND_LOW, 1), fd(ed::LAND_HIGH, 1), f(ed::LAND_ALT_LOW), f(ed::LAND_ALT_HIGH)),
            },
            if setup.land_length_mm.is_some() { ToolBasis::MACHINE } else { ToolBasis::PUBLISHED },
        ));
        items.push(ti("Back relief after the land", "Relief diameter and length", ToolBasis::TOOLMAKER));

        let mat_limit = ed::max_ratio(m).map(|r| (1.0 - 1.0 / r) * 100.0);
        items.push(ti(
            format!("Reduction limit for {}", m.key),
            format!(
                "{}open {} – {} %, trapped {} – {} %. This station: {} %",
                match mat_limit {
                    Some(l) => format!("Max extrusion ratio ≈ {} for {} (≈ {} %); ", fd(ed::max_ratio(m).unwrap(), 1), m.key, fd(l, 0)),
                    None => "No grade-specific limit published in the sources; ".into(),
                },
                fd(ed::OPEN_LOW, 0),
                fd(ed::OPEN_HIGH, 0),
                fd(ed::TRAPPED_LOW, 0),
                fd(ed::TRAPPED_HIGH, 0),
                fd(ex.reduction_pct, 1)
            ),
            ToolBasis::PUBLISHED,
        ));
        if let Some(l) = mat_limit {
            if ex.reduction_pct > l {
                problems.push(format!("reduction {} % is over the published ≈ {} % for {}", fd(ex.reduction_pct, 0), fd(l, 0), m.key));
            }
        }
        let (p_lo, p_hi) = pressure_mpa(m, ex.from_mm, ex.to_mm);
        let area = PI / 4.0 * container * container;
        items.push(ti(
            "Extrusion pressure (estimate)",
            format!(
                "{} – {} MPa → punch load ≈ {} – {} kN. Johnson p = σ̄(0.8 + 1.2…1.5 ln R) with σ̄ from the {} flow curve (K {} MPa, n {}); container friction not included",
                fd(p_lo, 0),
                fd(p_hi, 0),
                fd(p_lo * area / 1000.0, 0),
                fd(p_hi * area / 1000.0, 0),
                m.key,
                fd(m.flow_stress_coeff_k, 0),
                f(m.strain_hardening_n)
            ),
            ToolBasis::ESTIMATE,
        ));
        items.push(ti(
            "Tool pressure limits",
            format!(
                "Punch ≈ {} MPa, die internal ≈ {} MPa; M2 / M42 punch {} – {} MPa, PM-HSS ≈ {} MPa",
                fd(ed::PUNCH_LIMIT_MPA, 0),
                fd(ed::DIE_INTERNAL_LIMIT_MPA, 0),
                fd(ed::HSS_LOW, 0),
                fd(ed::HSS_HIGH, 0),
                fd(ed::PM_HSS, 0)
            ),
            ToolBasis::PUBLISHED,
        ));
        if p_hi > ed::PUNCH_LIMIT_MPA {
            problems.push(format!(
                "estimated pressure up to {} MPa is over the ≈ {} MPa punch limit: reduce the reduction per station or expect much shorter tool life",
                fd(p_hi, 0),
                fd(ed::PUNCH_LIMIT_MPA, 0)
            ));
        } else if p_hi > ed::HSS_LOW {
            items.push(ti("Punch steel", format!("Up to {} MPa is above the {} MPa low end for M2 / M42: PM-HSS punch preferred", fd(p_hi, 0), fd(ed::HSS_LOW, 0)), ToolBasis::PUBLISHED));
        }
        (items, problems)
    }
}

/// Splits a station's segments at `cut_mm` from the bottom: (below, above).
pub fn split(segments: &[ProfileSegment], cut_mm: f64) -> (Vec<ProfileSegment>, Vec<ProfileSegment>) {
    let mut below = Vec::new();
    let mut above = Vec::new();
    let mut acc = 0.0;
    for s in segments {
        let start = acc;
        let end = acc + s.height_mm;
        acc = end;
        if end <= cut_mm + 1e-9 {
            below.push(s.clone());
        } else if start >= cut_mm - 1e-9 {
            above.push(s.clone());
        } else {
            let h_below = cut_mm - start;
            if s.shape == SectionShape::ROUND && s.d_bottom_mm != s.d_top_mm {
                let d_cut = s.d_bottom_mm + (s.d_top_mm - s.d_bottom_mm) * h_below / s.height_mm;
                below.push(ProfileSegment { height_mm: h_below, d_top_mm: d_cut, ..s.clone() });
                above.push(ProfileSegment { height_mm: end - cut_mm, d_bottom_mm: d_cut, ..s.clone() });
            } else {
                below.push(ProfileSegment { height_mm: h_below, ..s.clone() });
                above.push(ProfileSegment { height_mm: end - cut_mm, ..s.clone() });
            }
        }
    }
    (below, above)
}

fn round_min(segs: &[ProfileSegment]) -> Option<f64> {
    jfmt::min_by(segs.iter().filter(|s| s.shape == SectionShape::ROUND).map(|s| s.d_bottom_mm.min(s.d_top_mm)), |v| *v)
}

/// Every station that reduces the shank, with its area reduction. Nut formers are not shank extrusions.
pub fn extrusions(plan: &HeadingPlan) -> Vec<StationExtrusion> {
    if plan.route == HeaderRoute::NUT_FORMER {
        return vec![];
    }
    let mut before = plan.slug_dia_mm;
    let mut out = Vec::new();
    for st in plan.stations.iter().filter(|s| !s.is_cut_off()) {
        let in_die = split(&st.segments, st.in_die_mm).0;
        let Some(now) = round_min(&in_die) else { continue };
        if now < before - 0.01 {
            let held: f64 = in_die.iter().filter(|s| s.shape == SectionShape::ROUND && s.d_bottom_mm.min(s.d_top_mm) >= before - 0.01).map(|s| s.height_mm).sum();
            out.push(StationExtrusion { station_index: st.index, from_mm: before, to_mm: now, reduction_pct: (1.0 - (now / before) * (now / before)) * 100.0, held_length_mm: held });
        }
        before = before.min(now);
    }
    out
}

fn seg_text(s: &ProfileSegment, clearance: bool) -> String {
    let c = |d: f64| if clearance { d + heading::stage_clearance(d) } else { d };
    match s.shape {
        SectionShape::ROUND => {
            if s.d_bottom_mm != s.d_top_mm {
                format!("cone Ø{} → Ø{} × {}", f(c(s.d_bottom_mm)), f(c(s.d_top_mm)), fd(s.height_mm, 1))
            } else {
                format!("Ø{} × {}", f(c(s.d_bottom_mm)), fd(s.height_mm, 1))
            }
        }
        SectionShape::HEX => format!("hex s={} × {}", f(s.d_bottom_mm), fd(s.height_mm, 1)),
        SectionShape::TWELVE_POINT => format!("12-point s={} × {}", f(s.d_bottom_mm), fd(s.height_mm, 1)),
        SectionShape::SQUARE => format!("square {} × {}", f(s.d_bottom_mm), fd(s.height_mm, 1)),
        SectionShape::DOME => format!("dome Ø{} × {} high", f(s.d_bottom_mm), fd(s.height_mm, 1)),
        _ => format!("{} {} × {}", s.shape.name().to_lowercase(), f(s.d_bottom_mm), fd(s.height_mm, 1)),
    }
}

pub fn build(r: &FastenerCalculationResult, machine: &MachineSetup) -> Option<ToolingSheet> {
    let plan = r.heading_plan.as_ref()?;
    let cut_off = vec![
        ti("Wire", format!("Ø{} mm {}", f(plan.slug_dia_mm), r.material.key), ToolBasis::CALCULATED),
        ti("Cut-off length", format!("{} mm (slug {} mm³)", f(plan.slug_length_mm), fd(plan.slug_volume_mm3, 0)), ToolBasis::CALCULATED),
        ti("Cut-off quill / knife bore", format!("Wire Ø{} + shearing clearance", f(plan.slug_dia_mm)), ToolBasis::TOOLMAKER),
    ];
    let die_stations: Vec<&HeadingStation> = plan.stations.iter().filter(|s| !s.is_cut_off()).collect();
    let exts = extrusions(plan);
    let ext: HashMap<i32, &StationExtrusion> = exts.iter().map(|e| (e.station_index, e)).collect();
    let stations = die_stations
        .iter()
        .enumerate()
        .map(|(i, st)| {
            let before = if i == 0 { plan.stations.iter().find(|s| s.is_cut_off()) } else { Some(die_stations[i - 1]) };
            station_tooling(st, machine, before, ext.get(&st.index).copied(), Some(&r.material))
        })
        .collect();
    let materials = vec![ti(format!("Die insert / punch (for {})", r.material.key), r.material.die_tooling.clone(), ToolBasis::CATALOGUE)];
    let mut not_decided = Vec::new();
    if !exts.is_empty() {
        not_decided.push(format!("Extrusion die values marked [published guideline] come from: {}", extrusion_data::SOURCES.join(" | ")));
    }
    not_decided.push(format!(
        "Carbide insert OD and shrink-fit interference: no published rule was found (studies report anything from 0.1 % to 0.8 % by station); size them with your carbide supplier or toolmaker.{}",
        machine.insert_od_mm.map(|v| format!(" Your planned insert OD Ø{} is used for the wall checks.", f(v))).unwrap_or_default()
    ));
    not_decided.push("Pressure pad, springs, guide bush and running fits of pins: set from your tool standard.".into());
    not_decided.push("Tolerances, surface finish and polish of bores and cavities: from your tool standard and the part drawing.".into());
    Some(ToolingSheet {
        machine: format!("{} — {} die station(s)", plan.machine_description(), plan.die_stations()),
        cut_off,
        stations,
        materials,
        not_decided,
    })
}

fn station_tooling(st: &HeadingStation, m: &MachineSetup, before: Option<&HeadingStation>, ex: Option<&StationExtrusion>, material: Option<&MaterialInfo>) -> StationTooling {
    let (in_die, above) = split(&st.segments, st.in_die_mm);
    let mut items: Vec<ToolItem> = Vec::new();
    let mut problems: Vec<String> = Vec::new();
    let trapped_max = heading::TRAPPED_EXTRUSION_MAX_PCT as i32;

    if !in_die.is_empty() {
        items.push(ti(
            "Die bore (bottom → die face)",
            format!(
                "{} mm, incl. transfer clearance {}–{} mm by Ø",
                in_die.iter().map(|s| seg_text(s, true)).collect::<Vec<_>>().join(" → "),
                f(heading::MIN_STAGE_CLEARANCE_MM),
                f(heading::MAX_STAGE_CLEARANCE_MM)
            ),
            ToolBasis::CALCULATED,
        ));
        items.push(ti("Die bore depth", format!("{} mm (part held in the die)", fd(st.in_die_mm, 1)), ToolBasis::CALCULATED));
        if ex.is_some() {
            for w in in_die.windows(2) {
                let (lower, upper) = (&w[0], &w[1]);
                if upper.max_width_mm() - lower.max_width_mm() > 0.05 {
                    items.push(ti(format!("Extrusion entry Ø{} → Ø{}", f(upper.max_width_mm()), f(lower.max_width_mm())), "Die entry angle, land length and radius", ToolBasis::TOOLMAKER));
                }
            }
        }
    } else {
        items.push(ti("Die", "Part is not held in a die bore at this station", ToolBasis::CALCULATED));
    }
    if let Some(ex) = ex {
        let pct = format!("{} % area (Ø{} → Ø{})", fd(ex.reduction_pct, 1), f(ex.from_mm), f(ex.to_mm));
        if ex.reduction_pct > heading::TRAPPED_EXTRUSION_MAX_PCT {
            items.push(ti("Extrusion", format!("{} — over the ~{} % trapped-extrusion limit", pct, trapped_max), ToolBasis::SHOP_PRACTICE));
            problems.push(format!("extrusion {} % is over the ~{} % trapped limit: extrude in two stations or choose a thinner wire", fd(ex.reduction_pct, 0), trapped_max));
        } else if ex.reduction_pct > m.open_limit() {
            items.push(ti(
                "Extrusion: TRAPPED die required",
                format!(
                    "{} is over your {} % open-extrusion limit: hold the billet in the Ø{} bore before the reduction{}",
                    pct,
                    fd(m.open_limit(), 0),
                    f(ex.from_mm + heading::stage_clearance(ex.from_mm)),
                    if ex.held_length_mm > 0.0 { format!(" (billet held over {} mm)", fd(ex.held_length_mm, 1)) } else { String::new() }
                ),
                ToolBasis::SHOP_PRACTICE,
            ));
        } else {
            items.push(ti("Extrusion: open", format!("{}, within your {} % open-extrusion limit", pct, fd(m.open_limit(), 0)), ToolBasis::SHOP_PRACTICE));
        }
        if let Some(material) = material {
            if ex.reduction_pct <= heading::TRAPPED_EXTRUSION_MAX_PCT {
                let (die_items, die_problems) = extrusion_die::items(ex, material, m);
                items.extend(die_items);
                problems.extend(die_problems);
            }
        }
    }
    let above_h: f64 = above.iter().map(|s| s.height_mm).sum();
    if !above.is_empty() {
        items.push(ti("Punch cavity (die face → punch)", format!("{} mm", above.iter().map(|s| seg_text(s, false)).collect::<Vec<_>>().join(" → ")), ToolBasis::CALCULATED));
        items.push(ti("Punch cavity depth", format!("{} mm above the die face", fd(above_h, 1)), ToolBasis::CALCULATED));
    }
    if let Some(g) = cone_gather(st, &above, before) {
        items.extend(g);
    }
    for rc in st.recesses.iter().filter(|r| r.from_top) {
        items.push(ti("Punch pin (recess)", format!("{} {} × {} mm deep", shape_name(rc.shape), f(rc.size_mm), fd(rc.depth_mm, 1)), ToolBasis::CALCULATED));
    }
    for rc in st.recesses.iter().filter(|r| !r.from_top) {
        items.push(ti("Knock-out pin nose (bottom recess)", format!("{} {} × {} mm", shape_name(rc.shape), f(rc.size_mm), fd(rc.depth_mm, 1)), ToolBasis::CALCULATED));
    }
    if let Some(h) = st.through_hole_mm {
        items.push(ti("Pierce pin", format!("Ø{} mm", f(h)), ToolBasis::CALCULATED));
    }
    if st.scrap_mm3 > 0.0 {
        items.push(ti("Scrap removed", format!("{} mm³", fd(st.scrap_mm3, 0)), ToolBasis::CALCULATED));
    }
    if let Some(s) = in_die.first() {
        let bottom_bore = s.max_width_mm() + heading::stage_clearance(s.max_width_mm());
        items.push(ti("Knock-out pin", format!("Runs in the Ø{} bottom bore; ejection stroke ≥ {} mm", f(bottom_bore), fd(st.in_die_mm, 1)), ToolBasis::CALCULATED));
    }
    if st.is_peak_blow {
        items.push(ti("Load", "Peak blow: full forming load on this station", ToolBasis::CALCULATED));
    }

    let max_bore = jfmt::max_by(in_die.iter().map(|s| s.max_width_mm() + heading::stage_clearance(s.max_width_mm())), |v| *v);
    if let Some(l) = m.die_case_length_mm {
        items.push(ti("Die case length", format!("{} mm", fd(l, 1)), ToolBasis::MACHINE));
        if st.in_die_mm >= l {
            problems.push(format!("die bore depth {} mm does not fit the {} mm die case", fd(st.in_die_mm, 1), fd(l, 1)));
        }
    }
    if let Some(k) = m.ko_stroke_mm {
        if st.in_die_mm > k {
            problems.push(format!("knock-out stroke {} mm cannot push out a part held {} mm in the die", fd(k, 1), fd(st.in_die_mm, 1)));
        }
    }
    if let Some(max_bore) = max_bore {
        if let Some(ins) = m.insert_od_mm {
            items.push(ti("Insert wall", format!("(Ø{} − Ø{}) / 2 = {} mm; OD/bore = {}", f(ins), f(max_bore), f((ins - max_bore) / 2.0), f(ins / max_bore)), ToolBasis::MACHINE));
            if ins <= max_bore {
                problems.push(format!("insert OD Ø{} is not larger than the die bore Ø{}", f(ins), f(max_bore)));
            }
        }
        if let Some(case) = m.die_case_od_mm {
            let inner = m.insert_od_mm.unwrap_or(max_bore);
            items.push(ti("Case ring wall", format!("(Ø{} − Ø{}) / 2 = {} mm", f(case), f(inner), f((case - inner) / 2.0)), ToolBasis::MACHINE));
            if case <= inner {
                problems.push(format!("die case Ø{} has no room for Ø{}", f(case), f(inner)));
            }
        }
    }
    let cavity_width = jfmt::max_by(above.iter().map(|s| s.max_width_mm()), |v| *v);
    if let (Some(p), Some(cw)) = (m.punch_case_od_mm, cavity_width) {
        if cw >= p {
            problems.push(format!("punch cavity {} mm across is wider than the Ø{} punch case", f(cw), f(p)));
        }
    }
    if let Some(pl) = m.punch_case_length_mm {
        if above_h >= pl {
            problems.push(format!("punch cavity depth {} mm does not fit the {} mm punch case", fd(above_h, 1), fd(pl, 1)));
        }
    }
    StationTooling { station: st.clone(), in_die, above_die: above, items, problems }
}

fn cone_gather(st: &HeadingStation, above: &[ProfileSegment], before: Option<&HeadingStation>) -> Option<Vec<ToolItem>> {
    let cone = above.iter().find(|s| s.shape == SectionShape::ROUND && (s.d_bottom_mm - s.d_top_mm).abs() > 0.05)?;
    let before = before?;
    if above.len() != 1 {
        return None;
    }
    let stock = split(&before.segments, st.in_die_mm).1;
    if stock.is_empty() {
        return None;
    }
    let free_len: f64 = stock.iter().map(|s| s.height_mm).sum();
    let stock_dia = stock[0].max_width_mm();
    let ld = free_len / stock_dia;
    let cone_h = cone.height_mm;
    let mut list = vec![ti(
        "Free stock before this blow",
        format!(
            "{} mm on Ø{} = L/D {} (app limits: 1 blow ≤ {}, 2 blows ≤ {})",
            f(free_len),
            f(stock_dia),
            f(ld),
            dstr(heading::SINGLE_BLOW_LD),
            dstr(heading::TWO_BLOW_LD)
        ),
        ToolBasis::CALCULATED,
    )];
    if ld > heading::SINGLE_BLOW_LD {
        list.push(ti(
            "Cone punch type",
            "Sliding (spring-loaded) cone punch is commonly used here: the sleeve encloses the free stock at first contact and slides back as the cone forms, so the stock cannot buckle. Whether a fixed cone is enough depends on the stock length, cone angle, material and stroke",
            ToolBasis::SHOP_PRACTICE,
        ));
        list.push(ti("Sleeve travel", format!("≥ {} − {} = {} mm, if the sleeve encloses the whole free stock at first contact", f(free_len), f(cone_h), f(free_len - cone_h)), ToolBasis::CALCULATED));
    }
    list.push(ti("Sleeve bore, cone angle, spring force and preload", "From your tool standard / toolmaker", ToolBasis::TOOLMAKER));
    Some(list)
}

fn shape_name(s: SectionShape) -> &'static str {
    match s {
        SectionShape::HEX => "hex",
        SectionShape::HEXALOBULAR => "hexalobular",
        SectionShape::SQUARE => "square",
        SectionShape::TWELVE_POINT => "12-point",
        _ => "round Ø",
    }
}

/// Plain-text tooling sheet for sharing.
pub fn text(r: &FastenerCalculationResult, s: &ToolingSheet, m: &MachineSetup) -> String {
    let mut o = String::new();
    let mut ln = |t: String| {
        o.push_str(&t);
        o.push('\n');
    };
    let line = "=".repeat(60);
    let item = |i: &ToolItem| format!("  {}: {}  [{}]", i.name, i.value, i.basis.tag());
    ln(line.clone());
    ln("AEROFAST PRO — HEADER TOOLING SHEET".into());
    ln(format!("{} × {} mm {}, {}", r.thread_label, fd(r.length_mm, 1), r.fastener_type.label(), r.material.key));
    ln(format!("Machine: {}", s.machine));
    ln(line.clone());
    ln("CUT-OFF".into());
    for i in &s.cut_off {
        ln(item(i));
    }
    for st in &s.stations {
        ln(String::new());
        ln(format!("STATION {}: {}", st.station.index, st.station.title));
        ln(format!("  {}", st.station.operation));
        for i in &st.items {
            ln(item(i));
        }
        for p in &st.problems {
            ln(format!("  !! {}", p));
        }
    }
    ln(String::new());
    ln("TOOL MATERIALS".into());
    for i in &s.materials {
        ln(item(i));
    }
    if !m.is_empty() {
        ln(String::new());
        ln("MACHINE POCKETS (entered)".into());
        if let Some(v) = m.die_case_od_mm {
            ln(format!("  Die case OD: {} mm", fd(v, 1)));
        }
        if let Some(v) = m.die_case_length_mm {
            ln(format!("  Die case length: {} mm", fd(v, 1)));
        }
        if let Some(v) = m.punch_case_od_mm {
            ln(format!("  Punch case OD: {} mm", fd(v, 1)));
        }
        if let Some(v) = m.punch_case_length_mm {
            ln(format!("  Punch case length: {} mm", fd(v, 1)));
        }
        if let Some(v) = m.ko_stroke_mm {
            ln(format!("  Knock-out stroke: {} mm", fd(v, 1)));
        }
        if let Some(v) = m.insert_od_mm {
            ln(format!("  Planned insert OD: {} mm", fd(v, 1)));
        }
    }
    ln(String::new());
    ln("NOT SET BY THE APP".into());
    for n in &s.not_decided {
        ln(format!("  - {}", n));
    }
    ln(line);
    o
}
