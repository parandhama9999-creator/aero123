//! Ports of QualityEngine.kt (capability, VDI 2230 torque, OEE, IT grades) and QualityDocs.kt
//! (control plan, APQP, PPAP, PSW, ECN, coating methods, reference tables).

use crate::calculation::FastenerCalculationResult;
use crate::gen_enums::{CoatingType, MaterialFamily, ProductStandard, StainlessType, UnitSystem};
use crate::jfmt::{self, pad_left, pad_right};
use crate::poka_yoke;
use crate::production_report;
use serde::{Deserialize, Serialize};
use std::f64::consts::PI;

// ---------------------------------------------------------------- QualityEngine.kt

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Capability {
    pub n: i32,
    pub mean: f64,
    pub min: f64,
    pub max: f64,
    pub sigma_within: f64,
    pub sigma_overall: f64,
    pub cp: Option<f64>,
    pub cpk: Option<f64>,
    pub pp: Option<f64>,
    pub ppk: Option<f64>,
    pub ppm_outside: f64,
    pub out_of_spec_count: i32,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TorqueTension {
    pub d2_mm: f64,
    pub d3_mm: f64,
    pub stress_area_mm2: f64,
    pub dkm_mm: f64,
    pub preload_kn: f64,
    pub torque_nm: f64,
    pub nut_factor_k: f64,
    pub thread_friction_pct: f64,
    pub head_friction_pct: f64,
    pub pitch_pct: f64,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Oee {
    pub availability: f64,
    pub performance: f64,
    pub quality: f64,
}

impl Oee {
    pub fn oee(&self) -> f64 {
        self.availability * self.performance * self.quality
    }
}

/// Numbers separated by spaces, commas, semicolons or new lines; anything else is ignored.
pub fn parse_values(text: &str) -> Vec<f64> {
    // Java \s: space, \t, \n, \x0B, \f, \r
    text.split(|c: char| matches!(c, ' ' | '\t' | '\n' | '\u{0B}' | '\u{0C}' | '\r' | ',' | ';'))
        .filter_map(|s| jfmt::to_double_or_null(&s.trim().replace('−', "-")))
        .collect()
}

pub fn capability(values: &[f64], lsl: Option<f64>, usl: Option<f64>) -> Option<Capability> {
    if values.len() < 2 {
        return None;
    }
    let n = values.len();
    let mean = values.iter().sum::<f64>() / n as f64;
    let s_overall = (values.iter().map(|v| (v - mean).powi(2)).sum::<f64>() / (n - 1) as f64).sqrt();
    let mr: Vec<f64> = values.windows(2).map(|w| (w[1] - w[0]).abs()).collect();
    let mr_bar = mr.iter().sum::<f64>() / mr.len() as f64;
    let s_within = mr_bar / 1.128;
    let cp = |s: f64| match (lsl, usl) {
        (Some(l), Some(u)) if s > 0.0 => Some((u - l) / (6.0 * s)),
        _ => None,
    };
    let cpk = |s: f64| -> Option<f64> {
        if s <= 0.0 {
            return None;
        }
        let upper = usl.map(|u| (u - mean) / (3.0 * s));
        let lower = lsl.map(|l| (mean - l) / (3.0 * s));
        [upper, lower].into_iter().flatten().fold(None, |a: Option<f64>, v| Some(a.map_or(v, |a| a.min(v))))
    };
    let ppm = if s_overall > 0.0 {
        let above = usl.map(|u| 1.0 - normal_cdf((u - mean) / s_overall)).unwrap_or(0.0);
        let below = lsl.map(|l| normal_cdf((l - mean) / s_overall)).unwrap_or(0.0);
        (above + below) * 1e6
    } else {
        0.0
    };
    let oos = values.iter().filter(|&&v| usl.is_some_and(|u| v > u) || lsl.is_some_and(|l| v < l)).count();
    // Kotlin min() / max() on doubles use Math.min / Math.max: a NaN propagates
    let jmin = |a: f64, b: f64| if a.is_nan() || b.is_nan() { f64::NAN } else { a.min(b) };
    let jmax = |a: f64, b: f64| if a.is_nan() || b.is_nan() { f64::NAN } else { a.max(b) };
    let min = values[1..].iter().fold(values[0], |a, &b| jmin(a, b));
    let max = values[1..].iter().fold(values[0], |a, &b| jmax(a, b));
    Some(Capability {
        n: n as i32,
        mean,
        min,
        max,
        sigma_within: s_within,
        sigma_overall: s_overall,
        cp: cp(s_within),
        cpk: cpk(s_within),
        pp: cp(s_overall),
        ppk: cpk(s_overall),
        ppm_outside: ppm,
        out_of_spec_count: oos as i32,
    })
}

/// Standard normal CDF (Abramowitz & Stegun 7.1.26 erf, |error| < 1.5e-7).
pub fn normal_cdf(z: f64) -> f64 {
    let x = z.abs() / 2f64.sqrt();
    let t = 1.0 / (1.0 + 0.3275911 * x);
    let erf = 1.0 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t * (-x * x).exp();
    if z >= 0.0 {
        0.5 * (1.0 + erf)
    } else {
        0.5 * (1.0 - erf)
    }
}

/// Inverse standard normal CDF (Acklam). None outside 0 < p < 1 (Kotlin throws there).
pub fn normal_inv(p: f64) -> Option<f64> {
    if !(p > 0.0 && p < 1.0) {
        return None;
    }
    const A: [f64; 6] = [-3.969683028665376e+01, 2.209460984245205e+02, -2.759285104469687e+02, 1.383577518672690e+02, -3.066479806614716e+01, 2.506628277459239e+00];
    const B: [f64; 5] = [-5.447609879822406e+01, 1.615858368580409e+02, -1.556989798598866e+02, 6.680131188771972e+01, -1.328068155288572e+01];
    const C: [f64; 6] = [-7.784894002430293e-03, -3.223964580411365e-01, -2.400758277161838e+00, -2.549732539343734e+00, 4.374664141464968e+00, 2.938163982698783e+00];
    const D: [f64; 4] = [7.784695709041462e-03, 3.224671290700398e-01, 2.445134137142996e+00, 3.754408661907416e+00];
    let p_low = 0.02425;
    Some(if p < p_low {
        let q = (-2.0 * p.ln()).sqrt();
        (((((C[0] * q + C[1]) * q + C[2]) * q + C[3]) * q + C[4]) * q + C[5]) / ((((D[0] * q + D[1]) * q + D[2]) * q + D[3]) * q + 1.0)
    } else if p <= 1.0 - p_low {
        let q = p - 0.5;
        let r = q * q;
        (((((A[0] * r + A[1]) * r + A[2]) * r + A[3]) * r + A[4]) * r + A[5]) * q / (((((B[0] * r + B[1]) * r + B[2]) * r + B[3]) * r + B[4]) * r + 1.0)
    } else {
        let q = (-2.0 * (1.0 - p).ln()).sqrt();
        -(((((C[0] * q + C[1]) * q + C[2]) * q + C[3]) * q + C[4]) * q + C[5]) / ((((D[0] * q + D[1]) * q + D[2]) * q + D[3]) * q + 1.0)
    })
}

/// Six Sigma level from DPMO, with the conventional 1.5 σ long-term shift.
pub fn sigma_level(dpmo: f64) -> Option<f64> {
    if dpmo <= 0.0 || dpmo >= 1e6 {
        None
    } else {
        normal_inv(1.0 - dpmo / 1e6).map(|z| z + 1.5)
    }
}

pub fn dpmo(defects: f64, units: f64, opportunities_per_unit: f64) -> Option<f64> {
    if units <= 0.0 || opportunities_per_unit <= 0.0 || defects < 0.0 {
        None
    } else {
        Some(defects / (units * opportunities_per_unit) * 1e6)
    }
}

pub fn oee(planned_min: f64, downtime_min: f64, ideal_cycle_sec: f64, total_count: f64, good_count: f64) -> Option<Oee> {
    let run = planned_min - downtime_min;
    if planned_min <= 0.0 || run <= 0.0 || ideal_cycle_sec <= 0.0 || total_count <= 0.0 || good_count < 0.0 || good_count > total_count {
        return None;
    }
    Some(Oee { availability: run / planned_min, performance: (ideal_cycle_sec * total_count / 60.0) / run, quality: good_count / total_count })
}

/// ISO 273 medium-series clearance hole dh for a metric thread.
pub fn clearance_hole_medium(d: f64) -> Option<f64> {
    const T: [(f64, f64); 20] = [
        (1.6, 1.8), (2.0, 2.4), (2.5, 2.9), (3.0, 3.4), (4.0, 4.5), (5.0, 5.5), (6.0, 6.6), (8.0, 9.0), (10.0, 11.0), (12.0, 13.5),
        (14.0, 15.5), (16.0, 17.5), (18.0, 20.0), (20.0, 22.0), (22.0, 24.0), (24.0, 26.0), (27.0, 30.0), (30.0, 33.0), (33.0, 36.0), (36.0, 39.0),
    ];
    T.iter().find(|(k, _)| (k - d).abs() < 1e-6).map(|x| x.1)
}

/// VDI 2230 Part 1 assembly preload and tightening torque.
#[allow(clippy::too_many_arguments)]
pub fn torque_tension(d: f64, p: f64, rp02_mpa: f64, mu_thread: f64, mu_head: f64, dw_mm: f64, dh_mm: f64, utilisation: f64) -> Option<TorqueTension> {
    if d <= 0.0 || p <= 0.0 || rp02_mpa <= 0.0 || mu_thread < 0.0 || mu_head < 0.0 || dw_mm <= 0.0 || dh_mm <= 0.0 || dw_mm <= dh_mm || utilisation <= 0.0 || utilisation > 1.0 {
        return None;
    }
    let d2 = d - 0.649519 * p;
    let d3 = d - 1.226869 * p;
    let ds = (d2 + d3) / 2.0;
    let as_mm2 = PI / 4.0 * ds * ds;
    let torsion = 1.5 * (d2 / ds) * (p / (PI * d2) + 1.155 * mu_thread);
    let fm_n = as_mm2 * utilisation * rp02_mpa / (1.0 + 3.0 * torsion * torsion).sqrt();
    let dkm = (dw_mm + dh_mm) / 2.0;
    let pitch_part = 0.16 * p;
    let thread_part = 0.58 * d2 * mu_thread;
    let head_part = dkm / 2.0 * mu_head;
    let sum = pitch_part + thread_part + head_part;
    let ma_nm = fm_n * sum / 1000.0;
    Some(TorqueTension {
        d2_mm: d2,
        d3_mm: d3,
        stress_area_mm2: as_mm2,
        dkm_mm: dkm,
        preload_kn: fm_n / 1000.0,
        torque_nm: ma_nm,
        nut_factor_k: ma_nm * 1000.0 / (fm_n * d),
        thread_friction_pct: thread_part / sum * 100.0,
        head_friction_pct: head_part / sum * 100.0,
        pitch_pct: pitch_part / sum * 100.0,
    })
}

/// ISO 286-1 tolerance grades IT13, IT14, IT15 (mm) up to 315 mm.
pub fn it_grade(grade: i32, size: f64) -> Option<f64> {
    const RANGES: [f64; 11] = [3.0, 6.0, 10.0, 18.0, 30.0, 50.0, 80.0, 120.0, 180.0, 250.0, 315.0];
    let row: [f64; 11] = match grade {
        13 => [0.14, 0.18, 0.22, 0.27, 0.33, 0.39, 0.46, 0.54, 0.63, 0.72, 0.81],
        14 => [0.25, 0.30, 0.36, 0.43, 0.52, 0.62, 0.74, 0.87, 1.00, 1.15, 1.30],
        15 => [0.40, 0.48, 0.58, 0.70, 0.84, 1.00, 1.20, 1.40, 1.60, 1.85, 2.10],
        _ => return None,
    };
    let i = RANGES.iter().position(|&r| size <= r);
    match i {
        Some(i) if size > 0.0 => Some(row[i]),
        _ => None,
    }
}

// ---------------------------------------------------------------- QualityDocs.kt

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ControlPlanRow {
    pub step: String,
    pub characteristic: String,
    pub is_product: bool,
    pub special: bool,
    pub spec: String,
    pub method: String,
    pub sampling: String,
    pub control: String,
    pub reaction: String,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PpapElement {
    pub no: i32,
    pub name: String,
    pub levels: String,
    pub for_this_part: String,
}

#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ChecklistPhase {
    pub title: String,
    pub items: Vec<(String, String)>,
}

fn n(v: f64, dp: usize) -> String {
    jfmt::f(v, dp)
}
fn n2(v: f64) -> String {
    n(v, 2)
}
fn before_paren(s: &str) -> &str {
    s.find(" (").map(|i| &s[..i]).unwrap_or(s)
}
fn if_blank<'a>(s: &'a str, d: &'a str) -> &'a str {
    if s.trim().is_empty() {
        d
    } else {
        s
    }
}

const REACTION: &str = "Stop, quarantine parts since the last good check, correct, 100 % sort, record";

fn is_iso_hex_bolt(r: &FastenerCalculationResult) -> bool {
    r.unit_system == UnitSystem::METRIC && (r.standard == ProductStandard::ISO_4014 || r.standard == ProductStandard::ISO_4017)
}

/// ISO 4014 / 4017 product grade A: d ≤ 24 and l ≤ min(10d, 150 mm).
pub fn product_grade_a(r: &FastenerCalculationResult) -> bool {
    r.nominal_dia_mm <= 24.0 && r.length_mm <= (10.0 * r.nominal_dia_mm).min(150.0)
}

/// ISO 4759-1 tolerances for ISO 4014 / 4017 hex bolts.
pub fn hex_bolt_tolerances(r: &FastenerCalculationResult) -> Vec<(String, String, String)> {
    let Some(hd) = &r.head_dimensions else { return vec![] };
    if !is_iso_hex_bolt(r) || !hd.is_table {
        return vec![];
    }
    let grade_a = product_grade_a(r);
    let mut out = Vec::new();
    let s_small = hd.across_flats_s <= 30.0;
    if let Some(it) = it_grade(if s_small { 13 } else { 14 }, hd.across_flats_s) {
        out.push((
            "Across flats s".to_string(),
            format!("{} −{} / 0 ({})", n2(hd.across_flats_s), n2(it), if s_small { "h13" } else { "h14" }),
            "Caliper / flat-anvil micrometer".to_string(),
        ));
    }
    if let Some(it) = it_grade(if grade_a { 14 } else { 15 }, hd.head_height_k) {
        out.push((
            "Head height k".into(),
            format!("{} ±{} ({})", n2(hd.head_height_k), n2(it / 2.0), if grade_a { "js14, grade A" } else { "js15, grade B" }),
            "Micrometer / height gauge".into(),
        ));
    }
    if grade_a {
        if let Some(it) = it_grade(15, r.length_mm) {
            out.push(("Length l".into(), format!("{} ±{} (js15, grade A)", n(r.length_mm, 1), n2(it / 2.0)), "Caliper / height gauge".into()));
        }
    } else {
        out.push(("Length l".into(), format!("{} (js17, grade B: see ISO 4759-1)", n(r.length_mm, 1)), "Caliper / height gauge".into()));
    }
    out.push(("Bearing face dw".into(), format!("{} min", n2(hd.bearing_dia_dw)), "Caliper / profile projector".into()));
    out
}

#[allow(clippy::too_many_arguments)]
fn cpr(step: &str, ch: &str, product: bool, special: bool, spec: impl Into<String>, method: impl Into<String>, sampling: &str, control: &str, reaction: &str) -> ControlPlanRow {
    ControlPlanRow {
        step: step.into(),
        characteristic: ch.into(),
        is_product: product,
        special,
        spec: spec.into(),
        method: method.into(),
        sampling: sampling.into(),
        control: control.into(),
        reaction: reaction.into(),
    }
}

pub fn control_plan(r: &FastenerCalculationResult) -> Vec<ControlPlanRow> {
    let mut v = Vec::new();
    let threaded = !r.fastener_type.is_unthreaded() && !r.fastener_type.is_washer();
    let ht = &r.heat_treatment_spec;
    let hardened = !ht.austenitizing_or_solution_temp_c.starts_with("N/A");
    let sample = "Set by PFMEA / customer (typical start: first-off, then per shift)";
    let gauge_std = if r.unit_system == UnitSystem::INCH { "ASME B1.2" } else { "ISO 1502" };

    v.push(cpr(
        "10 Receive wire",
        "Wire diameter and grade",
        true,
        false,
        format!("Ø{} mm {}; mill certificate to spec", n2(r.wire_dia_mm), r.material.key),
        "Micrometer; mill certificate review",
        "Each coil",
        "Incoming inspection record",
        "Reject coil, notify supplier",
    ));
    if r.wire_check.is_some() {
        v.push(cpr(
            "10 Receive wire",
            "Wire tensile / hardness for heading",
            true,
            false,
            r.raw_material_sourcing_uts.clone(),
            "Mill certificate; tensile test if required",
            "Each heat / coil",
            "Incoming inspection record",
            "Hold coil; anneal or reject",
        ));
    }
    v.push(cpr(
        "20 Cut-off & head",
        "Cut-off length / slug weight",
        false,
        false,
        format!("{} mm; {} g", n2(r.cut_off_length_mm), n(r.gross_weight_grams, 3)),
        "Caliper; balance",
        sample,
        "Set-up approval, patrol check",
        REACTION,
    ));
    let tol = hex_bolt_tolerances(r);
    for (name, spec, method) in &tol {
        v.push(cpr("20 Cut-off & head", name, true, false, spec.clone(), method.clone(), sample, "First-off approval, patrol check", REACTION));
    }
    if tol.is_empty() {
        if let Some(hd) = &r.head_dimensions {
            v.push(cpr(
                "20 Cut-off & head",
                "Head dimensions",
                true,
                false,
                format!("s / dk {}, k {} nominal; tolerances per {} / drawing", n2(hd.across_flats_s), n2(hd.head_height_k), r.standard.code()),
                "Caliper / micrometer / profile projector",
                sample,
                "First-off approval, patrol check",
                REACTION,
            ));
        }
    }
    if r.finished_part.as_ref().is_some_and(|p| p.recesses.iter().any(|x| x.from_top)) {
        v.push(cpr(
            "20 Cut-off & head",
            "Drive recess",
            true,
            false,
            "Penetration per standard / drawing",
            "Recess penetration gauge (e.g. ISO 4757 cross recess, ISO 10664 hexalobular)",
            sample,
            "First-off approval, patrol check",
            REACTION,
        ));
    }
    if threaded {
        if !r.is_internal_nut {
            v.push(cpr(
                "30 Thread roll",
                "Rolling blank diameter",
                false,
                false,
                format!("{} – {} mm", n(r.blank_lower_limit, 3), n(r.blank_upper_limit, 3)),
                "Micrometer",
                sample,
                "Set-up approval, patrol check",
                REACTION,
            ));
        }
        v.push(cpr(
            "30 Thread roll",
            &format!("Pitch diameter (thread {})", r.thread_class),
            true,
            true,
            format!(
                "{} – {} mm{}",
                n(r.pitch_dia_min, 3),
                n(r.pitch_dia_max, 3),
                if r.pre_plate_pitch_allowance_um > 0.0 { format!(" before coating (allowance {} µm)", n(r.pre_plate_pitch_allowance_um, 1)) } else { String::new() }
            ),
            if r.is_internal_nut { format!("GO / NO-GO plug gauge ({})", gauge_std) } else { format!("GO / NO-GO ring gauge ({}); three-wire for set-up", gauge_std) },
            sample,
            "First-off approval, patrol check",
            REACTION,
        ));
        v.push(cpr(
            "30 Thread roll",
            if r.is_internal_nut { "Minor diameter" } else { "Major diameter" },
            true,
            false,
            if r.is_internal_nut {
                format!("{} – {} mm", n(r.minor_dia_min, 3), n(r.minor_dia_max, 3))
            } else {
                format!("{} – {} mm", n(r.major_dia_min, 3), n(r.major_dia_max, 3))
            },
            if r.is_internal_nut { "Plug gauge / bore gauge" } else { "Micrometer" },
            sample,
            "Patrol check",
            REACTION,
        ));
    }
    if hardened {
        v.push(cpr(
            "40 Heat treat",
            "Furnace temperature, time, atmosphere",
            false,
            true,
            format!("{}; quench {}; temper {}", ht.austenitizing_or_solution_temp_c, ht.quench_medium, ht.tempering_or_aging_temp_c),
            "Furnace controller record; thermocouple survey (CQI-9 where required)",
            "Continuous / each load",
            "Furnace chart review",
            "Hold load, re-test hardness, re-treat or scrap",
        ));
        v.push(cpr(
            "40 Heat treat",
            "Core hardness",
            true,
            true,
            ht.target_hardness_hrc.clone(),
            "Rockwell (ISO 6508-1) / Vickers HV10 referee (ISO 6507-1)",
            "Each load",
            "Lab record",
            "Hold load, re-treat or scrap",
        ));
        let decarbs = poka_yoke::material_family(&r.material) == MaterialFamily::CARBON_ALLOY_STEEL || poka_yoke::stainless_type(&r.material) == Some(StainlessType::MARTENSITIC);
        if decarbs {
            v.push(cpr(
                "40 Heat treat",
                "Surface hardness / decarburisation",
                true,
                true,
                format!("Per {}", r.property_class.map(|p| format!("ISO 898-1 ({})", before_paren(p.label()))).unwrap_or_else(|| "material spec".into())),
                "Vickers HV0.3 surface vs core; metallographic section",
                "Each load",
                "Lab record",
                "Hold load, re-treat or scrap",
            ));
        }
    }
    if let Some(pc) = r.property_class.filter(|_| !r.fastener_type.is_washer() && !r.fastener_type.is_unthreaded()) {
        let step = if hardened { "40 Heat treat" } else { "50 Final" };
        v.push(cpr(
            step,
            "Proof load",
            true,
            true,
            format!("{} kN ({}, {})", n(r.proof_load_kn, 1), if r.fastener_type.is_nut() { "ISO 898-2" } else { "ISO 898-1" }, before_paren(pc.label())),
            "Tensile machine, proof load test",
            "Each load / lot",
            "Lab record",
            "Hold lot, investigate",
        ));
        if !r.fastener_type.is_nut() {
            v.push(cpr(
                step,
                "Minimum tensile (wedge) load",
                true,
                true,
                format!("{} kN min (ISO 898-1 wedge test)", n(r.tensile_breaking_load_kn, 1)),
                "Tensile machine with wedge",
                "Each load / lot",
                "Lab record",
                "Hold lot, investigate",
            ));
        }
    }
    if r.coating.thickness_max_um() > 0.0 {
        v.push(cpr(
            "60 Coat",
            "Coating thickness",
            true,
            false,
            r.coating.spec_standard(),
            coating_thickness_methods(r.coating).first().map(|x| x.1.clone()).unwrap_or_else(|| "Per coating spec".into()),
            "Each lot",
            "Coater certificate + receiving check",
            "Hold lot, return to coater",
        ));
        v.push(cpr(
            "60 Coat",
            "Corrosion resistance",
            true,
            false,
            "Neutral salt spray hours per drawing / customer spec",
            "ISO 9227 NSS / ASTM B117",
            "Per customer frequency",
            "Lab report",
            "Hold lot, investigate coating",
        ));
    }
    if r.has_hydrogen_embrittlement_risk {
        v.push(cpr(
            "60 Coat",
            "Hydrogen de-embrittlement bake",
            false,
            true,
            format!("{}, {}", ht.baking_temp_c, ht.baking_soak_time),
            "Oven chart; time from plating to bake recorded",
            "Each load",
            "Oven chart review",
            "Hold load; embrittlement test before release",
        ));
        v.push(cpr(
            "60 Coat",
            "Hydrogen embrittlement",
            true,
            true,
            "No fracture",
            "ISO 15330 sustained load test / ASTM F1940 process control",
            "Per customer frequency",
            "Lab report",
            "Hold lot, investigate",
        ));
    }
    if r.thread_lock_patch {
        v.push(cpr(
            "70 Patch",
            "Patch position and prevailing torque",
            true,
            false,
            "Per drawing (e.g. DIN 267-28 for polyamide patches)",
            "Visual; torque test",
            "Each lot",
            "Patrol check",
            REACTION,
        ));
    }
    v.push(cpr(
        "80 Final inspection",
        "Appearance, mixed parts",
        true,
        false,
        "No cracks, laps, burrs or foreign parts",
        "Visual; optical sorting where required",
        "Each lot",
        "Final inspection record",
        REACTION,
    ));
    v.push(cpr(
        "90 Pack & label",
        "Label, quantity, traceability",
        false,
        false,
        "Part number, lot and heat traceable",
        "Visual; label check",
        "Each container",
        "Packing record",
        "Re-label, re-count",
    ));
    v
}

fn phase(title: &str, items: Vec<(&str, String)>) -> ChecklistPhase {
    ChecklistPhase { title: title.into(), items: items.into_iter().map(|(a, b)| (a.to_string(), b)).collect() }
}
fn s(v: &str) -> String {
    v.to_string()
}

pub fn apqp(r: &FastenerCalculationResult) -> Vec<ChecklistPhase> {
    vec![
        phase(
            "1. Plan and define program",
            vec![
                ("Design goals", format!("Application, load and environment of the {}", before_paren(r.fastener_type.label()))),
                ("Reliability and quality goals", s("Customer PPM target, field failure history")),
                ("Preliminary bill of material", format!("{} wire Ø{} mm, {}", r.material.key, n2(r.wire_dia_mm), r.coating.label())),
                ("Preliminary process flow chart", format!("{} operations (see Process sequence)", production_report::process_sequence(r).len())),
                ("Preliminary special characteristics", special_characteristics(r).join(", ")),
                ("Product assurance plan", s("Requirements, reliability targets, test plan")),
                ("Management support", s("Resources and timing approved")),
            ],
        ),
        phase(
            "2. Product design and development",
            vec![
                ("Design FMEA", s("Customer-owned for build-to-print parts; supplier-owned for supplier-designed parts")),
                (
                    "Design for manufacturability",
                    format!("Heading route: {}, upset L/D {}", r.heading_plan.as_ref().map(|p| p.route.label()).unwrap_or("per header"), n2(r.upset_ld_ratio)),
                ),
                ("Design verification", format!("Proof load {} kN, hardness {}", n(r.proof_load_kn, 1), r.heat_treatment_spec.target_hardness_hrc)),
                ("Design reviews", format!("Drawing, standard {}, thread {}", r.standard.code(), r.thread_class)),
                ("Prototype control plan", s("Dimensions, thread, hardness, coating")),
                ("Engineering drawings and specifications", s("Customer drawing revision recorded")),
                ("Material specifications", r.raw_material_sourcing_uts.clone()),
                ("New equipment, tooling and gauges", format!("Header {}; thread gauges {}", r.recommended_machine, r.thread_class)),
                ("Team feasibility commitment", s("Signed feasibility review")),
            ],
        ),
        phase(
            "3. Process design and development",
            vec![
                ("Packaging standards", s("Customer packaging and labelling spec")),
                ("Process flow chart", s("From wire receiving to packing (see Control plan steps)")),
                ("Floor plan layout", s("Header, roller, furnace, coater, sorting")),
                ("Characteristics matrix", s("Links each characteristic to the operation that creates it")),
                ("Process FMEA", s("See the 8D / PFMEA tab")),
                ("Pre-launch control plan", s("Control plan with tighter frequencies")),
                ("Process instructions", s("Set-up and patrol sheets per operation")),
                ("MSA plan", s("Gauge R&R on thread, hardness and head dimension gauges")),
                ("Preliminary capability study plan", s("Ppk on head height, pitch diameter and hardness")),
            ],
        ),
        phase(
            "4. Product and process validation",
            vec![
                ("Significant production run", s("1 to 8 h of production, at least 300 consecutive parts unless the customer states otherwise")),
                ("MSA evaluation", s("%GRR < 10 % acceptable; 10–30 % may be acceptable; > 30 % not acceptable (AIAG MSA)")),
                ("Preliminary process capability", s("Ppk > 1.67 acceptable; 1.33–1.67 may be acceptable; < 1.33 not acceptable (AIAG PPAP)")),
                ("PPAP", s("Submit at the level the customer requests (see PPAP)")),
                ("Production validation testing", s("Mechanical, coating and embrittlement tests on run parts")),
                ("Packaging evaluation", s("Trial shipment / drop test if required")),
                ("Production control plan", s("See Control plan")),
                ("Quality planning sign-off", s("Team and management sign-off")),
            ],
        ),
        phase(
            "5. Feedback, assessment and corrective action",
            vec![
                ("Reduced variation", s("Control charts, capability tracking")),
                ("Customer satisfaction", s("PPM, field returns")),
                ("Delivery and service", s("On-time delivery, 8D response time")),
                ("Lessons learned", s("Feed findings back into PFMEA and control plan")),
            ],
        ),
    ]
}

pub fn special_characteristics(r: &FastenerCalculationResult) -> Vec<String> {
    let mut v = Vec::new();
    if !r.fastener_type.is_unthreaded() && !r.fastener_type.is_washer() {
        v.push(s("thread pitch diameter"));
    }
    if !r.heat_treatment_spec.austenitizing_or_solution_temp_c.starts_with("N/A") {
        v.push(s("core hardness"));
        if poka_yoke::material_family(&r.material) == MaterialFamily::CARBON_ALLOY_STEEL {
            v.push(s("decarburisation"));
        }
    }
    if r.property_class.is_some() && !r.fastener_type.is_washer() && !r.fastener_type.is_unthreaded() {
        v.push(s("proof load"));
    }
    if r.has_hydrogen_embrittlement_risk {
        v.push(s("hydrogen embrittlement"));
    }
    v
}

/// Variable (measured) characteristics where a Ppk study is meaningful.
pub fn capability_characteristics(r: &FastenerCalculationResult) -> Vec<String> {
    let mut v = Vec::new();
    if r.head_dimensions.is_some() {
        v.push(s("head height k"));
        v.push(s("across flats / head diameter"));
    }
    if !r.fastener_type.is_unthreaded() && !r.fastener_type.is_washer() {
        v.push(s("pitch diameter (thread micrometer / three-wire)"));
    }
    if !r.heat_treatment_spec.austenitizing_or_solution_temp_c.starts_with("N/A") {
        v.push(s("core hardness"));
    }
    if v.is_empty() {
        v.push(s("the drawing's key dimensions"));
    }
    v
}

fn pe(no: i32, name: &str, levels: &str, for_this_part: impl Into<String>) -> PpapElement {
    PpapElement { no, name: name.into(), levels: levels.into(), for_this_part: for_this_part.into() }
}

/// AIAG PPAP 4th edition, Table 4.2.
pub fn ppap_elements(r: &FastenerCalculationResult) -> Vec<PpapElement> {
    let threaded = !r.fastener_type.is_unthreaded() && !r.fastener_type.is_washer();
    let tests: Vec<String> = [
        Some(s("material cert")),
        r.property_class.map(|_| format!("proof load {} kN", n(r.proof_load_kn, 1))),
        Some(s("hardness")),
        if r.coating.thickness_max_um() > 0.0 { Some(s("coating thickness and salt spray")) } else { None },
        if r.has_hydrogen_embrittlement_risk { Some(s("hydrogen embrittlement")) } else { None },
    ]
    .into_iter()
    .flatten()
    .collect();
    vec![
        pe(1, "Design records", "R S S * R", format!("Customer drawing {}, balloon every characteristic", r.standard.code())),
        pe(2, "Engineering change documents", "R S S * R", "Authorised ECNs not yet on the drawing (see Change management)"),
        pe(3, "Customer engineering approval", "R R S * R", "If the customer requires it"),
        pe(4, "Design FMEA", "R R S * R", "Only if the supplier is design-responsible"),
        pe(5, "Process flow diagram", "R R S * R", format!("{} operations", production_report::process_sequence(r).len())),
        pe(6, "Process FMEA", "R R S * R", "See the 8D / PFMEA tab"),
        pe(7, "Control plan", "R R S * R", format!("{} lines (see Control plan)", control_plan(r).len())),
        pe(8, "Measurement system analysis", "R R S * R", format!("GR&R on {}head gauges, hardness tester", if threaded { "thread gauges, " } else { "" })),
        pe(9, "Dimensional results", "R S S * R", "All ballooned characteristics on the PPAP samples"),
        pe(10, "Material / performance test results", "R S S * R", tests.join(", ")),
        pe(
            11,
            "Initial process studies",
            "R R S * R",
            format!("Ppk on measured characteristics: {} (pass / fail tests are not capability studies)", capability_characteristics(r).join(", ")),
        ),
        pe(12, "Qualified laboratory documentation", "R S S * R", "Lab scope (ISO/IEC 17025 or customer-approved)"),
        pe(13, "Appearance approval report", "S S S * R", "Only for appearance items"),
        pe(14, "Sample production parts", "R S S * R", "Quantity per customer"),
        pe(15, "Master sample", "R R R * R", "Retain a signed sample"),
        pe(16, "Checking aids", "R R R * R", if threaded { format!("Thread gauges {}, recess / special gauges", r.thread_class) } else { s("Special gauges") }),
        pe(17, "Customer-specific requirements", "R R S * R", "See the customer requirements list"),
        pe(18, "Part submission warrant (PSW)", "S S S S R", format!("Part weight {} kg", n(r.net_weight_grams / 1000.0, 4))),
        pe(19, "Bulk material requirements checklist", "R R S * R", "Bulk materials only"),
    ]
}

pub const PSW_REASONS: [&str; 10] = [
    "Initial submission",
    "Engineering change(s)",
    "Tooling: transfer, replacement, refurbishment or additional",
    "Correction of discrepancy",
    "Tooling inactive > 1 year",
    "Change to optional construction or material",
    "Sub-supplier or material source change",
    "Change in part processing",
    "Parts produced at additional location",
    "Other",
];

/// Customer notification cases from AIAG PPAP 4th edition, section 3.1.
pub const CHANGE_TRIGGERS: [&str; 9] = [
    "Use of other construction or material than in the approved part",
    "Production from new or modified tools (except perishable tools), dies or moulds, including additional or replacement tooling",
    "Production after upgrade or rearrangement of existing tooling or equipment",
    "Production from tooling and equipment transferred to a different or additional plant location",
    "Change of supplier for parts, non-equivalent materials or services (e.g. heat treatment, plating)",
    "Product produced after tooling has been inactive for volume production for 12 months or more",
    "Product and process changes on components made internally or by suppliers",
    "Change in test / inspection method (new technique, no change to acceptance criteria)",
    "Bulk materials: new source of raw material, or change in appearance attributes",
];

/// Kotlin `String.lines()`: splits on \r\n, \n and \r.
fn kotlin_lines(s: &str) -> Vec<&str> {
    let mut out = Vec::new();
    let b = s.as_bytes();
    let mut start = 0;
    let mut i = 0;
    while i < b.len() {
        if b[i] == b'\n' || b[i] == b'\r' {
            out.push(&s[start..i]);
            if b[i] == b'\r' && i + 1 < b.len() && b[i + 1] == b'\n' {
                i += 1;
            }
            start = i + 1;
        }
        i += 1;
    }
    out.push(&s[start..]);
    out
}

pub fn psw(r: &FastenerCalculationResult, part_no: &str, drawing_rev: &str, customer: &str, level: i32, reason: &str, csr: &str) -> String {
    let rule = "-".repeat(60);
    let mut o = String::new();
    o.push_str("PART SUBMISSION WARRANT (AIAG PPAP 4th ed. format)\n");
    o.push_str(&rule);
    o.push('\n');
    o.push_str(&format!("Part name        : {}\n", r.fastener_type.label()));
    o.push_str(&format!("Customer part no.: {}\n", if_blank(part_no, "________")));
    o.push_str(&format!("Drawing revision : {}\n", if_blank(drawing_rev, "________")));
    o.push_str(&format!("Customer         : {}\n", if_blank(customer, "________")));
    o.push_str(&format!("Size             : {} x {} mm\n", r.thread_label, n(r.length_mm, 1)));
    o.push_str(&format!("Material         : {} - {}\n", r.material.key, r.material.name));
    let strength = r.tensile_range.as_ref().map(|t| t.label()).or_else(|| r.property_class.map(|p| p.label().to_string())).unwrap_or_else(|| s("per material spec"));
    o.push_str(&format!("Strength         : {}\n", strength));
    o.push_str(&format!("Coating          : {} ({})\n", r.coating.label(), r.coating.spec_standard()));
    o.push_str(&format!("Part weight      : {} kg\n", n(r.net_weight_grams / 1000.0, 4)));
    o.push_str(&format!("Submission level : {}\n", level));
    o.push_str(&format!("Reason           : {}\n", reason));
    if !csr.trim().is_empty() {
        o.push_str("Customer-specific requirements:\n");
        o.push_str(&kotlin_lines(csr).into_iter().filter(|l| !l.trim().is_empty()).map(|l| format!(" - {}", l.trim())).collect::<Vec<_>>().join("\n"));
        o.push('\n');
    }
    o.push_str(&rule);
    o.push('\n');
    o.push_str(&format!("ELEMENTS FOR LEVEL {} (S = submit, R = retain, * = per customer)\n", level));
    for e in ppap_elements(r) {
        let code = if level >= 1 { e.levels.split(' ').nth((level - 1) as usize).unwrap_or("?") } else { "?" };
        o.push_str(&format!(" {} {} {}  {}\n", pad_left(&e.no.to_string(), 2), pad_right(&e.name, 38), code, e.for_this_part));
    }
    o.push_str(&rule);
    o.push('\n');
    o.push_str("Declaration: samples are representative of parts made by the production process, from a significant\n");
    o.push_str("production run, and meet all drawing and specification requirements.\n");
    o.push_str("Supplier signature / date: __________________\n");
    o
}

#[allow(clippy::too_many_arguments)]
pub fn ecn(r: &FastenerCalculationResult, number: &str, from_rev: &str, to_rev: &str, description: &str, reason: &str, triggers: &[usize], csr: &str) -> String {
    let rule = "-".repeat(60);
    let mut o = String::new();
    o.push_str(&format!("ENGINEERING CHANGE NOTICE {}\n", if_blank(number, "________")));
    o.push_str(&rule);
    o.push('\n');
    o.push_str(&format!("Part      : {} {} x {} mm\n", r.fastener_type.label(), r.thread_label, n(r.length_mm, 1)));
    o.push_str(&format!("Revision  : {} -> {}\n", if_blank(from_rev, "__"), if_blank(to_rev, "__")));
    o.push_str(&format!("Change    : {}\n", if_blank(description, "________")));
    o.push_str(&format!("Reason    : {}\n", if_blank(reason, "________")));
    o.push_str(&rule);
    o.push('\n');
    let mut t: Vec<usize> = triggers.to_vec();
    t.sort();
    t.dedup();
    if t.is_empty() {
        o.push_str("No PPAP customer-notification case selected.\n");
    } else {
        o.push_str("CUSTOMER NOTIFICATION REQUIRED (AIAG PPAP section 3.1):\n");
        for i in t {
            o.push_str(&format!(" - {}\n", CHANGE_TRIGGERS[i]));
        }
        o.push_str("Submit PPAP before shipping changed parts unless the customer waives it.\n");
    }
    if !csr.trim().is_empty() {
        o.push_str(&format!("Check also: {}\n", kotlin_lines(csr).into_iter().filter(|l| !l.trim().is_empty()).collect::<Vec<_>>().join("; ")));
    }
    o.push_str(&rule);
    o.push('\n');
    o.push_str("DOCUMENTS TO UPDATE\n");
    for d in [
        "Drawing / specification",
        "Process flow",
        "PFMEA",
        "Control plan",
        "Process and inspection instructions",
        "Gauges and MSA",
        "Tooling drawings",
        "PPAP file / PSW",
        "Packaging and labels",
    ] {
        o.push_str(&format!(" [ ] {}\n", d));
    }
    o.push_str(&rule);
    o.push('\n');
    o.push_str("Stock: [ ] use up  [ ] rework  [ ] scrap   Effective from lot / date: ________\n");
    o.push_str("Approved: Engineering ______  Quality ______  Production ______  Customer ______\n");
    o
}

/// Thickness or coating-weight methods for this coating, most usual first.
pub fn coating_thickness_methods(c: CoatingType) -> Vec<(String, String)> {
    use CoatingType::*;
    let v: &[(&str, &str)] = match c {
        ZN_NI => &[
            ("XRF", "X-ray fluorescence ISO 3497 / ASTM B568 (also reads the Ni %)"),
            ("Magnetic", "Magnetic induction ISO 2178 / ASTM B499"),
            ("Referee", "Microscopic cross-section ISO 1463 / ASTM B487"),
        ],
        ZINC_BLUE | ZINC_YELLOW | AERO_CAD | AERO_CAD_CL2 | AERO_CAD_CL3 | AERO_CAD_LHE => &[
            ("Magnetic", "Magnetic induction ISO 2178 / ASTM B499"),
            ("XRF", "X-ray fluorescence ISO 3497 / ASTM B568"),
            ("Coulometric", "Coulometric ISO 2177 / ASTM B504"),
            ("Referee", "Microscopic cross-section ISO 1463 / ASTM B487"),
        ],
        GEOMET | DACROMET => &[
            ("Coating weight", "Coating mass per unit area (ISO 10683)"),
            ("Magnetic", "Magnetic induction ISO 2178 on the reference area"),
            ("Referee", "Microscopic cross-section ISO 1463"),
        ],
        PHOSPHATE | PHOSPHATE_MN => &[("Coating weight", "Gravimetric coating mass ISO 3892 (g/m²)")],
        HOT_DIP_GALV => &[("Magnetic", "Magnetic induction ISO 2178"), ("Gravimetric", "Gravimetric ISO 1460 (ISO 10684)")],
        ENP => &[("XRF", "X-ray fluorescence ISO 3497"), ("Referee", "Microscopic cross-section ISO 1463")],
        AERO_IVD | PTFE_XYLAN => &[("Magnetic", "Magnetic induction ISO 2178 / ASTM B499")],
        PASSIVATION | PASSIVATION_CITRIC => &[("No thickness", "Verify per ASTM A967: water immersion, humidity, salt spray or copper sulphate test")],
        BLACK_OXIDE => &[("No thickness", "Appearance and oil film; corrosion test per MIL-DTL-13924")],
        _ => &[("Per spec", "Per the coating specification")],
    };
    v.iter().map(|(a, b)| (a.to_string(), b.to_string())).collect()
}

pub const TEST_METHODS: [(&str, &str); 10] = [
    ("Tensile and wedge load (bolts)", "ISO 898-1 full-size tests; ISO 6892-1 machined test pieces"),
    ("Proof load (bolts / nuts)", "ISO 898-1 / ISO 898-2; inch: ASTM F606"),
    ("Hardness", "Rockwell ISO 6508-1, Vickers ISO 6507-1 (HV10 referee); surface HV0.3 for decarburisation"),
    ("Torque-tension / friction", "ISO 16047 (total, thread and head friction coefficients)"),
    ("Prevailing torque", "ISO 2320 (lock nuts); DIN 267-28 (polyamide patch)"),
    ("Hydrogen embrittlement", "ISO 15330 sustained load; ASTM F1940 process control; ASTM F519 (aerospace)"),
    ("Salt spray", "ISO 9227 NSS / ASTM B117; hours to white and red corrosion per drawing"),
    ("Thread gauging", "ISO 1502 GO / NO-GO (metric); ASME B1.2 gauges, ASME B1.3 system 21 / 22 / 23 (inch)"),
    ("Dimensions", "Caliper, micrometer, height gauge; profile projector for radii and chamfers; CMM / vision for GD&T"),
    ("Drive recess", "Penetration gauges: ISO 4757 (cross recess), ISO 10664 (hexalobular)"),
];

pub const GDT: [(&str, &str, &str); 6] = [
    ("Straightness", "Shank / thread axis", "V-blocks + dial indicator, or CMM"),
    ("Perpendicularity / total runout", "Bearing face to thread axis", "Threaded collet + dial indicator on the bearing face"),
    ("Coaxiality / position", "Head or socket to shank", "CMM, vision system, or functional gauge"),
    ("Symmetry", "Slot to head axis", "Profile projector / vision"),
    ("Position", "Cross hole, split-pin hole", "Functional pin gauge or CMM"),
    ("Profile / angle", "Countersink head, cone seat", "Profile projector, angle gauge"),
];

pub const JIS_EQUIVALENTS: [(&str, &str); 11] = [
    ("JIS B 1180", "ISO 4014 / 4017 / 4018 hex bolts and screws (old JIS sizes in the annex)"),
    ("JIS B 1181", "ISO 4032 / 4033 / 4035 hex nuts"),
    ("JIS B 1176", "ISO 4762 hexagon socket head cap screws"),
    ("JIS B 1177", "ISO 4026 – 4029 hexagon socket set screws"),
    ("JIS B 1101", "ISO 1207 / 1580 / 2009 / 2010 slotted head screws"),
    ("JIS B 1111", "ISO 7045 / 7046 / 7047 cross recessed head screws"),
    ("JIS B 1256", "ISO 7089 / 7090 / 7092 / 7093 plain washers"),
    ("JIS B 1051", "ISO 898-1 bolt mechanical properties"),
    ("JIS B 1052-2", "ISO 898-2 nut mechanical properties"),
    ("JIS B 0205", "ISO 261 / 262 / 724 / 965-1 metric threads"),
    ("JIS B 0209", "ISO 965 metric thread tolerances"),
];

/// Old JIS B 1180 annex across-flats that differ from ISO (d, JIS s, ISO s).
pub const JIS_OLD_ACROSS_FLATS: [(f64, f64, f64); 4] = [(10.0, 17.0, 16.0), (12.0, 19.0, 18.0), (14.0, 22.0, 21.0), (22.0, 32.0, 34.0)];

pub const DMAIC: [(&str, &str); 5] = [
    ("Define", "Problem, customer impact, scope, goal (e.g. cut head crack PPM by 80 %)"),
    ("Measure", "Baseline PPM / Cpk; validate gauges with GR&R first"),
    ("Analyze", "Root cause: 5-Why, fishbone, data by machine / shift / coil / heat"),
    ("Improve", "Fix and verify with data (tooling, wire, settings, poka-yoke)"),
    ("Control", "Update PFMEA and control plan, SPC, standard work"),
];

pub const WASTES: [(&str, &str); 8] = [
    ("Transport", "Bins moved between header, roller and furnace"),
    ("Inventory", "WIP waiting for heat treatment or plating"),
    ("Motion", "Searching for gauges and tools"),
    ("Waiting", "Die change, first-off approval, furnace loading"),
    ("Over-production", "Running beyond the order to avoid a die change"),
    ("Over-processing", "Tighter tolerances than the drawing asks"),
    ("Defects", "Cracks, mixed parts, thread damage, sorting"),
    ("Skills", "Operator knowledge not used for improvement"),
];
